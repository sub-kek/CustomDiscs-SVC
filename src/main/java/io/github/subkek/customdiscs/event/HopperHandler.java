package io.github.subkek.customdiscs.event;

import io.github.subkek.customdiscs.*;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.file.Path;
import java.util.Objects;

public class HopperHandler implements Listener {
  private static HopperHandler instance;
  private final CustomDiscs plugin = CustomDiscs.getPlugin();
  private final PlayerManager playerManager = PlayerManager.getInstance();
  private final LavaPlayerManager lavaPlayerManager = LavaPlayerManager.getInstance();

  public static HopperHandler getInstance() {
    if (instance == null) {
      instance = new HopperHandler();
    }
    return instance;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onJukeboxInsertFromHopper(InventoryMoveItemEvent event) {

    if (event.getDestination().getLocation() == null) return;
    if (!event.getDestination().getLocation().getBlock().getType().equals(Material.JUKEBOX)) return;

    boolean isCustomDisc = LegacyUtil.isCustomDisc(event.getItem());
    boolean isYouTubeCustomDisc = LegacyUtil.isCustomYouTubeDisc(event.getItem());

    Block block = event.getDestination().getLocation().getBlock();

    ItemMeta discMeta = LegacyUtil.getItemMeta(event.getItem());

    if (isCustomDisc && !LegacyUtil.isJukeboxContainsDisc(block)) {
      plugin.discsPlayed++;

      String soundFileName = discMeta.getPersistentDataContainer()
          .get(Keys.CUSTOM_DISC.getKey(), Keys.CUSTOM_DISC.getDataType());

      Path soundFilePath = Path.of(plugin.getDataFolder().getPath(), "musicdata", soundFileName);

      if (soundFilePath.toFile().exists()) {
        String songName = Objects.requireNonNull(discMeta.getLore()).get(0);
        songName = songName.replace("§7", "<gray>");

        Component customActionBarSongPlaying = plugin.getLanguage().component("now-playing", songName);

        playerManager.playLocationalAudio(soundFilePath, block, customActionBarSongPlaying);
      }
    }

    if (isYouTubeCustomDisc && !LegacyUtil.isJukeboxContainsDisc(block)) {
      plugin.discsPlayed++;

      String soundLink = discMeta.getPersistentDataContainer()
          .get(Keys.YOUTUBE_DISC.getKey(), Keys.YOUTUBE_DISC.getDataType());

      String songName = Objects.requireNonNull(discMeta.getLore()).get(0);
      songName = songName.replace("§7", "<gray>");

      Component customActionBarSongPlaying = plugin.getLanguage().component("now-playing", songName);

      lavaPlayerManager.playLocationalAudioYoutube(block, VoicePlugin.voicechatApi, soundLink, customActionBarSongPlaying);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onJukeboxEjectToHopper(InventoryMoveItemEvent event) {
    if (event.getSource().getLocation() == null) return;
    Block block = event.getSource().getLocation().getBlock();
    if (!block.getType().equals(Material.JUKEBOX)) return;
    if (!event.getItem().hasItemMeta()) return;
    if (!LegacyUtil.isCustomDisc(event.getItem()) && !LegacyUtil.isCustomYouTubeDisc(event.getItem())) return;

    // CustomDiscs.debug("Hopper try move dicsc");

    event.setCancelled(playerManager.isAudioPlayerPlaying(block.getLocation()) ||
        lavaPlayerManager.isAudioPlayerPlaying(block.getLocation()));
  }

  public void discToHopper(Block block) {
    if (!plugin.getCDConfig().isAllowHoppers()) return;
    if (!plugin.isEnabled()) return;
    if (!block.getLocation().getChunk().isLoaded()) return;

    plugin.getFoliaLib().getScheduler().runAtLocation(block.getLocation(), task -> {
      if (!block.getType().equals(Material.JUKEBOX)) return;

      CustomDiscs.debug("Disc to hopper send");

      Jukebox jukebox = (Jukebox) block.getState();

      block.setType(Material.JUKEBOX);
      jukebox.update(true, true);
      jukebox.stopPlaying();
    });
  }
}
