package io.github.subkek.customdiscs.particle;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.LavaPlayerManager;
import io.github.subkek.customdiscs.PlayerManager;
import io.github.subkek.customdiscs.util.LegacyUtil;
import lombok.Data;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;

import java.util.*;

public class ParticleManager {
  private static ParticleManager instance;
  private final CustomDiscs plugin = CustomDiscs.getPlugin();
  private final Map<UUID, ParticleJukebox> locationParticleManager = new HashMap<>();

  public static ParticleManager getInstance() {
    if (instance == null) return instance = new ParticleManager();
    return instance;
  }

  public ParticleJukebox get(Block block) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    ParticleJukebox particleJukebox = locationParticleManager.get(uuid);
    if (particleJukebox == null)
      throw new IllegalStateException("This ParticleJukebox doesn't exists cannot get");
    return particleJukebox;
  }

  public record NeedUpdate(boolean returnForced, boolean value) {
  }

  public NeedUpdate isNeedUpdate(Block block) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    ParticleJukebox particleJukebox = locationParticleManager.get(uuid);
    if (particleJukebox == null) {
      CustomDiscs.debug("ParticleManager return value false because ParticleJukebox is null");
      return new NeedUpdate(true, false);
    }
    return new NeedUpdate(false, particleJukebox.isNeedUpdate());
  }

  public void setNeedUpdate(Block block, boolean value) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    ParticleJukebox particleJukebox = locationParticleManager.get(uuid);

    if (particleJukebox == null)
      throw new IllegalStateException("This ParticleJukebox doesn't exists cannot set NeedUpdate value");

    particleJukebox.setNeedUpdate(value);
  }

  public void stop(Block block) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    if (locationParticleManager.containsKey(uuid)) {
      ParticleJukebox particleJukebox = locationParticleManager.remove(uuid);
      particleJukebox.task.cancel();

      Jukebox jukebox = (Jukebox) block.getState();
      jukebox.update();
      jukebox.stopPlaying();
    }
  }

  public void start(Jukebox jukebox) {
    UUID uuid = UUID.nameUUIDFromBytes(jukebox.getLocation().toString().getBytes());
    if (locationParticleManager.containsKey(uuid)) return;
    ParticleJukebox particleJukebox = new ParticleJukebox();
    locationParticleManager.put(uuid, particleJukebox);

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
