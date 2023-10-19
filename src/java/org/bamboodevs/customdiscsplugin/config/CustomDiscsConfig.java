package org.bamboodevs.customdiscsplugin.config;

import org.bamboodevs.customdiscsplugin.CustomDiscs;
import org.bamboodevs.customdiscsplugin.utils.Languages;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class CustomDiscsConfig {
    private final CustomDiscs plugin = CustomDiscs.getInstance();
    private FileConfiguration bukkitConfig = null;

    private int musicDiscDistance;
    private float musicDiscVolume;
    private int maxDownloadSize;
    private String locale;
    private boolean discCleaning;


    public int getMusicDiscDistance() { return musicDiscDistance; }
    public float getMusicDiscVolume() { return musicDiscVolume; }
    public int getMaxDownloadSize() { return maxDownloadSize; }
    public String getLocale() { return locale; }
    public boolean getDiscCleaning() { return discCleaning; }

    public void init() {
        if (bukkitConfig == null) bukkitConfig = plugin.getConfig();

        musicDiscDistance = bukkitConfig.getInt("music-disc-distance", 16);
        musicDiscVolume = Float.parseFloat(bukkitConfig.getString("music-disc-volume", "1"));
        maxDownloadSize = bukkitConfig.getInt("max-download-size", 50);
        discCleaning = bukkitConfig.getBoolean("cleaning-disc", false);

        locale = bukkitConfig.getString("locale", Languages.ENGLISH.toString());
        if (!Languages.languageExists(locale)) {
            locale = Languages.ENGLISH.toString();
            plugin.getSLF4JLogger().warn("Your language is not supported! Please set supported language in config!");
        }
    }

    public void reload() {
        plugin.reloadConfig();
    }

    public void saveDefaultConfig() {
        plugin.saveDefaultConfig();
    }
}
