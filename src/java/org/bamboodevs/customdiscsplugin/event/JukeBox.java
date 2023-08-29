package org.bamboodevs.customdiscsplugin.event;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bamboodevs.customdiscsplugin.CustomDiscs;
import org.bamboodevs.customdiscsplugin.PlayerManager;
import org.bamboodevs.customdiscsplugin.VoicePlugin;
import org.bamboodevs.customdiscsplugin.YouTubePlayer;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class JukeBox implements Listener{

    CustomDiscs customDiscs = CustomDiscs.getInstance();
    PlayerManager playerManager = PlayerManager.instance();

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onInsert(PlayerInteractEvent event) throws IOException {

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || event.getItem() == null || event.getItem().getItemMeta() == null || block == null) return;
        if (event.getClickedBlock().getType() != Material.JUKEBOX) return;

        boolean isCustomDisc = isCustomMusicDisc(event.getItem());
        boolean isYouTubeCustomDisc = isCustomMusicDiscYouTube(event.getItem());

        if (isCustomDisc && !jukeboxContainsDisc(block)) {

            String soundFileName = event.getItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(customDiscs, "customdisc"), PersistentDataType.STRING);

            Path soundFilePath = Path.of(customDiscs.getDataFolder().getPath(), "musicdata", soundFileName);

            if (soundFilePath.toFile().exists()) {

                Component songNameComponent = Objects.requireNonNull(event.getItem().getItemMeta().lore()).get(0).asComponent();
                String songName = PlainTextComponentSerializer.plainText().serialize(songNameComponent);

                TextComponent customActionBarSongPlaying = Component.text()
                        .content("Сейчас играет: " + songName)
                        .color(NamedTextColor.GOLD)
                        .build();

                assert VoicePlugin.voicechatServerApi != null;
                playerManager.playLocationalAudio(VoicePlugin.voicechatServerApi, soundFilePath, block, customActionBarSongPlaying.asComponent());
            } else {
                player.sendMessage(ChatColor.RED + "Файл не найден.");
                event.setCancelled(true);
                throw new FileNotFoundException("Звуковой файл отсутствует!");
            }
        }

        if (isYouTubeCustomDisc && !jukeboxContainsDisc(block)) {
            if (!player.hasPermission("customdiscs.playt")) {
                player.sendMessage("§cКупите премиум для воспроизведения дисков с ютуба");
                return;
            }

            String soundLink = event.getItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(customDiscs, "customdiscyt"), PersistentDataType.STRING);

            System.out.println(soundLink);

            Component songNameComponent = Objects.requireNonNull(event.getItem().getItemMeta().lore()).get(0).asComponent();
            String songName = PlainTextComponentSerializer.plainText().serialize(songNameComponent);

            TextComponent customActionBarSongPlaying = Component.text()
                    .content("Сейчас играет: " + songName)
                    .color(NamedTextColor.GOLD)
                    .build();

            assert VoicePlugin.voicechatServerApi != null;
            YouTubePlayer.instance(block).playLocationalAudioYoutube(VoicePlugin.voicechatServerApi, soundLink, customActionBarSongPlaying);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onEject(PlayerInteractEvent event) {

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || block == null) return;
        if (event.getClickedBlock().getType() != Material.JUKEBOX) return;

        if (jukeboxContainsDisc(block)) {

            ItemStack itemInvolvedInEvent;
            if (event.getMaterial().equals(Material.AIR)) {

                if (!player.getInventory().getItemInMainHand().getType().equals(Material.AIR)) {
                    itemInvolvedInEvent = player.getInventory().getItemInMainHand();
                } else if (!player.getInventory().getItemInOffHand().getType().equals(Material.AIR)) {
                    itemInvolvedInEvent = player.getInventory().getItemInOffHand();
                } else {
                    itemInvolvedInEvent = new ItemStack(Material.AIR);
                }

            } else {
                itemInvolvedInEvent = new ItemStack(event.getMaterial());
            }

            if (player.isSneaking() && !itemInvolvedInEvent.getType().equals(Material.AIR)) return;

            if (jukeboxContainsDisc(block)) {
                stopDisc(block);
                YouTubePlayer.stopPlaying(block);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJukeboxBreak(BlockBreakEvent event) {

        Block block = event.getBlock();

        if (block.getType() != Material.JUKEBOX) return;

        stopDisc(block);
        YouTubePlayer.stopPlaying(block);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onJukeboxExplode(EntityExplodeEvent event) {

        for (Block explodedBlock : event.blockList()) {
            if (explodedBlock.getType() == Material.JUKEBOX) {
                stopDisc(explodedBlock);
                YouTubePlayer.stopPlaying(explodedBlock);
            }
        }

    }

    public boolean jukeboxContainsDisc(Block b) {
        Jukebox jukebox = (Jukebox) b.getLocation().getBlock().getState();
        return jukebox.getRecord().getType() != Material.AIR;
    }

    public boolean isCustomMusicDisc(ItemStack item) {

        if (item==null) return false;

        return item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(customDiscs, "customdisc"), PersistentDataType.STRING) &&
                (
                        item.getType().equals(Material.MUSIC_DISC_13) ||
                                item.getType().equals(Material.MUSIC_DISC_CAT) ||
                                item.getType().equals(Material.MUSIC_DISC_BLOCKS) ||
                                item.getType().equals(Material.MUSIC_DISC_CHIRP) ||
                                item.getType().equals(Material.MUSIC_DISC_FAR) ||
                                item.getType().equals(Material.MUSIC_DISC_MALL) ||
                                item.getType().equals(Material.MUSIC_DISC_MELLOHI) ||
                                item.getType().equals(Material.MUSIC_DISC_STAL) ||
                                item.getType().equals(Material.MUSIC_DISC_STRAD) ||
                                item.getType().equals(Material.MUSIC_DISC_WARD) ||
                                item.getType().equals(Material.MUSIC_DISC_11) ||
                                item.getType().equals(Material.MUSIC_DISC_WAIT) ||
                                item.getType().equals(Material.MUSIC_DISC_PIGSTEP)
                );
    }

    public boolean isCustomMusicDiscYouTube(ItemStack item) {

        if (item==null) return false;

        return item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(customDiscs, "customdiscyt"), PersistentDataType.STRING) &&
                (
                        item.getType().equals(Material.MUSIC_DISC_13) ||
                                item.getType().equals(Material.MUSIC_DISC_CAT) ||
                                item.getType().equals(Material.MUSIC_DISC_BLOCKS) ||
                                item.getType().equals(Material.MUSIC_DISC_CHIRP) ||
                                item.getType().equals(Material.MUSIC_DISC_FAR) ||
                                item.getType().equals(Material.MUSIC_DISC_MALL) ||
                                item.getType().equals(Material.MUSIC_DISC_MELLOHI) ||
                                item.getType().equals(Material.MUSIC_DISC_STAL) ||
                                item.getType().equals(Material.MUSIC_DISC_STRAD) ||
                                item.getType().equals(Material.MUSIC_DISC_WARD) ||
                                item.getType().equals(Material.MUSIC_DISC_11) ||
                                item.getType().equals(Material.MUSIC_DISC_WAIT) ||
                                item.getType().equals(Material.MUSIC_DISC_PIGSTEP)
                );
    }

    private void stopDisc(Block block) {
        playerManager.stopLocationalAudio(block.getLocation());
    }
}
