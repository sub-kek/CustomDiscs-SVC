package io.github.subkek.customdiscs;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.tcoded.folialib.FoliaLib;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import io.github.subkek.customdiscs.command.CustomDiscsCommand;
import io.github.subkek.customdiscs.command.CustomDiscsTabCompleter;
import io.github.subkek.customdiscs.config.CustomDiscsConfiguration;
import io.github.subkek.customdiscs.event.JukeboxHandler;
import io.github.subkek.customdiscs.language.YamlLanguage;
import io.github.subkek.customdiscs.metrics.BStatsLink;
import io.github.subkek.customdiscs.particle.ParticleManager;
import lombok.Getter;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Jukebox;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class CustomDiscs extends JavaPlugin {
  public static final String PLUGIN_ID = "CustomDiscs";
  @Getter private static CustomDiscs instance = null;
  private VoicePlugin voicechatPlugin;
  public YamlLanguage language = null;
  private ParticleManager particleManager;
  @Getter private FoliaLib foliaLib = new FoliaLib(this);

  @Override
  public void onEnable() {
    CustomDiscs.instance = this;

    if (getDataFolder().mkdir()) getLogger().info("Created plugin data folder");

    CustomDiscsConfiguration.load();

    language = new YamlLanguage();
    language.init();

    linkBStats();

    BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);

    File musicData = new File(this.getDataFolder(), "musicdata");
    if (!(musicData.exists())) {
      if (musicData.mkdir()) getLogger().info("Created music data folder");
    }

    if (service != null) {
      voicechatPlugin = new VoicePlugin();
      service.registerPlugin(voicechatPlugin);
      getLogger().info("Successfully enabled CustomDiscs plugin");
    } else {
      getLogger().severe("Failed to enable CustomDiscs plugin");
    }

    getServer().getPluginManager().registerEvents(new JukeboxHandler(), this);
    CustomDiscsCommand customDiscsCommand = new CustomDiscsCommand();
    getCommand("customdisc").setExecutor(customDiscsCommand);
    getCommand("customdisc").setTabCompleter(new CustomDiscsTabCompleter(customDiscsCommand));


    ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    particleManager = new ParticleManager();
    protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.WORLD_EVENT) {
      @Override
      public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();

        if (packet.getIntegers().read(0).toString().equals("1010")) {
          Jukebox jukebox = (Jukebox) packet.getBlockPositionModifier().read(0).toLocation(event.getPlayer().getWorld()).getBlock().getState();

          if (!jukebox.getRecord().hasItemMeta()) return;

          if (jukebox.getRecord().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(CustomDiscs.getInstance(), "customdisc"), PersistentDataType.STRING) ||
              jukebox.getRecord().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(CustomDiscs.getInstance(), "customdiscyt"), PersistentDataType.STRING)) {
            event.setCancelled(true);
            particleManager.start(jukebox);
          }
        }
      }
    });
  }

  @Override
  public void onDisable() {
    LavaPlayerManager.getInstance().stopPlayingAll();
    PlayerManager.instance().stopAll();

    if (voicechatPlugin != null) {
      getServer().getServicesManager().unregister(voicechatPlugin);
      getLogger().info("Successfully disabled CustomDiscs plugin");
    }

    foliaLib.getImpl().cancelAllTasks();
  }

  private void linkBStats() {
    BStatsLink bstats = new BStatsLink(getInstance(), 20077);

    bstats.addCustomChart(new BStatsLink.SimplePie("plugin_language", () -> CustomDiscsConfiguration.locale));
    bstats.addCustomChart(new BStatsLink.SingleLineChart("discs_played", () -> {
      int value = CustomDiscsConfiguration.discsPlayed;
      CustomDiscsConfiguration.discsPlayed = 0;
      return value;
    }));
  }
}
