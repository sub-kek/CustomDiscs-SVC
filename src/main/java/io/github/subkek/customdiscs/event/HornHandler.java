package io.github.subkek.customdiscs.event;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.HornPlayerManager;
import io.github.subkek.customdiscs.Keys;
import io.github.subkek.customdiscs.LavaPlayerManager;
import io.github.subkek.customdiscs.util.LegacyUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.file.Path;
import java.util.Objects;

public class HornHandler implements Listener {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
  public void onHornUse(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    ItemStack item = event.getItem();
    
    // Check if the item is a goat horn
    if (item == null || !item.getType().toString().equals("GOAT_HORN")) {
      return;
    }
    
    // Check if it's a custom horn
    if (!LegacyUtil.isCustomHorn(item) && !LegacyUtil.isCustomYouTubeHorn(item)) {
      return;
    }
    
    // Check if it's a right-click action (either in air or on block)
    if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
      return;
    }
    
    // Cancel the original event to prevent default horn sound
    // doesn't work on the guy who triggered the event because client side
    event.setCancelled(true);
    
    plugin.discsPlayed++;
    
    // Stop any currently playing horn for this player
    HornPlayerManager.getInstance().stopPlaying(player);
    
    // Play the custom horn
    if (LegacyUtil.isCustomHorn(item)) {
      playStandardHorn(player, item);
    } else if (LegacyUtil.isCustomYouTubeHorn(item)) {
      playYouTubeHorn(player, item);
    }
  }

  private void playStandardHorn(Player player, ItemStack horn) {
    ItemMeta hornMeta = LegacyUtil.getItemMeta(horn);

    String soundFileName = hornMeta.getPersistentDataContainer()
        .get(Keys.CUSTOM_HORN.getKey(), Keys.CUSTOM_HORN.getDataType());

    Path soundFilePath = Path.of(plugin.getDataFolder().getPath(), "musicdata", soundFileName);

    if (soundFilePath.toFile().exists()) {
      String songName = Objects.requireNonNull(hornMeta.getLore()).get(0);
      songName = songName.replace("§7", "<gray>");

      Component customActionBarSongPlaying = plugin.getLanguage().component("now-playing", songName);

      HornPlayerManager.getInstance().playHorn(soundFilePath, player, customActionBarSongPlaying);
    } else {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.play.file-not-found"));
    }
  }

  private void playYouTubeHorn(Player player, ItemStack horn) {
    if (!plugin.youtubeSupport) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.play.youtube-not-supported"));
      return;
    }

    ItemMeta hornMeta = LegacyUtil.getItemMeta(horn);

    String soundLink = hornMeta.getPersistentDataContainer()
        .get(Keys.YOUTUBE_HORN.getKey(), Keys.YOUTUBE_HORN.getDataType());

    String songName = Objects.requireNonNull(hornMeta.getLore()).get(0);
    songName = songName.replace("§7", "<gray>");

    Component customActionBarSongPlaying = plugin.getLanguage().component("now-playing", songName);

    // For YouTube horns, we need to use LavaPlayerManager but with a different approach
    // since we need it to follow the player and auto-stop after horn duration
    LavaPlayerManager.getInstance().playHorn(player, soundLink, customActionBarSongPlaying);
  }
} 