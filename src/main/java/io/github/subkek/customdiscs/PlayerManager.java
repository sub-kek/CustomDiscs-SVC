package io.github.subkek.customdiscs;

import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import io.github.subkek.customdiscs.config.CustomDiscsConfiguration;
import io.github.subkek.customdiscs.event.HopperHandler;
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.jflac.sound.spi.Flac2PcmAudioInputStream;
import org.jflac.sound.spi.FlacAudioFileReader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

public class PlayerManager {
  private static final AudioFormat FORMAT = new AudioFormat(
      AudioFormat.Encoding.PCM_SIGNED,
      48000F,
      16,
      1,
      2,
      48000F,
      false
  );
  private static PlayerManager instance;
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  private final Map<UUID, DiscPlayer> playerMap = new HashMap<>();

  public static PlayerManager getInstance() {
    if (instance == null) {
      instance = new PlayerManager();
    }
    return instance;
  }

  public void playLocationalAudio(Path soundFilePath, Block block, Component actionbarComponent) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    CustomDiscs.debug("Player UUID is {0}", uuid.toString());
    if (playerMap.containsKey(uuid)) stopPlaying(uuid);
    CustomDiscs.debug("Player {0} not already exists", uuid.toString());

    VoicechatServerApi api = VoicePlugin.voicechatApi;

    DiscPlayer discPlayer = new DiscPlayer();
    playerMap.put(uuid, discPlayer);

    discPlayer.block = block;
    discPlayer.soundFilePath = soundFilePath;
    discPlayer.playerUUID = uuid;
    discPlayer.audioChannel = api.createLocationalAudioChannel(uuid, api.fromServerLevel(block.getWorld()), api.createPosition(block.getLocation().getX() + 0.5d, block.getLocation().getY() + 0.5d, block.getLocation().getZ() + 0.5d));

    if (discPlayer.audioChannel == null) return;

    discPlayer.audioChannel.setCategory(VoicePlugin.MUSIC_DISC_CATEGORY);
    discPlayer.audioChannel.setDistance(CustomDiscsConfiguration.musicDiscDistance);

    discPlayer.playersInRange = api.getPlayersInRange(api.fromServerLevel(block.getWorld()), api.createPosition(block.getLocation().getX() + 0.5d, block.getLocation().getY() + 0.5d, block.getLocation().getZ() + 0.5d), CustomDiscsConfiguration.musicDiscDistance);

    discPlayer.audioPlayerThread.start();

