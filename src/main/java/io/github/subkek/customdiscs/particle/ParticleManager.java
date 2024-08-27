package io.github.subkek.customdiscs.particle;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.LavaPlayerManager;
import io.github.subkek.customdiscs.PlayerManager;
import lombok.Data;
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
    if (particleJukebox == null) {
      CustomDiscs.debug("particleJukebox returning needUpdate false");
      return false;
    }
    return particleJukebox.isNeedUpdate();
  }

  public void stop(Block block) {
    UUID uuid = UUID.nameUUIDFromBytes(block.getLocation().toString().getBytes());
    if (locationParticleManager.containsKey(uuid)) {
      ParticleJukebox particleJukebox = locationParticleManager.remove(uuid);
      particleJukebox.setNeedUpdate(true);
      particleJukebox.task.cancel();
      particleJukebox.setCancelled(true);
    }
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
      }

      if (!particleJukebox.isUpdatedFirst()) {
        particleJukebox.setUpdatedFirst(true);
        particleJukebox.setNeedUpdate(true);
      }

      jukebox.stopPlaying();
      jukebox.update(true, false);
      particleJukebox.setNeedUpdate(false);
    }, 1, 20);
  }

  @Data
  private static final class ParticleJukebox {
    private boolean needUpdate = false;
    private boolean updatedFirst = false;
    private WrappedTask task;
    private boolean cancelled;
  }
}
