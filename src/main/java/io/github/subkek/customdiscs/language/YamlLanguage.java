package io.github.subkek.customdiscs.language;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.config.CustomDiscsConfiguration;
import io.github.subkek.customdiscs.util.Formatter;
import io.github.subkek.customdiscs.util.Language;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.simpleyaml.configuration.file.YamlFile;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.logging.Level;

public class YamlLanguage {
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  private final YamlFile language = new YamlFile();
  private final MiniMessage miniMessage = MiniMessage.miniMessage();

  @SuppressWarnings("all")
  public void init() {
    try {
      File languageFolder = Path.of(plugin.getDataFolder().getPath(), "language").toFile();
      languageFolder.mkdir();
      File languageFile = new File(languageFolder, Formatter.format("{0}.yml", CustomDiscsConfiguration.locale));
      boolean isNewFile = false;

      if (!languageFile.exists()) {
        InputStream inputStream = plugin.getClass().getClassLoader().getResourceAsStream(Formatter.format("language{0}{1}.yml", File.separator,
            languageExists(CustomDiscsConfiguration.locale) ? CustomDiscsConfiguration.locale : Language.ENGLISH.getLabel()
        ));
        Files.copy(inputStream, languageFile.toPath());
        isNewFile = true;
      }

      language.load(languageFile);

      if (isNewFile) {
        language.set("version", plugin.getDescription().getVersion());
        language.save(languageFile);
      }

      if (!language.getString("version").equals(plugin.getDescription().getVersion()) && !isNewFile) {
        Object oldLanguage = language.get("language");
        languageFile.delete();
        InputStream inputStream = plugin.getClass().getClassLoader().getResourceAsStream(Formatter.format("language{0}{1}.yml", File.separator, CustomDiscsConfiguration.locale));
        Files.copy(inputStream, languageFile.toPath());
        language.load(languageFile);
        language.set("version", plugin.getDescription().getVersion());
        language.set("language-old", oldLanguage);
        language.save(languageFile);
      }
    } catch (Throwable e) {
      plugin.getLogger().log(Level.SEVERE, "Error while loading language: ", e);
    }
  }

  public String get(String key) {
    return BukkitComponentSerializer.legacy().serialize(
        miniMessage.deserialize(
            language.getString(Formatter.format("language.{0}", key), "<unknown lang key>")));
  }

  public boolean languageExists(String label) {
    InputStream inputStream = plugin.getClass().getClassLoader().getResourceAsStream(Formatter.format("language{0}{1}.yml", File.separator, label));
    return !Objects.isNull(inputStream);
  }
}
