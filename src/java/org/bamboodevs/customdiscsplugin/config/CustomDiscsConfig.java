package org.bamboodevs.customdiscsplugin.config;

import org.bamboodevs.customdiscsplugin.CustomDiscs;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class CustomDiscsConfig {
    private final CustomDiscs plugin = CustomDiscs.getInstance();
    private FileConfiguration bukkitConfig;

    private int musicDiscDistance;
    private float musicDiscVolume;
    private int maxDownloadSize;
    private String locale;


    public int getMusicDiscDistance() {
        return musicDiscDistance;
    }

    public float getMusicDiscVolume() {
        return musicDiscVolume;
    }

    public int getMaxDownloadSize() {
        return maxDownloadSize;
    }

    public String getLocale() {
        return locale;
    }

    public void init() {
        saveDefaultConfig();
        plugin.reloadConfig();
        musicDiscDistance = bukkitConfig.getInt("music-disc-distance", 24);
        musicDiscVolume = Float.parseFloat(bukkitConfig.getString("music-disc-volume", "1"));
        maxDownloadSize = bukkitConfig.getInt("max-download-size", 50);
        locale = bukkitConfig.getString("locale", "ru_RU");
    }

    private void saveDefaultConfig() {
        plugin.saveDefaultConfig();
        bukkitConfig = plugin.getConfig();
    }
}
