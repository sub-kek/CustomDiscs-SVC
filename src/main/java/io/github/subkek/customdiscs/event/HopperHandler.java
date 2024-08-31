package io.github.subkek.customdiscs.event;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.LavaPlayerManager;
import io.github.subkek.customdiscs.PlayerManager;
import io.github.subkek.customdiscs.particle.ParticleManager;
import io.github.subkek.customdiscs.util.LegacyUtil;
import io.github.subkek.customdiscs.util.PlayUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;

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
    Block block = event.getDestination().getLocation().getBlock();
    if (!block.getType().equals(Material.JUKEBOX)) return;
    if (LegacyUtil.isJukeboxContainsDisc(block)) return;

    boolean isCustomDisc = LegacyUtil.isCustomDisc(event.getItem());
    boolean isYouTubeCustomDisc = LegacyUtil.isCustomYouTubeDisc(event.getItem());

    if (!isCustomDisc && !isYouTubeCustomDisc) return;

    CustomDiscs.debug("Jukebox insert by hopper/dropper event");

    if (isCustomDisc)
      PlayUtil.playStandard(block, event.getItem());

    if (isYouTubeCustomDisc)
      PlayUtil. playLava(block, event.getItem());
  }

  @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
  public void onJukeboxEjectToHopper(InventoryMoveItemEvent event) {
    if (event.getSource().getLocation() == null) return;
    Block block = event.getSource().getLocation().getBlock();
    if (!block.getType().equals(Material.JUKEBOX)) return;
    if (!event.getItem().hasItemMeta()) return;
    if (!LegacyUtil.isCustomDisc(event.getItem()) && !LegacyUtil.isCustomYouTubeDisc(event.getItem())) return;

    event.setCancelled(playerManager.isPlaying(block) ||
        lavaPlayerManager.isPlaying(block));

    if (!event.isCancelled()) CustomDiscs.debug("Jukebox eject by hopper event");
  }

  public void discToHopper(Block block) {
    if (!plugin.getCDConfig().isAllowHoppers()) return;
    if (!plugin.isEnabled()) return;
    if (!block.getLocation().getChunk().isLoaded()) return;

    plugin.getFoliaLib().getScheduler().runAtLocation(block.getLocation(), task -> {
      if (!block.getType().equals(Material.JUKEBOX)) return;

      Block possibleHopper = block.getRelative(BlockFace.DOWN);
      if (!possibleHopper.getType().equals(Material.HOPPER)) return;

      CustomDiscs.debug("Attempting to send a disk to the hopper using a hopper update.");
    });
  }

  @EventHandler
  public void onPhysics(BlockPhysicsEvent event) {
    if (!event.getSourceBlock().getType().equals(Material.JUKEBOX)) return;
    Block block = event.getSourceBlock();

    if (!LavaPlayerManager.getInstance().isPlaying(block)
        && !PlayerManager.getInstance().isPlaying(block)) return;

    ParticleManager.NeedUpdate needUpdate = ParticleManager.getInstance().isNeedUpdate(block);

    boolean reallyNeed = false;
    ParticleManager.ParticleJukebox particleJukebox = null;
    long time = block.getWorld().getTime();
    if (!needUpdate.returnForced()) {
      particleJukebox = ParticleManager.getInstance().get(block);

      reallyNeed = particleJukebox.getLastUpdateTick() == time;
    }

    if (needUpdate.value() || reallyNeed) {
      ParticleManager.getInstance().setNeedUpdate(block, false);
      assert particleJukebox != null;
      particleJukebox.setLastUpdateTick(time);
      CustomDiscs.debug("Updating physics on {0} by jukebox", event.getBlock().getType());
      return;
    }

    event.setCancelled(true);
  }
}
