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
import dev.lavalink.youtube.clients.AndroidVr;
import dev.lavalink.youtube.clients.Web;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class LavaPlayerManager {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();
  private final AudioPlayerManager lavaPlayerManager = new DefaultAudioPlayerManager();
  private final Map<UUID, LavaPlayer> playerMap = new HashMap<>();
  File refreshTokenFile = new File(plugin.getDataFolder(), ".youtube-token");

  private static LavaPlayerManager instance;

  public static LavaPlayerManager getInstance() {
    if (instance == null) return instance = new LavaPlayerManager();
    return instance;
  }

  public LavaPlayerManager() {
    YoutubeAudioSourceManager source = new YoutubeAudioSourceManager(false, new Web());
    if (!plugin.getCDConfig().getPoToken().isBlank() && !plugin.getCDConfig().getPoVisitorData().isBlank()) {
      Web.setPoTokenAndVisitorData(plugin.getCDConfig().getPoToken(), plugin.getCDConfig().getPoVisitorData());
    } else if (plugin.getCDConfig().isYoutubeOauth2()) {
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
      } catch (Throwable e) {
        CustomDiscs.error("Error load Youtube OAuth2 token: ", e);
      }
    }
    lavaPlayerManager.registerSourceManager(source);
  }

  public void save() {
    for (AudioSourceManager manager : lavaPlayerManager.getSourceManagers()) {
      if (!(manager instanceof YoutubeAudioSourceManager)) continue;

      CustomDiscs.debug("Found Youtube source to save OAuth2");

      String refreshToken = ((YoutubeAudioSourceManager) manager).getOauth2RefreshToken();
      if (refreshToken == null) continue;

      CustomDiscs.debug("refreshToken not null");

      try {
        BufferedWriter writer = new BufferedWriter(new FileWriter(refreshTokenFile));
        writer.write(refreshToken);
        writer.close();
        CustomDiscs.debug("refreshToken written");
      } catch (IOException e) {
        CustomDiscs.error("Error save Youtube OAuth2 token: ", e);
      }
    }
  }

  public void play(Block block, String ytUrl, Component actionbarComponent) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    CustomDiscs.debug("LavaPlayer UUID is {0}", uuid);
    if (playerMap.containsKey(uuid)) stopPlaying(uuid);

    VoicechatServerApi api = CDVoiceAddon.getInstance().getVoicechatApi();

    LavaPlayer lavaPlayer = new LavaPlayer();
    playerMap.put(uuid, lavaPlayer);

    lavaPlayer.ytUrl = ytUrl;
    lavaPlayer.playerUUID = uuid;
    lavaPlayer.audioChannel = api.createLocationalAudioChannel(
        UUID.randomUUID(),
        api.fromServerLevel(block.getWorld()),
        api.createPosition(
            block.getLocation().getX() + 0.5d,
            block.getLocation().getY() + 0.5d,
            block.getLocation().getZ() + 0.5d
        )
    );

    if (lavaPlayer.audioChannel == null) return;

    lavaPlayer.audioChannel.setCategory(CDVoiceAddon.MUSIC_DISC_CATEGORY);
    lavaPlayer.audioChannel.setDistance(plugin.getCDData().getJukeboxDistance(block));

    lavaPlayer.playersInRange = api.getPlayersInRange(
        api.fromServerLevel(block.getWorld()),
        api.createPosition(
            block.getLocation().getX() + 0.5d,
            block.getLocation().getY() + 0.5d,
            block.getLocation().getZ() + 0.5d
        ),
        plugin.getCDData().getJukeboxDistance(block)
    );

    lavaPlayer.lavaPlayerThread.start();

    for (ServerPlayer serverPlayer : lavaPlayer.playersInRange) {
      Player bukkitPlayer = (Player) serverPlayer.getPlayer();
      plugin.getAudience().sender(bukkitPlayer).sendActionBar(actionbarComponent);
    }
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
    Set.copyOf(playerMap.keySet()).forEach(this::stopPlaying);
  }

  public boolean isPlaying(Block block) {
    UUID id = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    return playerMap.containsKey(id);
  }

  private class LavaPlayer {
    private final CompletableFuture<AudioTrack> trackFuture = new CompletableFuture<>();
    private String ytUrl;
    private LocationalAudioChannel audioChannel;
    private Collection<ServerPlayer> playersInRange;
    private UUID playerUUID;
    private AudioPlayer audioPlayer;
    private final Thread lavaPlayerThread = new Thread(this::startTrackJob, "LavaPlayerThread");

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
              CustomDiscs.sendMessage(bukkitPlayer, plugin.getLanguage().PComponent("error.play.no-matches"));
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
              CustomDiscs.sendMessage(bukkitPlayer, plugin.getLanguage().PComponent("error.play.audio-load"));
            }
            stopPlaying(playerUUID);
            trackFuture.completeExceptionally(e);
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

        int volume = Math.round(plugin.getCDConfig().getMusicDiscVolume() * 100);
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
          CustomDiscs.debug("LavaPlayer {0} got InterruptedException its stop most likely", playerUUID);
          lavaPlayerThread.interrupt();
          return;
        } catch (Throwable e) {
          CustomDiscs.error("LavaPlayer {0} got Throwable Exception: {1}", e, playerUUID);
        }

        stopPlaying(playerUUID);
      } catch (Throwable e) {
        for (ServerPlayer serverPlayer : playersInRange) {
          Player bukkitPlayer = (Player) serverPlayer.getPlayer();
          CustomDiscs.sendMessage(bukkitPlayer, plugin.getLanguage().PComponent("error.play.while-playing"));
          CustomDiscs.error("Error while playing disc: ", e);
        }
      }
    }
  }
}
