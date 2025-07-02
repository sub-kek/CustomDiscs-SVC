package io.github.subkek.customdiscs;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.AudioPlayer;
import de.maxhenkel.voicechat.api.audiochannel.EntityAudioChannel;
import javazoom.spi.mpeg.sampled.convert.MpegFormatConversionProvider;
import javazoom.spi.mpeg.sampled.file.MpegAudioFileReader;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jflac.sound.spi.Flac2PcmAudioInputStream;
import org.jflac.sound.spi.FlacAudioFileReader;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class HornPlayerManager {
  private static final AudioFormat FORMAT = new AudioFormat(
      AudioFormat.Encoding.PCM_SIGNED,
      48000F,
      16,
      1,
      2,
      48000F,
      false
  );
  

  
  private final CustomDiscs plugin = CustomDiscs.getPlugin();
  private final Map<UUID, HornPlayer> playerMap = new HashMap<>();

  private static HornPlayerManager instance;

  public synchronized static HornPlayerManager getInstance() {
    if (instance == null) return instance = new HornPlayerManager();
    return instance;
  }

  public void playHorn(Path soundFilePath, Player player, Component actionbarComponent) {
    UUID playerUUID = player.getUniqueId();
    CustomDiscs.debug("Horn Player UUID is {0}", playerUUID);
    if (playerMap.containsKey(playerUUID)) stopPlaying(playerUUID);
    CustomDiscs.debug("Horn Player {0} not already exists", playerUUID);

    VoicechatServerApi api = CDVoiceAddon.getInstance().getVoicechatApi();

    HornPlayer hornPlayer = new HornPlayer();
    playerMap.put(playerUUID, hornPlayer);

    hornPlayer.soundFilePath = soundFilePath;
    hornPlayer.playerUUID = playerUUID;
    hornPlayer.bukkitPlayer = player;
    
    // Create entity audio channel that follows the player
    hornPlayer.audioChannel = api.createEntityAudioChannel(
        UUID.randomUUID(),
        api.fromServerPlayer(player)
    );

    if (hornPlayer.audioChannel == null) return;

    hornPlayer.audioChannel.setCategory(CDVoiceAddon.CUSTOM_HORN_CATEGORY);
    hornPlayer.audioChannel.setDistance(plugin.getCDConfig().getHornDistance());

    hornPlayer.audioPlayerThread.start();

    // Send actionbar message to the player
    plugin.getAudience().sender(player).sendActionBar(actionbarComponent);
    
    // Schedule automatic stop after configured horn duration
    int hornTimeout = plugin.getCDConfig().getHornTimeout();
    hornPlayer.autoStopTask = new BukkitRunnable() {
      @Override
      public void run() {
        CustomDiscs.debug("Auto-stopping horn for player {0} after {1} seconds", playerUUID, hornTimeout);
        stopPlaying(playerUUID);
      }
    }.runTaskLater(plugin, hornTimeout * 20L); // Convert seconds to ticks
  }

  private AudioPlayer playChannel(HornPlayer hornPlayer) {
    VoicechatServerApi api = CDVoiceAddon.getInstance().getVoicechatApi();

    try {
      short[] audio = readSoundFile(hornPlayer.soundFilePath);
      AudioPlayer audioPlayer = api.createAudioPlayer(hornPlayer.audioChannel, api.createEncoder(), audio);
      if (hornPlayer.audioPlayerThread.isInterrupted()) {
        CustomDiscs.debug("Horn Player {0} return AudioPlayer null because thread interrupted", hornPlayer.playerUUID);
        return null;
      }
      audioPlayer.startPlaying();
      return audioPlayer;
    } catch (Exception e) {
      CustomDiscs.sendMessage(hornPlayer.bukkitPlayer, plugin.getLanguage().PComponent("error.play.while-playing"));
      return null;
    }
  }

  private short[] readSoundFile(Path file) throws UnsupportedAudioFileException, IOException {
    return CDVoiceAddon.getInstance().getVoicechatApi().getAudioConverter().bytesToShorts(convertFormat(file));
  }

  private byte[] convertFormat(Path file) throws UnsupportedAudioFileException, IOException {
    AudioInputStream finalInputStream = null;

    if (getFileExtension(file.toFile().toString()).equals("wav")) {
      AudioInputStream inputStream = AudioSystem.getAudioInputStream(file.toFile());
      finalInputStream = AudioSystem.getAudioInputStream(HornPlayerManager.FORMAT, inputStream);
    } else if (getFileExtension(file.toFile().toString()).equals("mp3")) {

      AudioInputStream inputStream = new MpegAudioFileReader().getAudioInputStream(file.toFile());
      AudioFormat baseFormat = inputStream.getFormat();
      AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getFrameRate(), false);
      AudioInputStream convertedInputStream = new MpegFormatConversionProvider().getAudioInputStream(decodedFormat, inputStream);
      finalInputStream = AudioSystem.getAudioInputStream(HornPlayerManager.FORMAT, convertedInputStream);

    } else if (getFileExtension(file.toFile().toString()).equals("flac")) {
      AudioInputStream inputStream = new FlacAudioFileReader().getAudioInputStream(file.toFile());
      AudioFormat baseFormat = inputStream.getFormat();
      AudioFormat decodedFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, baseFormat.getSampleRate(), 16, baseFormat.getChannels(), baseFormat.getChannels() * 2, baseFormat.getFrameRate(), false);
      AudioInputStream convertedInputStream = new Flac2PcmAudioInputStream(inputStream, decodedFormat, inputStream.getFrameLength());
      finalInputStream = AudioSystem.getAudioInputStream(HornPlayerManager.FORMAT, convertedInputStream);
    }

    assert finalInputStream != null;

    return adjustVolume(finalInputStream.readAllBytes(), plugin.getCDConfig().getHornVolume());
  }

  private byte[] adjustVolume(byte[] audioSamples, double volume) {
    if (volume > 1d || volume < 0d) {
      CustomDiscs.error("The horn volume must be between 0 and 1 in the config!");
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

  public void stopPlaying(Player player) {
    UUID uuid = player.getUniqueId();
    stopPlaying(uuid);
  }

  public void stopPlaying(UUID uuid) {
    if (playerMap.containsKey(uuid)) {
      CustomDiscs.debug(
          "Stopping Horn Player {0}",
          uuid.toString());

      HornPlayer hornPlayer = playerMap.get(uuid);
      
      // Cancel auto-stop task if it exists
      if (hornPlayer.autoStopTask != null && !hornPlayer.autoStopTask.isCancelled()) {
        hornPlayer.autoStopTask.cancel();
      }

      if (hornPlayer.audioPlayer != null) {
        hornPlayer.audioPlayer.stopPlaying();
      } else {
        playerMap.remove(uuid);
        hornPlayer.audioPlayerThread.interrupt();
      }
    } else {
      CustomDiscs.debug(
          "Couldn't find Horn Player {0} to stop",
          uuid.toString());
    }
  }

  public void stopPlayingAll() {
    Set.copyOf(playerMap.keySet()).forEach(this::stopPlaying);
  }

  private String getFileExtension(String s) {
    int lastIndexOf = s.lastIndexOf(".");
    if (lastIndexOf == -1) {
      return "";
    }
    return s.substring(lastIndexOf + 1);
  }

  private class HornPlayer {
    private Path soundFilePath;
    private EntityAudioChannel audioChannel;
    private Player bukkitPlayer;
    private UUID playerUUID;
    private BukkitTask autoStopTask;
    private final Thread audioPlayerThread = new Thread(this::startTrackJob, "HornAudioPlayerThread");
    private AudioPlayer audioPlayer;

    private void startTrackJob() {
      try {
        Thread.sleep(100);
      } catch (InterruptedException ignored) {
        return;
      }

      audioPlayer = playChannel(this);

      if (audioPlayer == null) {
        playerMap.remove(playerUUID);
        return;
      }

      while (!audioPlayerThread.isInterrupted() && audioPlayer.isPlaying()) {
        try {
          Thread.sleep(100);
        } catch (InterruptedException ignored) {
          break;
        }
      }

      playerMap.remove(playerUUID);
    }
  }
} 