package io.github.subkek.customdiscs.event;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.LavaPlayerManager;
import io.github.subkek.customdiscs.PlayerManager;
import io.github.subkek.customdiscs.VoicePlugin;
import io.github.subkek.customdiscs.config.CustomDiscsConfiguration;
import io.github.subkek.customdiscs.util.Formatter;
import net.kyori.adventure.text.Component;
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

public class JukeboxHandler implements Listener {
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  PlayerManager playerManager = PlayerManager.instance();

  @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onInsert(PlayerInteractEvent event) throws IOException {
    Player player = event.getPlayer();
    Block block = event.getClickedBlock();

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null || event.getItem() == null || event.getItem().getItemMeta() == null || block == null)
      return;
    if (event.getClickedBlock().getType() != Material.JUKEBOX) return;

    boolean isCustomDisc = isCustomMusicDisc(event.getItem());
    boolean isYouTubeCustomDisc = isCustomMusicDiscYouTube(event.getItem());

    if (isCustomDisc && !jukeboxContainsDisc(block)) {
      if (!player.hasPermission("customdiscs.play")) {
        player.sendMessage(Formatter.format(plugin.language.get("play-no-permission-error"), true));
        return;
      }
      CustomDiscsConfiguration.discsPlayed++;

      String soundFileName = event.getItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "customdisc"), PersistentDataType.STRING);

      Path soundFilePath = Path.of(plugin.getDataFolder().getPath(), "musicdata", soundFileName);

      if (soundFilePath.toFile().exists()) {
        String songName = Objects.requireNonNull(event.getItem().getItemMeta().getLore()).get(0);

        Component customActionBarSongPlaying = Component.text(Formatter.format(plugin.language.get("now-playing"), songName));

        assert VoicePlugin.voicechatServerApi != null;
        playerManager.playLocationalAudio(VoicePlugin.voicechatServerApi, soundFilePath, block, customActionBarSongPlaying);
      } else {
        player.sendMessage(Formatter.format(plugin.language.get("file-not-found"), true));
        event.setCancelled(true);
        throw new FileNotFoundException("File not found!");
      }
    }

    if (isYouTubeCustomDisc && !jukeboxContainsDisc(block)) {
      if (!player.hasPermission("customdiscs.playt")) {
        player.sendMessage(Formatter.format(plugin.language.get("play-no-permission-error"), true));
        return;
      }

      CustomDiscsConfiguration.discsPlayed++;

      String soundLink = event.getItem().getItemMeta().getPersistentDataContainer().get(new NamespacedKey(plugin, "customdiscyt"), PersistentDataType.STRING);

      String songName = Objects.requireNonNull(event.getItem().getItemMeta().getLore()).get(0);

      Component customActionBarSongPlaying = Component.text(Formatter.format(plugin.language.get("now-playing"), songName));

      assert VoicePlugin.voicechatServerApi != null;
      LavaPlayerManager.getInstance().playLocationalAudioYoutube(block, VoicePlugin.voicechatServerApi, soundLink, customActionBarSongPlaying);
    }
  }

  @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
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
        LavaPlayerManager.getInstance().stopPlaying(block);
      }
    }
  }

  @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onJukeboxBreak(BlockBreakEvent event) {

    Block block = event.getBlock();

    if (block.getType() != Material.JUKEBOX) return;

    stopDisc(block);
    LavaPlayerManager.getInstance().stopPlaying(block);
  }

  @EventHandler (priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onJukeboxExplode(EntityExplodeEvent event) {
    for (Block explodedBlock : event.blockList()) {
      if (explodedBlock.getType() == Material.JUKEBOX) {
        stopDisc(explodedBlock);
        LavaPlayerManager.getInstance().stopPlaying(explodedBlock);
      }
    }
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

  private void stopDisc(Block block) {
    playerManager.stopLocationalAudio(block.getLocation());
  }
}
