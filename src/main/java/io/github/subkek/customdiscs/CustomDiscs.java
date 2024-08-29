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
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPIBukkitConfig;
import io.github.subkek.customdiscs.command.CustomDiscsCommand;
import io.github.subkek.customdiscs.config.CDConfig;
import io.github.subkek.customdiscs.event.HopperHandler;
import io.github.subkek.customdiscs.event.JukeboxHandler;
import io.github.subkek.customdiscs.language.YamlLanguage;
import io.github.subkek.customdiscs.metrics.BStatsLink;
import io.github.subkek.customdiscs.particle.ParticleManager;
import io.github.subkek.customdiscs.util.Formatter;
import lombok.Getter;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.bukkit.block.Jukebox;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class CustomDiscs extends JavaPlugin {
  public static final String PLUGIN_ID = "CustomDiscs";
  private VoicePlugin voicechatPlugin;
  @Getter
  private YamlLanguage language = new YamlLanguage();
  @Getter
  private CDConfig cDConfig = new CDConfig(
      new File(getDataFolder().getPath(), "config.yml"));
  @Getter
  private FoliaLib foliaLib = new FoliaLib(this);
  @Getter
  private BukkitAudiences audience;
  public int discsPlayed = 0;

  public static CustomDiscs getPlugin() {
    return getPlugin(CustomDiscs.class);
  }

  @Override
  public void onLoad() {
    CommandAPI.onLoad(new CommandAPIBukkitConfig(this).verboseOutput(true));
  }

  @Override
  public void onEnable() {
    audience = BukkitAudiences.create(this);

    if (getDataFolder().mkdir()) info("Created plugin data folder");

    cDConfig.init();
    language.init();

    linkBStats();

    File musicData = new File(this.getDataFolder(), "musicdata");
    if (!(musicData.exists())) {
      if (musicData.mkdir()) info("Created music data folder");
    }

    registerVoicechatHook();

    registerEvents();
    registerCommands();

    ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
    protocolManager.addPacketListener(new PacketAdapter(this, ListenerPriority.NORMAL, PacketType.Play.Server.WORLD_EVENT) {
      @Override
      public void onPacketSending(PacketEvent event) {
        PacketContainer packet = event.getPacket();

        if (packet.getIntegers().read(0).equals(1010)) {
          Jukebox jukebox = (Jukebox) packet.getBlockPositionModifier().read(0).toLocation(event.getPlayer().getWorld()).getBlock().getState();

          if (!jukebox.getRecord().hasItemMeta()) return;

          if (LegacyUtil.isCustomDisc(jukebox.getRecord()) ||
              LegacyUtil.isCustomYouTubeDisc(jukebox.getRecord())) {
            event.setCancelled(true);
            ParticleManager.getInstance().start(jukebox);
          }
        }
      }
    });
  }

  private void registerVoicechatHook() {
    BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);

    if (service != null) {
      voicechatPlugin = new VoicePlugin();
      service.registerPlugin(voicechatPlugin);
      getLogger().info("Successfully enabled voicechat hook");
    } else {
      getLogger().severe("Failed to enable voicechat hook");
    }
  }

  private void registerCommands() {
    new CustomDiscsCommand().register("customdiscs");
  }

  private void registerEvents() {
    getServer().getPluginManager().registerEvents(new JukeboxHandler(), this);
    getServer().getPluginManager().registerEvents(HopperHandler.getInstance(), this);
  }

  @Override
  public void onDisable() {
    LavaPlayerManager.getInstance().stopPlayingAll();
    LavaPlayerManager.getInstance().save();

    PlayerManager.getInstance().stopAll();

    if (voicechatPlugin != null) {
      getServer().getServicesManager().unregister(voicechatPlugin);
      getLogger().info("Successfully disabled CustomDiscs plugin");
    }

    foliaLib.getScheduler().cancelAllTasks();
  }

  private void linkBStats() {
    BStatsLink bstats = new BStatsLink(getPlugin(), 20077);

    bstats.addCustomChart(new BStatsLink.SimplePie("plugin_language", () -> getCDConfig().getLocale()));
    bstats.addCustomChart(new BStatsLink.SingleLineChart("discs_played", () -> {
      int value = discsPlayed;
      discsPlayed = 0;
      return value;
    }));
  }

  public static void sendMessage(CommandSender sender, Component component) {
    getPlugin().getAudience().sender(sender).sendMessage(component);
  }

  public static void debug(@NotNull String message, Object... format) {
    if (!getPlugin().getCDConfig().isDebug()) return;
    sendMessage(
        getPlugin().getServer().getConsoleSender(),
        getPlugin().getLanguage().deserialize(
            Formatter.format(
                "{0}{1}",
                getPlugin().getLanguage().string("prefix.debug"),
                Formatter.format(message, format)
            )
        )
    );
  }

  public static void info(@NotNull String message, Object... format) {
    sendMessage(
        getPlugin().getServer().getConsoleSender(),
        getPlugin().getLanguage().deserialize(
            Formatter.format(
                "{0}{1}",
                getPlugin().getLanguage().string("prefix.info"),
                Formatter.format(message, format)
            )
        )
    );
  }

  public static void error(@NotNull String message, @Nullable Throwable e, Object... format) {
    String stackTrace = "";

    if (e != null) {
      stackTrace = ExceptionUtils.getStackTrace(e);
    }

    sendMessage(
        getPlugin().getServer().getConsoleSender(),
        getPlugin().getLanguage().deserialize(
            Formatter.format(
                "{0}{1}{2}",
                getPlugin().getLanguage().string("prefix.error"),
                Formatter.format(message, format),
                stackTrace
            )
        )
    );
  }

  public static void error(@NotNull String message, Object... format) {
    error(message, null, format);
  }
}
