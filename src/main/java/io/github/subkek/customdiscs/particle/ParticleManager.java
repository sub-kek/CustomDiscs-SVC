package io.github.subkek.customdiscs.particle;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.LavaPlayerManager;
import io.github.subkek.customdiscs.PlayerManager;
import lombok.Data;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ParticleManager {
  private static ParticleManager instance;
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  private final Map<UUID, ParticleJukebox> locationParticleManager = new HashMap<>();

  public static ParticleManager getInstance() {
    if (instance == null) return instance = new ParticleManager();
    return instance;
  }

  public boolean isNeedUpdate(Block block) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    ParticleJukebox particleJukebox = locationParticleManager.get(uuid);
    if (particleJukebox == null) return false;
    return particleJukebox.isNeedUpdate();
  }

  public void stop(Block block) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    ParticleJukebox particleJukebox = locationParticleManager.get(uuid);
    particleJukebox.setNeedUpdate(true);
    particleJukebox.task.cancel();
    particleJukebox.setCancelled(true);
    locationParticleManager.remove(uuid);
  }

  public void start(Jukebox jukebox) {
    UUID uuid = UUID.nameUUIDFromBytes(jukebox.getLocation().toString().getBytes());
    if (locationParticleManager.containsKey(uuid)) return;
    ParticleJukebox particleJukebox = new ParticleJukebox();
    locationParticleManager.put(uuid, particleJukebox);

    plugin.getFoliaLib().getScheduler().runAtLocationTimer(jukebox.getLocation(), task -> {
      particleJukebox.setTask(task);
      if (!LavaPlayerManager.getInstance().isAudioPlayerPlaying(jukebox.getLocation()) &&
          !PlayerManager.getInstance().isAudioPlayerPlaying(jukebox.getLocation())) {

        stop(jukebox.getBlock());
        return;

      } else if (!jukebox.isPlaying()) {
        jukebox.getWorld().spawnParticle(Particle.NOTE, jukebox.getLocation().add(0.5, 1.1, 0.5), 1);
      }

      jukebox.stopPlaying();
      jukebox.update(false, false);
      particleJukebox.setNeedUpdate(false);
    }, 1, 20);
  }

  @Data
  private static final class ParticleJukebox {
    private boolean needUpdate = true;
    private WrappedTask task;
    private boolean cancelled;
  }
}
