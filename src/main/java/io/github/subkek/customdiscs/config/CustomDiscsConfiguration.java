package io.github.subkek.customdiscs.config;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.utils.Languages;
import org.jetbrains.annotations.Nullable;
import org.simpleyaml.configuration.comments.CommentType;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;

public class CustomDiscsConfiguration {
  private static final CustomDiscs plugin = CustomDiscs.getInstance();
  private static final YamlFile config = new YamlFile();

  public static void load() {
    File configFile = Path.of(plugin.getDataFolder().getPath(), "config.yml").toFile();

    if (configFile.exists()) {
      try {
        config.load(configFile);
      } catch (IOException e) {
        plugin.getLogger().log(Level.SEVERE, "Error while load config: ", e);
      }
    }

    getString("info.version", "1.0", "Don't change this value");
    setComment("info",
        "CustomDiscs Configuration",
        "Join our Discord for support: https://discord.gg/eRvwvmEXWz");

    for (Method method : CustomDiscsConfiguration.class.getDeclaredMethods()) {
      if (Modifier.isStatic(method.getModifiers()) && Modifier.isPrivate(method.getModifiers()) && method.getParameterCount() == 0 &&
          method.getReturnType() == Void.TYPE && !method.getName().startsWith("lambda")) {
        method.setAccessible(true);
        try {
          method.invoke(null);
        } catch (Throwable t) {
          plugin.getLogger().log (Level.WARNING, "Failed to load configuration option from " + method.getName(), t);
        }
      }
    }

    try {
      config.save(configFile);
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Error while save config: ", e);
    }
  }

  private static void setComment(String key, String... comment) {
    if (config.contains(key) && comment.length > 0) {
      config.setComment(key, String.join("\n", comment), CommentType.BLOCK);
    }
  }

  private static void ensureDefault(String key, Object defaultValue, String... comment) {
    if (!config.contains(key))
      config.set(key, defaultValue);

    setComment(key, comment);
  }

  private static boolean getBoolean(String key, boolean defaultValue, String... comment) {
    return getBoolean(key, null, defaultValue, comment);
  }

  private static boolean getBoolean(String key, @Nullable String oldKey, boolean defaultValue, String... comment) {
    ensureDefault(key, defaultValue, comment);
    return config.getBoolean(key, defaultValue);
  }

  private static int getInt(String key, int defaultValue, String... comment) {
    return getInt(key, null, defaultValue, comment);
  }

  private static int getInt(String key, @Nullable String oldKey, int defaultValue, String... comment) {
    ensureDefault(key, defaultValue, comment);
    return config.getInt(key, defaultValue);
  }

  private static double getDouble(String key, double defaultValue, String... comment) {
    return getDouble(key, null, defaultValue, comment);
  }

  private static double getDouble(String key, @Nullable String oldKey, double defaultValue, String... comment) {
    ensureDefault(key, defaultValue, comment);
    return config.getDouble(key, defaultValue);
  }

  private static String getString(String key, String defaultValue, String... comment) {
    return getOldString(key, null, defaultValue, comment);
  }

  private static String getOldString(String key, @Nullable String oldKey, String defaultValue, String... comment) {
    ensureDefault(key, defaultValue, comment);
    return config.getString(key, defaultValue);
  }

  private static List<String> getStringList(String key, List<String> defaultValue, String... comment) {
    return getStringList(key, null, defaultValue, comment);
  }

  private static List<String> getStringList(String key, @Nullable String oldKey, List<String> defaultValue, String... comment) {
    ensureDefault(key, defaultValue, comment);
    return config.getStringList(key);
  }

  public static int musicDiscDistance;
  public static float musicDiscVolume;
  public static int maxDownloadSize;
  public static String locale;
  public static boolean discCleaning;
  public static int discsPlayed = 0;
  private static void customDiscsSettings() {
    musicDiscDistance = getInt("music-disc-distance", 16, "The distance from which music discs can be heard in blocks.");
    musicDiscVolume = Float.parseFloat(getString("music-disc-volume", "1", "The master volume of music discs from 0-1. (You can set values like 0.5 for 50% volume)."));
    maxDownloadSize = getInt("max-download-size", 50, "The maximum download size in megabytes.");
    discCleaning = getBoolean("cleaning-disc", false, "Enable disc cleaning before re-record disc", "This feature for my server use this not recommended");
    locale = getString("locale", Languages.ENGLISH.toString(), "Language of plugin", "Supported en_US, ru_RU");
    if (!Languages.languageExists(locale)) {
      locale = Languages.ENGLISH.toString();
      plugin.getLogger().warning("Your language is not supported! Please set supported language in the config! If you need your own translate edit any lang in jar.");
    }
  }
}