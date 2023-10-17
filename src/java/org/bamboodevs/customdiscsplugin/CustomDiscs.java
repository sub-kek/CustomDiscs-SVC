package org.bamboodevs.customdiscsplugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bamboodevs.customdiscsplugin.command.CommandManager;
import org.bamboodevs.customdiscsplugin.config.CustomDiscsConfig;
import org.bamboodevs.customdiscsplugin.event.ClearDiscs;
import org.bamboodevs.customdiscsplugin.event.JukeBox;
import org.bamboodevs.customdiscsplugin.language.FileLanguage;
import org.bamboodevs.customdiscsplugin.libs.AssetsDownloader;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;


public final class CustomDiscs extends JavaPlugin {
    public String defaultResourceHash = "N/A";
    public static final String PLUGIN_ID = "CustomDiscs";
    public static final Logger LOGGER = LogManager.getLogger(PLUGIN_ID);
    private static CustomDiscs instance = null;
    private VoicePlugin voicechatPlugin;
    public CustomDiscsConfig config = null;
    public FileLanguage language = null;

    @Override
    public void onEnable() {
        CustomDiscs.instance = this;

        AssetsDownloader.loadLibraries(getDataFolder());

        config = new CustomDiscsConfig();
        config.init();

        language = new FileLanguage();
        language.init(config.getLocale());

        BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);

        File musicData = new File(this.getDataFolder(), "musicdata");
        if (!(musicData.exists())) {
            musicData.mkdirs();
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
        getCommand("customdisc").setExecutor(new CommandManager());

        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

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
                    }
                    if (jukebox.getRecord().getItemMeta().getPersistentDataContainer().has(new NamespacedKey(CustomDiscs.getInstance(), "customdiscyt"), PersistentDataType.STRING)) {
                        event.setCancelled(true);
                    }

                    //Spawn particles if there isn't any music playing at this location.
                    ParticleManager.start(jukebox);
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

    public static CustomDiscs getInstance() {
        return instance;
    }
}
