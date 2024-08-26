package io.github.subkek.customdiscs.particle;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.LavaPlayerManager;
import io.github.subkek.customdiscs.PlayerManager;
import org.bukkit.Particle;
import org.bukkit.block.Jukebox;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ParticleManager {
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  private final Set<UUID> locationParticleManager = new HashSet<>();

  public void start(Jukebox jukebox) {
    UUID uuid = UUID.nameUUIDFromBytes(jukebox.getLocation().toString().getBytes());
    if (locationParticleManager.contains(uuid)) return;
    locationParticleManager.add(uuid);

    plugin.getFoliaLib().getScheduler().runAtLocationTimer(jukebox.getLocation(), task -> {
      if (!LavaPlayerManager.getInstance().isAudioPlayerPlaying(jukebox.getLocation()) &&
          !PlayerManager.getInstance().isAudioPlayerPlaying(jukebox.getLocation())) {
        locationParticleManager.remove(uuid);

        jukebox.stopPlaying();

        task.cancel();
        return;

      } else if (!jukebox.isPlaying()) {
        jukebox.getLocation().getWorld().spawnParticle(Particle.NOTE, jukebox.getLocation().add(0.5, 1.1, 0.5), 1);
      }

      jukebox.stopPlaying();
      jukebox.update();
    }, 1, 20);
  }
}
