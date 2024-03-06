package io.github.subkek.customdiscs;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Jukebox;

import java.util.HashSet;
import java.util.Set;

public class ParticleManager {
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  private final PlayerManager playerManager = PlayerManager.instance();
  private final Set<Location> locationParticleManager = new HashSet<>();

  public void start(Jukebox jukebox) {
    if (locationParticleManager.contains(jukebox.getLocation())) return;
    plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, jukebox.getLocation(), t -> {
      if (!YouTubePlayerManager.isAudioPlayerPlaying(jukebox.getLocation()) && !playerManager.isAudioPlayerPlaying(jukebox.getLocation())) {
        locationParticleManager.remove(jukebox.getLocation());
        t.cancel();
      } else {
        if (!jukebox.isPlaying()) {
          jukebox.getLocation().getWorld().spawnParticle(Particle.NOTE, jukebox.getLocation().add(0.5, 1.1, 0.5), 1);
        }
      }
    }, 1, 20);
  }
}