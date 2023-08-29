package org.bamboodevs.customdiscsplugin;

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
import de.maxhenkel.voicechat.api.audiochannel.AudioChannel;
import de.maxhenkel.voicechat.api.audiochannel.LocationalAudioChannel;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

public class YouTubePlayer {
    private final AudioPlayerManager lavaPlayerManager = new DefaultAudioPlayerManager();
    public ExecutorService executorService;
    public UUID uuid;
    public static Map<UUID, YouTubePlayer> playerMap = new ConcurrentHashMap<>();;
    public AudioPlayer audioPlayer;
    public Block block;

    public YouTubePlayer(Block block) {
        executorService = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            return thread;
        });
        lavaPlayerManager.registerSourceManager(new YoutubeAudioSourceManager());
        audioPlayer = lavaPlayerManager.createPlayer();
        uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
        this.block = block;
        playerMap.put(uuid, this);
    }


    public void playLocationalAudioYoutube(VoicechatServerApi api, String ytUrl, Component actionbarComponent) {
        LocationalAudioChannel audioChannel = api.createLocationalAudioChannel(uuid, api.fromServerLevel(block.getWorld()), api.createPosition(block.getLocation().getX() + 0.5d, block.getLocation().getY() + 0.5d, block.getLocation().getZ() + 0.5d));

        if (audioChannel == null) return;

        audioChannel.setCategory(VoicePlugin.MUSIC_DISC_CATEGORY);
        audioChannel.setDistance(CustomDiscs.getInstance().getConfig().getInt("music-disc-distance"));

        executorService.execute(() -> {
            Collection<ServerPlayer> playersInRange = api.getPlayersInRange(api.fromServerLevel(block.getWorld()), api.createPosition(block.getLocation().getX() + 0.5d, block.getLocation().getY() + 0.5d, block.getLocation().getZ() + 0.5d), CustomDiscs.getInstance().getConfig().getInt("music-disc-distance"));

            playChannelYt(audioChannel, ytUrl, playersInRange);

            for (ServerPlayer serverPlayer : playersInRange) {
                Player bukkitPlayer = (Player) serverPlayer.getPlayer();
                bukkitPlayer.sendActionBar(actionbarComponent);
            }
        });
    }

    private void playChannelYt(AudioChannel audioChannel, String YtUrl, Collection<ServerPlayer> playersInRange) {
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

            int volume = Math.round(Float.parseFloat(CustomDiscs.getInstance().getConfig().getString("music-disc-volume"))*100);
            audioPlayer.setVolume(volume);
            audioPlayer.playTrack(audioTrack);

            executorService.execute(() -> {
                long start = 0L;

                while (audioPlayer.getPlayingTrack() != null) {
                    try {
                        AudioFrame frame = audioPlayer.provide(5L, TimeUnit.MILLISECONDS);

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

        } catch (Exception e) {
            for (ServerPlayer serverPlayer : playersInRange) {
                Player bukkitPlayer = (Player) serverPlayer.getPlayer();
                bukkitPlayer.sendMessage(ChatColor.RED + "Ошибка при воспроизведении диска!");
                e.printStackTrace();
            }
        }
    }

    public boolean isAudioPlayerPlaying(Location blockLocation) {
        UUID id = UUID.nameUUIDFromBytes(blockLocation.toString().getBytes());
        return playerMap.containsKey(id);
    }

    public static YouTubePlayer instance(Block block) {

        return new YouTubePlayer(block);
    }

    public static void stopPlaying(Block block) {
        UUID _uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());

        if (playerMap.containsKey(_uuid)) {
            YouTubePlayer tubePlayer = playerMap.get(_uuid);

            tubePlayer.audioPlayer.getPlayingTrack().stop();
            tubePlayer.audioPlayer.stopTrack();
            tubePlayer.audioPlayer.destroy();
            tubePlayer.executorService.shutdownNow();

            playerMap.remove(tubePlayer.uuid);
        }
    }
}