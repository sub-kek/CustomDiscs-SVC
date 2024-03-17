package io.github.subkek.customdiscs.config;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.utils.Languages;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;

@Getter
public class CustomDiscsConfig {
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  private FileConfiguration bukkitConfig = plugin.getConfig();

  private int musicDiscDistance;
  private float musicDiscVolume;
  private int maxDownloadSize;
  private String locale;
  private boolean discCleaning;
  @Setter private int discsPlayed = 0;

  public void init() {
    musicDiscDistance = bukkitConfig.getInt("music-disc-distance", 16);
    musicDiscVolume = Float.parseFloat(bukkitConfig.getString("music-disc-volume", "1"));
    maxDownloadSize = bukkitConfig.getInt("max-download-size", 50);
    discCleaning = bukkitConfig.getBoolean("cleaning-disc", false);

    locale = bukkitConfig.getString("locale", Languages.ENGLISH.toString());
    if (!Languages.languageExists(locale)) {
      locale = Languages.ENGLISH.toString();
      plugin.getLogger().warning("Your language is not supported! Please set supported language in the config! If you need your own translate edit any lang in jar.");
    }
  }

  public void reload() {
    plugin.reloadConfig();
    bukkitConfig = plugin.getConfig();
    init();
  }

  public void saveDefaultConfig() {
    plugin.saveDefaultConfig();
  }
}