    for (ServerPlayer serverPlayer : discPlayer.playersInRange) {
      Player bukkitPlayer = (Player) serverPlayer.getPlayer();
      plugin.getAudience().sender(bukkitPlayer).sendActionBar(actionbarComponent);
    }
  }

  private AudioPlayer playChannel(DiscPlayer discPlayer) {
    VoicechatServerApi api = VoicePlugin.voicechatApi;

    try {
      short[] audio = readSoundFile(discPlayer.soundFilePath);
      AudioPlayer audioPlayer = api.createAudioPlayer(discPlayer.audioChannel, api.createEncoder(), audio);
      audioPlayer.startPlaying();
      return audioPlayer;
    } catch (Exception e) {
      for (ServerPlayer serverPlayer : discPlayer.playersInRange) {
        Player bukkitPlayer = (Player) serverPlayer.getPlayer();
        plugin.sendMessage(bukkitPlayer, plugin.getLanguage().PComponent("disc-play-error"));
      }
      return null;
    }
  }

  private short[] readSoundFile(Path file) throws UnsupportedAudioFileException, IOException {
    return VoicePlugin.voicechatApi.getAudioConverter().bytesToShorts(convertFormat(file));
  }

  private byte[] convertFormat(Path file) throws UnsupportedAudioFileException, IOException {
    AudioInputStream finalInputStream = null;

    if (getFileExtension(file.toFile().toString()).equals("wav")) {
      AudioInputStream inputStream = AudioSystem.getAudioInputStream(file.toFile());
      finalInputStream = AudioSystem.getAudioInputStream(PlayerManager.FORMAT, inputStream);
    } else if (getFileExtension(file.toFile().toString()).equals("mp3")) {

      AudioInputStream inputStream = new MpegAudioFileReader().getAudioInputStream(file.toFile());
      AudioFormat baseFormat = inputStream.getFormat();
      AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getFrameRate(), false);
      AudioInputStream convertedInputStream = new MpegFormatConversionProvider().getAudioInputStream(decodedFormat, inputStream);
      finalInputStream = AudioSystem.getAudioInputStream(PlayerManager.FORMAT, convertedInputStream);

    } else if (getFileExtension(file.toFile().toString()).equals("flac")) {
      AudioInputStream inputStream = new FlacAudioFileReader().getAudioInputStream(file.toFile());
      AudioFormat baseFormat = inputStream.getFormat();
      AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getFrameRate(), false);
      AudioInputStream convertedInputStream = new Flac2PcmAudioInputStream(inputStream, decodedFormat, inputStream.getFrameLength());
      finalInputStream = AudioSystem.getAudioInputStream(PlayerManager.FORMAT, convertedInputStream);
    }

    assert finalInputStream != null;

    return adjustVolume(finalInputStream.readAllBytes(), CustomDiscsConfiguration.musicDiscVolume);
  }

  private byte[] adjustVolume(byte[] audioSamples, double volume) {

    if (volume > 1d || volume < 0d) {
      plugin.getServer().getLogger().severe("The volume must be between 0 and 1 in the config!");
      return null;
    }

    byte[] array = new byte[audioSamples.length];
    for (int i = 0; i < array.length; i += 2) {
      // convert byte pair to int
      short buf1 = audioSamples[i + 1];
      short buf2 = audioSamples[i];

      buf1 = (short) ((buf1 & 0xff) << 8);
      buf2 = (short) (buf2 & 0xff);

      short res = (short) (buf1 | buf2);
      res = (short) (res * volume);

      // convert back
      array[i] = (byte) res;
      array[i + 1] = (byte) (res >> 8);

    }
    return array;
  }

  public void stopPlaying(Block block) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    stopPlaying(uuid);
  }

  public void stopPlaying(UUID uuid) {
    if (playerMap.containsKey(uuid)) {
      CustomDiscs.debug(
          "Stopping Player {0}",
          uuid.toString());

      DiscPlayer discPlayer = playerMap.get(uuid);

      if (discPlayer.audioPlayer != null) {
        discPlayer.audioPlayer.stopPlaying();
      } else {
        playerMap.remove(uuid);

        discPlayer.audioPlayerThread.interrupt();
        HopperHandler.getInstance().discToHopper(discPlayer.block);
      }
    } else {
      CustomDiscs.debug(
          "Couldn't find Player {0} to stop",
          uuid.toString());
    }
  }

  public void stopAll() {
    Set.copyOf(playerMap.keySet()).forEach(this::stopPlaying);
  }

  public boolean isAudioPlayerPlaying(Location blockLocation) {
    UUID id = UUID.nameUUIDFromBytes(blockLocation.toString().getBytes());
    return playerMap.containsKey(id);
  }

  private String getFileExtension(String s) {
    int index = s.lastIndexOf(".");
    if (index > 0) {
      return s.substring(index + 1);
    } else {
      return "";
    }
  }

  private class DiscPlayer {
    private Path soundFilePath;
    private LocationalAudioChannel audioChannel;
    private Collection<ServerPlayer> playersInRange;
    private UUID playerUUID;
    private AudioPlayer audioPlayer;
    private Block block;

    private void startTrackJob() {
      try {
        audioPlayer = playChannel(this);

        if (audioPlayer == null) {
          CustomDiscs.debug("Excepted audioPlayer is null");
          stopPlaying(playerUUID);
          return;
        }

        audioPlayer.setOnStopped(() -> {
          CustomDiscs.debug(
              "VoiceChat AudioPlayer {0} got stop",
              playerUUID.toString());

          playerMap.remove(playerUUID);

          audioPlayerThread.interrupt();
          HopperHandler.getInstance().discToHopper(block);
        });
      } catch (Throwable e) {
        for (ServerPlayer serverPlayer : playersInRange) {
          Player bukkitPlayer = (Player) serverPlayer.getPlayer();
          plugin.sendMessage(bukkitPlayer, plugin.getLanguage().PComponent("disc-play-error"));
          plugin.getLogger().log(Level.SEVERE, "Error while playing disc: ", e);
        }
      }
    }

    private final Thread audioPlayerThread = new Thread(this::startTrackJob, "AudioPlayerThread");


  }
}
