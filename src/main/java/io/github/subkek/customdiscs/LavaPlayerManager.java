package io.github.subkek.customdiscs;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackState;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import de.maxhenkel.voicechat.api.ServerPlayer;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import dev.lavalink.youtube.YoutubeAudioSourceManager;
import dev.lavalink.youtube.http.YoutubeOauth2Handler;
import io.github.subkek.customdiscs.config.CustomDiscsConfiguration;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class LavaPlayerManager {
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  File refreshTokenFile = new File(plugin.getDataFolder(), ".youtube-token");

  private final AudioPlayerManager lavaPlayerManager = new DefaultAudioPlayerManager();
  private final Map<UUID, LavaPlayer> playerMap = new HashMap<>();

  public LavaPlayerManager() {
    lavaPlayerManager.registerSourceManager(new YoutubeAudioSourceManager(false));
    YoutubeAudioSourceManager source = new YoutubeAudioSourceManager(false);
    if (CustomDiscsConfiguration.oauth2) {
      try {
        String oauth2token;

        if (!refreshTokenFile.isFile() || !refreshTokenFile.exists()) oauth2token = null;
        else {
          StringBuilder tokenBuilder = new StringBuilder();
          BufferedReader bufferedReader = new BufferedReader(new FileReader(refreshTokenFile));

          for (String line : bufferedReader.lines().toList()) {
            tokenBuilder.append(line);
          }

          oauth2token = tokenBuilder.toString().trim();
        }

        source.useOauth2(oauth2token, false);

        if (oauth2token != null) {
          Field oauth2HandlerField = YoutubeAudioSourceManager.class.getDeclaredField("oauth2Handler");
          oauth2HandlerField.setAccessible(true);
          YoutubeOauth2Handler oauth2Handler = (YoutubeOauth2Handler) oauth2HandlerField.get(source);

          Field enabledField = YoutubeOauth2Handler.class.getDeclaredField("enabled");
          enabledField.setAccessible(true);
          enabledField.set(oauth2Handler, true);
        }
      } catch (Throwable e) {
        plugin.getLogger().log(Level.SEVERE, "Error load Youtube OAuth2 token: ", e);
      }
    }
  }

  public void save() {
    for (AudioSourceManager manager : lavaPlayerManager.getSourceManagers()) {
      if (!(manager instanceof YoutubeAudioSourceManager)) continue;

      String refreshToken = ((YoutubeAudioSourceManager) manager).getOauth2RefreshToken();
      if (refreshToken == null) continue;

      try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(refreshTokenFile));
        writer.write(refreshToken);
      } catch (IOException e) {
        plugin.getLogger().log(Level.SEVERE, "Error save Youtube OAuth2 token: ", e);
      }
    }
  }

  public void playLocationalAudioYoutube(Block block, VoicechatServerApi api, String ytUrl, Component actionbarComponent) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    CustomDiscs.debug("LavaPlayer UUID is {0}", uuid.toString());
    if (playerMap.containsKey(uuid)) stopPlaying(uuid);
    CustomDiscs.debug("LavaPlayer {0} not already exists", uuid.toString());

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
      plugin.getAudience().sender(bukkitPlayer).sendActionBar(actionbarComponent);
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
      CustomDiscs.debug(
          "Stopping LavaPlayer {0}",
          uuid.toString());

      LavaPlayer lavaPlayer = playerMap.remove(uuid);

      lavaPlayer.trackFuture.complete(null);
      lavaPlayer.audioPlayer.destroy();
      lavaPlayer.lavaPlayerThread.interrupt();
    } else {
      CustomDiscs.debug(
          "Couldn't find LavaPlayer {0} to stop",
          uuid.toString());
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
            CustomDiscs.debug(
                "LavaPlayer {0} loaded track {1} successfully",
                playerUUID.toString(), audioTrack.getInfo().title);
            trackFuture.complete(audioTrack);
          }

          @Override
          public void playlistLoaded(AudioPlaylist audioPlaylist) {
            AudioTrack selected = audioPlaylist.getSelectedTrack();
            CustomDiscs.debug(
                "LavaPlayer {0} loaded track {1} from playlist successfully",
                playerUUID.toString(), selected.getInfo().title);
            trackFuture.complete(selected);
          }

          @Override
          public void noMatches() {
            CustomDiscs.debug(
                "LavaPlayer {0} not found the track {1}",
                playerUUID.toString(), ytUrl);
            for (ServerPlayer serverPlayer : playersInRange) {
              Player bukkitPlayer = (Player) serverPlayer.getPlayer();
              plugin.sendMessage(bukkitPlayer, plugin.getLanguage().PComponent("url-no-matches-error"));
            }
            stopPlaying(playerUUID);
          }

          @Override
          public void loadFailed(FriendlyException e) {
            CustomDiscs.debug(
                "LavaPlayer {0} failed to load the track {1}. Exception msg: {2}",
                playerUUID.toString(), ytUrl, e.getMessage());
            for (ServerPlayer serverPlayer : playersInRange) {
              Player bukkitPlayer = (Player) serverPlayer.getPlayer();
              plugin.sendMessage(bukkitPlayer, plugin.getLanguage().PComponent("audio-load-error"));
            }
            stopPlaying(playerUUID);
          }
        });

        if (lavaPlayerThread.isInterrupted()) {
          return;
        }

        AudioTrack audioTrack;
        if (Objects.isNull(audioTrack = trackFuture.get())) {
          CustomDiscs.debug(
              "LavaPlayer {0} excepted track is null, interrupting and return",
              playerUUID.toString());
          if (!lavaPlayerThread.isInterrupted())
            stopPlaying(playerUUID);
          return;
        }

        int volume = Math.round(CustomDiscsConfiguration.musicDiscVolume * 100);
        audioPlayer.setVolume(volume);

        long start = 0L;

        audioPlayer.playTrack(audioTrack);

        try {
          while (audioPlayer.getPlayingTrack() != null && !lavaPlayerThread.isInterrupted()) {
            if (audioTrack.getState() == AudioTrackState.FINISHED) break;

            AudioFrame frame;
            if (Objects.isNull(frame = audioPlayer.provide(5L, TimeUnit.MILLISECONDS))) continue;

            audioChannel.send(frame.getData());

            if (start == 0L) start = System.currentTimeMillis();
            long wait = (start + frame.getTimecode()) - System.currentTimeMillis();
            if (wait > 0) {
              TimeUnit.MILLISECONDS.sleep(wait);
            }
          }
        } catch (InterruptedException e) {
          CustomDiscs.debug("LavaPlayer {0} got InterruptedException its stop most likely", playerUUID.toString());
          lavaPlayerThread.interrupt();
          return;
        } catch (Throwable e) {
          CustomDiscs.debug("LavaPlayer {0} got Throwable Exception: {1}", playerUUID.toString(), e.getMessage());
        }

        stopPlaying(playerUUID);
      } catch (Throwable e) {
        for (ServerPlayer serverPlayer : playersInRange) {
          Player bukkitPlayer = (Player) serverPlayer.getPlayer();
          plugin.sendMessage(bukkitPlayer, plugin.getLanguage().PComponent("disc-play-error"));
          plugin.getLogger().log(Level.SEVERE, "Error while playing disc: ", e);
        }
      }
    }
  }
}
