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

public class YouTubePlayerManager extends Thread {
    private final AudioPlayerManager lavaPlayerManager = new DefaultAudioPlayerManager();
    public UUID uuid;
    public static Map<UUID, YouTubePlayerManager> playerMap = new ConcurrentHashMap<>();
    public AudioPlayer audioPlayer;
    public Block block;
    private LocationalAudioChannel audioChannel;
    private String ytUrl;
    private Collection<ServerPlayer> playersInRange;

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
        audioChannel.setDistance(CustomDiscs.getInstance().getConfig().getInt("music-disc-distance"));

        playersInRange = api.getPlayersInRange(api.fromServerLevel(block.getWorld()), api.createPosition(block.getLocation().getX() + 0.5d, block.getLocation().getY() + 0.5d, block.getLocation().getZ() + 0.5d), CustomDiscs.getInstance().getConfig().getInt("music-disc-distance"));

        start();

        for (ServerPlayer serverPlayer : playersInRange) {
            Player bukkitPlayer = (Player) serverPlayer.getPlayer();
            bukkitPlayer.sendActionBar(actionbarComponent);
        }
    }

    @Override
    public void run() {
        try {
            CompletableFuture<AudioTrack> trackFuture = new CompletableFuture<>();

            lavaPlayerManager.loadItem(ytUrl, new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack audioTrack) {
                    trackFuture.complete(audioTrack);
                }

                @Override
                public void playlistLoaded(AudioPlaylist audioPlaylist) {
                    for (ServerPlayer serverPlayer : playersInRange) {
                        Player bukkitPlayer = (Player) serverPlayer.getPlayer();
                        bukkitPlayer.sendMessage(ChatColor.RED + "Невозможно воспроизвести плейлист");
                    }
                    trackFuture.complete(null);
                    stopPlaying(block);
                }

                @Override
                public void noMatches() {
                    for (ServerPlayer serverPlayer : playersInRange) {
                        Player bukkitPlayer = (Player) serverPlayer.getPlayer();
                        bukkitPlayer.sendMessage(ChatColor.RED + "Совпадений по URL не найдено");
                    }
                    trackFuture.complete(null);
                    stopPlaying(block);
                }

                @Override
                public void loadFailed(FriendlyException e) {
                    for (ServerPlayer serverPlayer : playersInRange) {
                        Player bukkitPlayer = (Player) serverPlayer.getPlayer();
                        bukkitPlayer.sendMessage(ChatColor.RED + "Ошибка загрузки видео!");
                    }
                    trackFuture.complete(null);
                    stopPlaying(block);
                }
            });

            if (isInterrupted())
                return;

            AudioTrack audioTrack = trackFuture.get();

            int volume = Math.round(Float.parseFloat(CustomDiscs.getInstance().getConfig().getString("music-disc-volume"))*100);
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
        } catch (Exception e) {
            for (ServerPlayer serverPlayer : playersInRange) {
                Player bukkitPlayer = (Player) serverPlayer.getPlayer();
                bukkitPlayer.sendMessage(ChatColor.RED + "Ошибка при воспроизведении диска!");
                e.printStackTrace();
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

            tubePlayer.audioPlayer.destroy();
            tubePlayer.interrupt();
        }
    }
}