package io.github.subkek.customdiscs;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import io.github.subkek.customdiscs.config.CustomDiscsConfiguration;
import io.github.subkek.customdiscs.util.Formatter;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class LavaPlayerManager {
  private final CustomDiscs plugin = CustomDiscs.getInstance();

  private final AudioPlayerManager lavaPlayerManager = new DefaultAudioPlayerManager();
  private final Map<UUID, LavaPlayer> playerMap = new HashMap<>();

  public LavaPlayerManager() {
    lavaPlayerManager.registerSourceManager(new YoutubeAudioSourceManager(
        false,
        CustomDiscsConfiguration.youtubeEmail.isBlank() ? null : CustomDiscsConfiguration.youtubeEmail,
        CustomDiscsConfiguration.youtubePassword.isBlank() ? null : CustomDiscsConfiguration.youtubePassword
    ));
  }

  public void playLocationalAudioYoutube(Block block, VoicechatServerApi api, String ytUrl, Component actionbarComponent) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    if (playerMap.containsKey(uuid)) stopPlaying(uuid);

    LavaPlayer lavaPlayer = new LavaPlayer();
    playerMap.put(uuid, lavaPlayer);

    lavaPlayer.ytUrl = ytUrl;
    lavaPlayer.playerUUID = uuid;
    lavaPlayer.audioChannel = api.createLocationalAudioChannel(UUID.randomUUID(), api.fromServerLevel(block.getWorld()), api.createPosition(block.getLocation().getX() + 0.5d, block.getLocation().getY() + 0.5d, block.getLocation().getZ() + 0.5d));

    if (lavaPlayer.audioChannel == null) return;

    lavaPlayer.audioChannel.setCategory(VoicePlugin.MUSIC_DISC_CATEGORY);
    lavaPlayer.audioChannel.setDistance(CustomDiscsConfiguration.musicDiscDistance);

    lavaPlayer.playersInRange = api.getPlayersInRange(api.fromServerLevel(block.getWorld()), api.createPosition(block.getLocation().getX() + 0.5d, block.getLocation().getY() + 0.5d, block.getLocation().getZ() + 0.5d), CustomDiscsConfiguration.musicDiscDistance);

    lavaPlayer.lavaPlayerThread.start();

    for (ServerPlayer serverPlayer : lavaPlayer.playersInRange) {
      Player bukkitPlayer = (Player) serverPlayer.getPlayer();
      bukkitPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(BukkitComponentSerializer.legacy().serialize(actionbarComponent)));
    }
  }

  private static LavaPlayerManager instance;

  public static LavaPlayerManager getInstance() {
    if (Objects.isNull(instance)) {
      instance = new LavaPlayerManager();
    }
    return instance;
  }

  public void stopPlaying(Block block) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    stopPlaying(uuid);
  }

  public void stopPlaying(UUID uuid) {
    if (playerMap.containsKey(uuid)) {
      LavaPlayer lavaPlayer = playerMap.get(uuid);
      playerMap.remove(uuid);

      lavaPlayer.trackFuture.complete(null);
      lavaPlayer.audioPlayer.destroy();
      lavaPlayer.lavaPlayerThread.interrupt();
    }
  }

  public void stopPlayingAll() {
    playerMap.keySet().forEach(this::stopPlaying);
  }

  public boolean isAudioPlayerPlaying(Location blockLocation) {
    UUID id = UUID.nameUUIDFromBytes(blockLocation.toString().getBytes());
    return playerMap.containsKey(id);
  }

  private class LavaPlayer {
    private String ytUrl;
    private LocationalAudioChannel audioChannel;
    private Collection<ServerPlayer> playersInRange;
    private final Thread lavaPlayerThread = new Thread(this::startTrackJob, "LavaPlayer");
    private final CompletableFuture<AudioTrack> trackFuture = new CompletableFuture<>();
    private UUID playerUUID;
    private AudioPlayer audioPlayer;

    private void startTrackJob() {
      try {
        audioPlayer = lavaPlayerManager.createPlayer();

        lavaPlayerManager.loadItem(ytUrl, new AudioLoadResultHandler() {
          @Override
          public void trackLoaded(AudioTrack audioTrack) {
            trackFuture.complete(audioTrack);
          }

          @Override
          public void playlistLoaded(AudioPlaylist audioPlaylist) {
            trackFuture.complete(audioPlaylist.getSelectedTrack());
            LavaPlayerManager.getInstance().stopPlaying(playerUUID);
          }

          @Override
          public void noMatches() {
            for (ServerPlayer serverPlayer : playersInRange) {
              Player bukkitPlayer = (Player) serverPlayer.getPlayer();
              bukkitPlayer.sendMessage(Formatter.format(plugin.language.get("url-no-matches-error"), true));
            }
            trackFuture.complete(null);
            stopPlaying(playerUUID);
          }

          @Override
          public void loadFailed(FriendlyException e) {
            for (ServerPlayer serverPlayer : playersInRange) {
              Player bukkitPlayer = (Player) serverPlayer.getPlayer();
              bukkitPlayer.sendMessage(Formatter.format(plugin.language.get("audio-load-error"), true));
            }
            trackFuture.complete(null);
            stopPlaying(playerUUID);
          }
        });

        if (lavaPlayerThread.isInterrupted()) {
          trackFuture.complete(null);
          return;
        }

        AudioTrack audioTrack;
        if (Objects.isNull(audioTrack = trackFuture.get())) {
          stopPlaying(playerUUID);
          return;
        }

        int volume = Math.round(CustomDiscsConfiguration.musicDiscVolume * 100);
        audioPlayer.setVolume(volume);

        long start = 0L;

        audioPlayer.playTrack(audioTrack);

        try {
          while (!audioTrack.getState().equals(AudioTrackState.FINISHED)) {
            AudioFrame frame;
            if (Objects.isNull(frame = audioPlayer.provide(5L, TimeUnit.MILLISECONDS))) continue;

            audioChannel.send(frame.getData());

            if (start == 0L) start = System.currentTimeMillis();
            long wait = (start + frame.getTimecode()) - System.currentTimeMillis();
            if (wait > 0) {TimeUnit.MILLISECONDS.sleep(wait);}
          }
        } catch (InterruptedException e) {
          // ignored
        }

        LavaPlayerManager.getInstance().stopPlaying(playerUUID);
      } catch (Throwable e) {
        for (ServerPlayer serverPlayer : playersInRange) {
          Player bukkitPlayer = (Player) serverPlayer.getPlayer();
          bukkitPlayer.sendMessage(Formatter.format(plugin.language.get("disc-play-error"), true));
          plugin.getLogger().log(Level.SEVERE, "Error while playing disc: ", e);
        }
      }
    }
  }
}

