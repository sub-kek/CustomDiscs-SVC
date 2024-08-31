package io.github.subkek.customdiscs;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import lombok.Data;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Jukebox;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PhysicsManager {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();
  private final Map<UUID, ParticleJukebox> jukeboxMap = new HashMap<>();

  private static PhysicsManager instance;
  public static PhysicsManager getInstance() {
    if (instance == null) return instance = new PhysicsManager();
    return instance;
  }

  public ParticleJukebox get(Block block) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    ParticleJukebox particleJukebox = jukeboxMap.get(uuid);
    if (particleJukebox == null)
      throw new IllegalStateException("This ParticleJukebox doesn't exists cannot get");
    return particleJukebox;
  }

  public record NeedUpdate(boolean returnForced, boolean value) {
  }

  public NeedUpdate isNeedUpdate(Block block) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    ParticleJukebox particleJukebox = jukeboxMap.get(uuid);
    if (particleJukebox == null) {
      CustomDiscs.debug("ParticleManager return value false because ParticleJukebox is null");
      return new NeedUpdate(true, false);
    }
    return new NeedUpdate(false, particleJukebox.isNeedUpdate());
  }

  private void discToHopper(Block block) {
    if (!plugin.getCDConfig().isAllowHoppers()) return;
    if (!plugin.isEnabled()) return;
    if (!block.getLocation().getChunk().isLoaded()) return;
    if (!block.getType().equals(Material.JUKEBOX)) return;

    Block possibleHopper = block.getRelative(BlockFace.DOWN);
    if (!possibleHopper.getType().equals(Material.HOPPER)) return;

    CustomDiscs.debug("Attempting to send a disk to the hopper using a hopper update.");
  }

  private void stop(Block block) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    if (jukeboxMap.containsKey(uuid)) {
      ParticleJukebox particleJukebox = jukeboxMap.remove(uuid);
      particleJukebox.task.cancel();

      Jukebox jukebox = (Jukebox) block.getState();
      jukebox.stopPlaying();
      discToHopper(block);
    }
  }

  public void start(Jukebox jukebox) {
    UUID uuid = UUID.nameUUIDFromBytes(jukebox.getLocation().toString().getBytes());
    if (jukeboxMap.containsKey(uuid)) return;
    ParticleJukebox particleJukebox = new ParticleJukebox();
    jukeboxMap.put(uuid, particleJukebox);

    plugin.getFoliaLib().getScheduler().runAtLocationTimer(jukebox.getLocation(), task -> {
      particleJukebox.setTask(task);
      if (!LavaPlayerManager.getInstance().isPlaying(jukebox.getBlock()) &&
          !PlayerManager.getInstance().isPlaying(jukebox.getBlock())) {

        stop(jukebox.getBlock());
        return;
      }

      if (!jukebox.isPlaying()) {
        jukebox.update();
      }

      particleJukebox.lastUpdateTick = jukebox.getWorld().getTime() - 1; // Может быть... есть вариант получше? Я серьезно!
    }, 1, 20);
  }

  @Data
  public static final class ParticleJukebox {
    private boolean needUpdate = true;
    private boolean updatedFirst = false;
    private WrappedTask task;
    private long lastUpdateTick = 0;
  }
}
