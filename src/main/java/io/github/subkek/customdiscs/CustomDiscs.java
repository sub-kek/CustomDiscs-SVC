package io.github.subkek.customdiscs;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import io.github.subkek.customdiscs.command.CustomDiscsCommand;
import io.github.subkek.customdiscs.config.CustomDiscsConfig;
import io.github.subkek.customdiscs.event.ClearDiscs;
import io.github.subkek.customdiscs.event.JukeBox;
import io.github.subkek.customdiscs.language.FileLanguage;
import io.github.subkek.customdiscs.libs.AssetsDownloader;
import io.github.subkek.customdiscs.metrics.BStatsLink;
import io.github.subkek.customdiscs.particle.BukkitParticleManager;
import io.github.subkek.customdiscs.particle.FoliaParticleManager;
import io.github.subkek.customdiscs.particle.ParticleManager;
import lombok.Getter;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.io.File;
import java.util.Objects;

public final class CustomDiscs extends JavaPlugin {
  public String defaultResourceHash = "N/A";
  public static final String PLUGIN_ID = "CustomDiscs";
  public static Logger LOGGER;
  @Getter private static CustomDiscs instance = null;
  private VoicePlugin voicechatPlugin;
  public CustomDiscsConfig config = null;
  public FileLanguage language = null;
  private ParticleManager particleManager;

  @Override
  public void onEnable() {
    CustomDiscs.instance = this;

    LOGGER = getSLF4JLogger();

    if (getDataFolder().mkdir()) getSLF4JLogger().info("Created plugin data folder");

    AssetsDownloader.loadLibraries(getDataFolder());

    config = new CustomDiscsConfig();
    config.saveDefaultConfig();
    config.init();

    language = new FileLanguage();
    language.init(config.getLocale());

    linkBStats();

    BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);

    File musicData = new File(this.getDataFolder(), "musicdata");
    if (!(musicData.exists())) {
      if (musicData.mkdir()) getSLF4JLogger().info("Created music data folder");
    }

    if (service != null) {
      voicechatPlugin = new VoicePlugin();
      service.registerPlugin(voicechatPlugin);
      LOGGER.info("Successfully enabled CustomDiscs plugin");
    } else {
      LOGGER.error("Failed to enable CustomDiscs plugin");
    }

    getServer().getPluginManager().registerEvents(new JukeBox(), this);
    getServer().getPluginManager().registerEvents(new ClearDiscs(), this);
    Objects.requireNonNull(getCommand("customdisc")).setExecutor(new CustomDiscsCommand());

    ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    particleManager = isFolia() ? new FoliaParticleManager() : new BukkitParticleManager();

    protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.WORLD_EVENT) {
      @Override
      public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();

        if (packet.getIntegers().read(0).toString().equals("1010")) {
          World world = event.getPlayer().getWorld();
          BlockPosition blockPosition = packet.getBlockPositionModifier().read(0);
          Location loc = new Location(world, blockPosition.getX(), blockPosition.getY(), blockPosition.getZ());
          Block block = world.getBlockAt(loc);
          Jukebox jukebox;
          if (block.getType().equals(Material.JUKEBOX)) {
            jukebox = (Jukebox) block.getState();
          } else {
            return;
          }

          if (!jukebox.getRecord().hasItemMeta()) return;

          if (jukebox.getRecord().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(CustomDiscs.getInstance(), "customdisc"), PersistentDataType.STRING)) {
            event.setCancelled(true);
            jukebox.stopPlaying();
            particleManager.start(jukebox);
          }
          if (jukebox.getRecord().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(CustomDiscs.getInstance(), "customdiscyt"), PersistentDataType.STRING)) {
            event.setCancelled(true);
            jukebox.stopPlaying();
            particleManager.start(jukebox);
          }
        }
      }
    });
  }

  @Override
  public void onDisable() {
    if (voicechatPlugin != null) {
      getServer().getServicesManager().unregister(voicechatPlugin);
      LOGGER.info("Successfully disabled CustomDiscs plugin");
    }
  }

  private void linkBStats() {

    BStatsLink bstats = new BStatsLink(getInstance(), 20077);

    bstats.addCustomChart(new BStatsLink.SimplePie("plugin_language", () -> config.getLocale()));
    bstats.addCustomChart(new BStatsLink.SingleLineChart("discs_played", () -> {
      int value = config.getDiscsPlayed();
      config.setDiscsPlayed(0);
      return value;
    }));
  }

  public boolean isFolia() {
    try {
      Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
      return true;
    } catch (ClassNotFoundException e) {
      return false;
    }
  }
}
