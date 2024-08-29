package io.github.subkek.customdiscs.event;

import io.github.subkek.customdiscs.*;
import io.github.subkek.customdiscs.particle.ParticleManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.file.Path;
import java.util.Objects;

public class JukeboxHandler implements Listener {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  private static ItemStack getItemStack(PlayerInteractEvent event, Player player) {
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
    return itemInvolvedInEvent;
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onInsert(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    Block block = event.getClickedBlock();

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (event.getClickedBlock() == null) return;
    if (event.getItem() == null) return;
    if (!event.getItem().hasItemMeta()) return;
    if (block == null) return;
    if (!block.getType().equals(Material.JUKEBOX)) return;

    boolean isCustomDisc = LegacyUtil.isCustomDisc(event.getItem());
    boolean isYouTubeCustomDisc = LegacyUtil.isCustomYouTubeDisc(event.getItem());

    if (!isCustomDisc && !isYouTubeCustomDisc) return;

    CustomDiscs.debug("On insert");

    ItemMeta discMeta = LegacyUtil.getItemMeta(event.getItem());

    if (isCustomDisc && !LegacyUtil.isJukeboxContainsDisc(block)) {
      if (!player.hasPermission("customdiscs.play")) {
        CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("play-no-permission-error"));
        return;
      }

      plugin.discsPlayed++;

      String soundFileName = discMeta.getPersistentDataContainer()
          .get(Keys.CUSTOM_DISC.getKey(), Keys.CUSTOM_DISC.getDataType());

      Path soundFilePath = Path.of(plugin.getDataFolder().getPath(), "musicdata", soundFileName);

      if (soundFilePath.toFile().exists()) {
        String songName = Objects.requireNonNull(discMeta.getLore()).get(0);
        songName = songName.replace("§7", "<gray>");

        Component customActionBarSongPlaying = plugin.getLanguage().component("now-playing", songName);

        PlayerManager.getInstance().playLocationalAudio(soundFilePath, block, customActionBarSongPlaying);
      } else {
        CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("file-not-found"));
      }
    }

    if (isYouTubeCustomDisc && !LegacyUtil.isJukeboxContainsDisc(block)) {
      if (!player.hasPermission("customdiscs.playt")) {
        CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("play-no-permission-error"));
        return;
      }

      plugin.discsPlayed++;

      String soundLink = discMeta.getPersistentDataContainer().get(Keys.YOUTUBE_DISC.getKey(), Keys.YOUTUBE_DISC.getDataType());

      String songName = Objects.requireNonNull(discMeta.getLore()).get(0);
      songName = songName.replace("§7", "<gray>");

      Component customActionBarSongPlaying = plugin.getLanguage().component("now-playing", songName);

      LavaPlayerManager.getInstance().playLocationalAudioYoutube(block, VoicePlugin.voicechatApi, soundLink, customActionBarSongPlaying);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onEject(PlayerInteractEvent event) {
    Player player = event.getPlayer();
    Block block = event.getClickedBlock();

    if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
    if (block == null) return;
    if (!block.getType().equals(Material.JUKEBOX)) return;
    if (!LegacyUtil.isJukeboxContainsDisc(block)) return;
    ItemStack itemInvolvedInEvent = getItemStack(event, player);
    if (player.isSneaking() && !itemInvolvedInEvent.getType().equals(Material.AIR)) return;
    Jukebox jukebox = (Jukebox) block.getState();
    if (!LegacyUtil.isCustomDisc(jukebox.getRecord()) &&
        !LegacyUtil.isCustomYouTubeDisc(jukebox.getRecord())) return;

    CustomDiscs.debug("On eject");

    ParticleManager.getInstance().stop(block);

    PlayerManager.getInstance().stopPlaying(block);
    LavaPlayerManager.getInstance().stopPlaying(block, true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onJukeboxBreak(BlockBreakEvent event) {

    Block block = event.getBlock();

    if (block.getType() != Material.JUKEBOX) return;

    PlayerManager.getInstance().stopPlaying(block);
    LavaPlayerManager.getInstance().stopPlaying(block, true);
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onJukeboxExplode(EntityExplodeEvent event) {
    for (Block explodedBlock : event.blockList()) {
      if (explodedBlock.getType() == Material.JUKEBOX) {
        PlayerManager.getInstance().stopPlaying(explodedBlock);
        LavaPlayerManager.getInstance().stopPlaying(explodedBlock, true);
      }
    }
  }

  @EventHandler
  public void onPhysics(BlockPhysicsEvent event) {
    if (!event.getSourceBlock().getType().equals(Material.JUKEBOX)) return;
    Block block = event.getSourceBlock();

    if (!LavaPlayerManager.getInstance().isAudioPlayerPlaying(block.getLocation())
        && !PlayerManager.getInstance().isAudioPlayerPlaying(block.getLocation())) return;

    if (ParticleManager.getInstance().isNeedUpdate(block)) return;

    event.setCancelled(true);
  }
}
