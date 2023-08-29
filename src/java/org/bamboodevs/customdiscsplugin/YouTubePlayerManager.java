package org.bamboodevs.customdiscsplugin;

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
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class YouTubePlayerManager {
    private final Map<UUID, YouTubePlayerManager.Stoppable> playerMap;
    private final AudioPlayerManager lavaPlayerManager = new DefaultAudioPlayerManager();
    private final ExecutorService executorService;

    public YouTubePlayerManager() {
        this.playerMap = new ConcurrentHashMap<>();
        this.executorService = Executors.newFixedThreadPool(100, r -> {
            Thread thread = new Thread(r, "AudioPlayerYouTubeThread");
            thread.setDaemon(true);
            return thread;
        });
        lavaPlayerManager.registerSourceManager(new YoutubeAudioSourceManager());
    }


    public void playLocationalAudioYoutube(VoicechatServerApi api, String ytUrl, Block block, Component actionbarComponent) {
        UUID id = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());

        LocationalAudioChannel audioChannel = api.createLocationalAudioChannel(id, api.fromServerLevel(block.getWorld()), api.createPosition(block.getLocation().getX() + 0.5d, block.getLocation().getY() + 0.5d, block.getLocation().getZ() + 0.5d));

        if (audioChannel == null) return;

        audioChannel.setCategory(VoicePlugin.MUSIC_DISC_CATEGORY);
        audioChannel.setDistance(CustomDiscs.getInstance().getConfig().getInt("music-disc-distance"));

        AtomicBoolean stopped = new AtomicBoolean();
        AtomicReference<AudioPlayer> player = new AtomicReference<>();

        playerMap.put(id, () -> {
            synchronized (stopped) {
                stopped.set(true);
                AudioPlayer audioPlayer = player.get();
                if (audioPlayer != null) {
                    audioPlayer.getPlayingTrack().stop();
                    audioPlayer.stopTrack();
                    audioPlayer.destroy();
                }
            }
        });

        executorService.execute(() -> {
            Collection<ServerPlayer> playersInRange = api.getPlayersInRange(api.fromServerLevel(block.getWorld()), api.createPosition(block.getLocation().getX() + 0.5d, block.getLocation().getY() + 0.5d, block.getLocation().getZ() + 0.5d), CustomDiscs.getInstance().getConfig().getInt("music-disc-distance"));

            AudioPlayer audioPlayer = playChannelYt(audioChannel, ytUrl, playersInRange);

            for (ServerPlayer serverPlayer : playersInRange) {
                Player bukkitPlayer = (Player) serverPlayer.getPlayer();
                bukkitPlayer.sendActionBar(actionbarComponent);
            }

            if (audioPlayer == null) {
                playerMap.remove(id);
                return;
            }

            synchronized (stopped) {
                if (!stopped.get()) {
                    player.set(audioPlayer);
                } else {
                    audioPlayer.getPlayingTrack().stop();
                    audioPlayer.stopTrack();
                    audioPlayer.destroy();
                }
            }
        });
    }

    @Nullable
    private com.sedmelluq.discord.lavaplayer.player.AudioPlayer playChannelYt(AudioChannel audioChannel, String YtUrl, Collection<ServerPlayer> playersInRange) {
        try {
            CompletableFuture<AudioTrack> trackFuture = new CompletableFuture<>();

            lavaPlayerManager.loadItem(YtUrl, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack audioTrack) {
                    trackFuture.complete(audioTrack);
                }

                @Override
                public void playlistLoaded(AudioPlaylist audioPlaylist) {

                }

                @Override
                public void noMatches() {

                }

                @Override
                public void loadFailed(FriendlyException e) {

                }
            });

            AudioTrack audioTrack = trackFuture.get();

            com.sedmelluq.discord.lavaplayer.player.AudioPlayer player = lavaPlayerManager.createPlayer();
            int volume = Math.round(Float.parseFloat(CustomDiscs.getInstance().getConfig().getString("music-disc-volume"))*100);
            player.setVolume(volume);
            player.playTrack(audioTrack);

            executorService.execute(() -> {
                long start = 0L;

                while (player.getPlayingTrack() != null) {
                    try {
                        AudioFrame frame = player.provide(5L, TimeUnit.MILLISECONDS);

                        audioChannel.send(frame.getData());

                        if (start == 0L)
                            start = System.currentTimeMillis();

                        long wait = (start + frame.getTimecode()) - System.currentTimeMillis();

                        TimeUnit.MILLISECONDS.sleep(wait);
                    } catch (Exception ignored) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                        } catch (InterruptedException ignored1) {}
                    }
                }
            });

            return player;
        } catch (Exception e) {
            for (ServerPlayer serverPlayer : playersInRange) {
                Player bukkitPlayer = (Player) serverPlayer.getPlayer();
                bukkitPlayer.sendMessage(ChatColor.RED + "Ошибка при воспроизведении диска!");
                e.printStackTrace();
            }
            return null;
        }
    }

    public void stopLocationalAudio(Location blockLocation) {
        UUID id = UUID.nameUUIDFromBytes(blockLocation.toString().getBytes());
        YouTubePlayerManager.Stoppable player = playerMap.get(id);
        if (player != null) {
            player.stop();
        }
        playerMap.remove(id);
    }

    public boolean isAudioPlayerPlaying(Location blockLocation) {
        UUID id = UUID.nameUUIDFromBytes(blockLocation.toString().getBytes());
        return playerMap.containsKey(id);
    }

    private static YouTubePlayerManager instance;

    public static YouTubePlayerManager instance() {
        if (instance == null) {
            instance = new YouTubePlayerManager();
        }
        return instance;
    }

    private interface Stoppable {
        void stop();
    }
}
