package io.github.subkek.customdiscs;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import io.github.subkek.customdiscs.utils.Formatter;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class YouTubePlayerManager extends Thread {
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  private final AudioPlayerManager lavaPlayerManager = new DefaultAudioPlayerManager();
  public UUID uuid;
  public static Map<UUID, YouTubePlayerManager> playerMap = new ConcurrentHashMap<>();
  public AudioPlayer audioPlayer;
  public Block block;
  private LocationalAudioChannel audioChannel;
  private String ytUrl;
  private Collection<ServerPlayer> playersInRange;
  private final CompletableFuture<AudioTrack> trackFuture = new CompletableFuture<>();

  public YouTubePlayerManager(Block block) {
    lavaPlayerManager.registerSourceManager(new YoutubeAudioSourceManager());
    audioPlayer = lavaPlayerManager.createPlayer();
    uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    this.block = block;
    playerMap.put(uuid, this);
  }

  public void playLocationalAudioYoutube(VoicechatServerApi api, String ytUrl, Component actionbarComponent) {
    this.ytUrl = ytUrl;

    audioChannel = api.createLocationalAudioChannel(UUID.randomUUID(), api.fromServerLevel(block.getWorld()), api.createPosition(block.getLocation().getX() + 0.5d, block.getLocation().getY() + 0.5d, block.getLocation().getZ() + 0.5d));

    if (audioChannel == null) return;

    audioChannel.setCategory(VoicePlugin.MUSIC_DISC_CATEGORY);
    audioChannel.setDistance(plugin.config.getMusicDiscDistance());

    playersInRange = api.getPlayersInRange(api.fromServerLevel(block.getWorld()), api.createPosition(block.getLocation().getX() + 0.5d, block.getLocation().getY() + 0.5d, block.getLocation().getZ() + 0.5d), plugin.config.getMusicDiscDistance());

    start();

    for (ServerPlayer serverPlayer : playersInRange) {
      Player bukkitPlayer = (Player) serverPlayer.getPlayer();
      bukkitPlayer.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(BukkitComponentSerializer.legacy().serialize(actionbarComponent)));
    }
  }

  @Override
  public void run() {
    try {
      lavaPlayerManager.loadItem(ytUrl, new AudioLoadResultHandler() {
        @Override
        public void trackLoaded(AudioTrack audioTrack) {
          trackFuture.complete(audioTrack);
        }

        @Override
        public void playlistLoaded(AudioPlaylist audioPlaylist) {
          for (ServerPlayer serverPlayer : playersInRange) {
            Player bukkitPlayer = (Player) serverPlayer.getPlayer();
            bukkitPlayer.sendMessage(Formatter.format(plugin.language.get("cant-play-playlist-error"), true));
          }
          trackFuture.complete(null);
          stopPlaying(block);
        }

        @Override
        public void noMatches() {
          for (ServerPlayer serverPlayer : playersInRange) {
            Player bukkitPlayer = (Player) serverPlayer.getPlayer();
            bukkitPlayer.sendMessage(Formatter.format(plugin.language.get("url-no-matches-error"), true));
          }
          trackFuture.complete(null);
          stopPlaying(block);
        }

        @Override
        public void loadFailed(FriendlyException e) {
          for (ServerPlayer serverPlayer : playersInRange) {
            Player bukkitPlayer = (Player) serverPlayer.getPlayer();
            bukkitPlayer.sendMessage(Formatter.format(plugin.language.get("audio-load-error"), true));
          }
          trackFuture.complete(null);
          stopPlaying(block);
        }
      });

      if (isInterrupted()) {
        trackFuture.complete(null);
        return;
      }

      AudioTrack audioTrack = trackFuture.get();

      int volume = Math.round(plugin.config.getMusicDiscVolume() * 100);
      audioPlayer.setVolume(volume);

      long start = 0L;

      audioPlayer.playTrack(audioTrack);

      while (audioPlayer.getPlayingTrack() != null) {
        try {
          AudioFrame frame = audioPlayer.provide(5L, TimeUnit.MILLISECONDS);

          audioChannel.send(frame.getData());

          if (start == 0L)
            start = System.currentTimeMillis();

          long wait = (start + frame.getTimecode()) - System.currentTimeMillis();

          TimeUnit.MILLISECONDS.sleep(wait);
        } catch (Exception e) {
          TimeUnit.MILLISECONDS.sleep(10);
        }
      }

      stopPlaying(this.block);
    } catch (Exception e) {
      for (ServerPlayer serverPlayer : playersInRange) {
        Player bukkitPlayer = (Player) serverPlayer.getPlayer();
        bukkitPlayer.sendMessage(Formatter.format(plugin.language.get("disc-play-error"), true));
        plugin.getLogger().log(Level.SEVERE, "Error while playing disc: ", e);
      }
    }
  }

  public static boolean isAudioPlayerPlaying(Location blockLocation) {
    UUID id = UUID.nameUUIDFromBytes(blockLocation.toString().getBytes());
    return playerMap.containsKey(id);
  }

  public static YouTubePlayerManager instance(Block block) {
    return new YouTubePlayerManager(block);
  }

  public static void stopPlaying(Block block) {
    UUID _uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());

    if (playerMap.containsKey(_uuid)) {
      YouTubePlayerManager tubePlayer = playerMap.get(_uuid);
      playerMap.remove(tubePlayer.uuid);

      tubePlayer.trackFuture.complete(null);
      tubePlayer.audioPlayer.destroy();
      tubePlayer.interrupt();
    }
  }
}