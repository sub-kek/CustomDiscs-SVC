package io.github.subkek.customdiscs.particle;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.LavaPlayerManager;
import io.github.subkek.customdiscs.PlayerManager;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Jukebox;

import java.util.HashSet;
import java.util.Set;

public class FoliaParticleManager implements ParticleManager{
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  private final PlayerManager playerManager = PlayerManager.instance();
  private final Set<Location> locationParticleManager = new HashSet<>();

  @Override
  public void start(Jukebox jukebox) {
    if (locationParticleManager.contains(jukebox.getLocation())) return;
    locationParticleManager.add(jukebox.getLocation());
    plugin.getServer().getRegionScheduler().run(plugin, jukebox.getLocation(), t -> jukebox.stopPlaying());
    plugin.getServer().getRegionScheduler().runAtFixedRate(plugin, jukebox.getLocation(), t -> {
      if (!LavaPlayerManager.getInstance().isAudioPlayerPlaying(jukebox.getLocation()) && !playerManager.isAudioPlayerPlaying(jukebox.getLocation())) {
        locationParticleManager.remove(jukebox.getLocation());
        t.cancel();
      } else {
        jukebox.getLocation().getWorld().spawnParticle(Particle.NOTE, jukebox.getLocation().add(0.5, 1.1, 0.5), 1);
      }
    }, 1, 20);
  }
}
