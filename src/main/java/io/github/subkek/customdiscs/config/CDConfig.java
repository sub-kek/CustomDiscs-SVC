package io.github.subkek.customdiscs.config;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.language.Language;
import io.github.subkek.customdiscs.util.Formatter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.simpleyaml.configuration.comments.CommentType;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;


@Getter
@RequiredArgsConstructor
@SuppressWarnings("unused")
public class CDConfig {
  private final YamlFile yaml = new YamlFile();
  private final File configFile;

  public void init() {
    if (configFile.exists()) {
      try {
        yaml.load(configFile);
      } catch (IOException e) {
        CustomDiscs.error("Error loading config: ", e);
      }
    }

    getString("info", "It's really needed only for the comment");
    setComment("info",
        "CustomDiscs Configuration",
        "Join our Discord for support: https://discord.gg/eRvwvmEXWz");

    for (Method method : this.getClass().getDeclaredMethods()) {
      if (Modifier.isPrivate(method.getModifiers()) &&
          method.getReturnType().equals(Void.TYPE) &&
          method.getName().endsWith("Settings")
      ) {
        try {
          method.invoke(this);
        } catch (Throwable t) {
          CustomDiscs.error("Failed to load configuration option from {0}", t, method.getName());
        }
      }
    }

    save();
  }

  public void save() {
    try {
      yaml.save(configFile);
    } catch (IOException e) {
      CustomDiscs.error("Error saving config: ", e);
    }
  }

  private void setComment(String key, String... comment) {
    if (yaml.contains(key) && comment.length > 0) {
      yaml.setComment(key, String.join("\n", comment), CommentType.BLOCK);
    }
  }

  private void ensureDefault(String key, Object defaultValue, String... comment) {
    if (!yaml.contains(key))
      yaml.set(key, defaultValue);

    setComment(key, comment);
  }

  private boolean getBoolean(String key, boolean defaultValue, String... comment) {
    ensureDefault(key, defaultValue, comment);
    return yaml.getBoolean(key, defaultValue);
  }

  private int getInt(String key, int defaultValue, String... comment) {
    ensureDefault(key, defaultValue, comment);
    return yaml.getInt(key, defaultValue);
  }

  private double getDouble(String key, double defaultValue, String... comment) {
    ensureDefault(key, defaultValue, comment);
    return yaml.getDouble(key, defaultValue);
  }

  private String getString(String key, String defaultValue, String... comment) {
    ensureDefault(key, defaultValue, comment);
    return yaml.getString(key, defaultValue);
  }

  private List<String> getStringList(String key, List<String> defaultValue, String... comment) {
    ensureDefault(key, defaultValue, comment);
    return yaml.getStringList(key);
  }

  private String locale = Language.ENGLISH.getLabel();
  private boolean debug = false;

  private void globalSettings() {
    locale = getString("global.locale", locale, "Language of the plugin",
        Formatter.format(
            "Supported: {0}",
            Language.getAllSeparatedComma()
        )
    );
    debug = getBoolean("global.debug", debug);
  }

  private int maxDownloadSize = 50;
  private boolean useCustomModelData = false;
  private int customModelData = 0;
  private boolean useCustomModelDataYoutube = false;
  private int customModelDataYoutube = 0;

  private void commandSettings() {
    maxDownloadSize = getInt("command.download.max-size", maxDownloadSize,
        "The maximum download size in megabytes.");
    useCustomModelData = getBoolean("command.create.custom-model-data.enable", useCustomModelData);
    customModelData = getInt("command.create.custom-model-data.value", customModelData);
    useCustomModelDataYoutube = getBoolean("command.createyt.custom-model-data.enable", useCustomModelDataYoutube);
    customModelDataYoutube = getInt("command.createyt.custom-model-data.value", customModelDataYoutube);
  }

  private int musicDiscDistance = 64;
  private float musicDiscVolume = 1f;
  private boolean allowHoppers = true;

  private void discSettings() {
    musicDiscDistance = getInt("disc.distance", musicDiscDistance,
        "The distance from which music discs can be heard in blocks.");
    musicDiscVolume = Float.parseFloat(getString("disc.volume", String.valueOf(musicDiscVolume),
        "The master volume of music discs from 0-1.", "You can set values like 0.5 for 50% volume."
    ));
    allowHoppers = getBoolean("disc.allow-hoppers", allowHoppers);
  }

  private boolean youtubeOauth2 = false;

  private void providersSettings() {
    youtubeOauth2 = getBoolean("providers.youtube.use-oauth2", youtubeOauth2);
  }
}
