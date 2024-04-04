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
    jukebox.stopPlaying();
    if (plugin.getFoliaLib().isFolia()) {plugin.getFoliaLib().getImpl().runAtLocation(jukebox.getLocation(), task -> jukebox.stopPlaying());}
    else {jukebox.stopPlaying();}

    plugin.getFoliaLib().getImpl().runAtLocationTimer(jukebox.getLocation(), task -> {
      if (!LavaPlayerManager.getInstance().isAudioPlayerPlaying(jukebox.getLocation()) && !PlayerManager.instance().isAudioPlayerPlaying(jukebox.getLocation())) {
        locationParticleManager.remove(uuid);
        task.cancel();
      } else {
        jukebox.getLocation().getWorld().spawnParticle(Particle.NOTE, jukebox.getLocation().add(0.5, 1.1, 0.5), 1);
      }
    }, 1, 20);
  }
}
