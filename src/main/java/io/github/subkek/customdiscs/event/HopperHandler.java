package io.github.subkek.customdiscs.event;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.LavaPlayerManager;
import io.github.subkek.customdiscs.PlayerManager;
import io.github.subkek.customdiscs.VoicePlugin;
import io.github.subkek.customdiscs.config.CustomDiscsConfiguration;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Jukebox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.nio.file.Path;
import java.util.Objects;

public class HopperHandler implements Listener {
  CustomDiscs plugin = CustomDiscs.getInstance();

  PlayerManager playerManager = PlayerManager.instance();
  LavaPlayerManager lavaPlayerManager = LavaPlayerManager.getInstance();

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onJukeboxInsertFromHopper(InventoryMoveItemEvent event) {

    if (event.getDestination().getLocation() == null) return;
    if (!event.getDestination().getLocation().getBlock().getType().equals(Material.JUKEBOX)) return;

    boolean isCustomDisc = isCustomMusicDisc(event.getItem());
    boolean isYouTubeCustomDisc = isCustomMusicDiscYouTube(event.getItem());

    Block block = event.getDestination().getLocation().getBlock();

    if (isCustomDisc && !jukeboxContainsDisc(block)) {
      CustomDiscsConfiguration.discsPlayed++;

      String soundFileName = event.getItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "customdisc"), PersistentDataType.STRING);

      Path soundFilePath = Path.of(plugin.getDataFolder().getPath(), "musicdata", soundFileName);

      if (soundFilePath.toFile().exists()) {
        String songName = Objects.requireNonNull(event.getItem().getItemMeta().getLore()).get(0);
        songName = songName.replace("§7", "<gray>");

        Component customActionBarSongPlaying = plugin.getLanguage().component("now-playing", songName);

        assert VoicePlugin.voicechatServerApi != null;
        playerManager.playLocationalAudio(VoicePlugin.voicechatServerApi, soundFilePath, block, customActionBarSongPlaying);
      }
    }

    if (isYouTubeCustomDisc && !jukeboxContainsDisc(block)) {
      CustomDiscsConfiguration.discsPlayed++;

      String soundLink = event.getItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "customdiscyt"), PersistentDataType.STRING);

      String songName = Objects.requireNonNull(event.getItem().getItemMeta().getLore()).get(0);
      songName = songName.replace("§7", "<gray>");

      Component customActionBarSongPlaying = plugin.getLanguage().component("now-playing", songName);

      assert VoicePlugin.voicechatServerApi != null;
      lavaPlayerManager.playLocationalAudioYoutube(block, VoicePlugin.voicechatServerApi, soundLink, customActionBarSongPlaying);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onJukeboxEjectToHopper(InventoryMoveItemEvent event) {
    if (event.getSource().getLocation() == null) return;
    Block block = event.getSource().getLocation().getBlock();
    if (!block.getType().equals(Material.JUKEBOX)) return;
    if (event.getItem().getItemMeta() == null) return;
    if (!isCustomMusicDisc(event.getItem()) && !isCustomMusicDiscYouTube(event.getItem())) return;

//    CustomDiscs.debug("Hopper try move dicsc");

    event.setCancelled(playerManager.isAudioPlayerPlaying(block.getLocation()) ||
        lavaPlayerManager.isAudioPlayerPlaying(block.getLocation()));
  }

  public void discToHopper(Block block) {
    if (block == null) return;
    if (!block.getLocation().getChunk().isLoaded()) return;

    plugin.getFoliaLib().getImpl().runAtLocation(block.getLocation(), task -> {
      if (!block.getType().equals(Material.JUKEBOX)) return;

      Jukebox jukebox = (Jukebox) block.getState();

      block.setType(Material.JUKEBOX);
      jukebox.update(true, true);
      jukebox.stopPlaying();
    });
  }

  public boolean jukeboxContainsDisc(Block b) {
    Jukebox jukebox = (Jukebox) b.getLocation().getBlock().getState();
    return jukebox.getRecord().getType() != Material.AIR;
  }

  public boolean isCustomMusicDisc(ItemStack item) {
    if (item == null) return false;
    return item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "customdisc"), PersistentDataType.STRING);
  }

  public boolean isCustomMusicDiscYouTube(ItemStack item) {
    if (item == null) return false;
    return item.getItemMeta().getPersistentDataContainer().has(new NamespacedKey(plugin, "customdiscyt"), PersistentDataType.STRING);
  }

  private static HopperHandler instance;

  public static HopperHandler instance() {
    if (instance == null) {
      instance = new HopperHandler();
    }
    return instance;
  }
}
