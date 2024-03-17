package io.github.subkek.customdiscs.language;

import io.github.subkek.customdiscs.CustomDiscs;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Level;

public class FileLanguage {
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  private final Properties properties = new Properties();

  public void init(String fileName) {
    InputStreamReader inputStreamReader = new InputStreamReader(
        Objects.requireNonNull(
            plugin.getClass().getClassLoader().getResourceAsStream(fileName + ".properties")), StandardCharsets.UTF_8);
    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
    try {
      properties.load(bufferedReader);
      inputStreamReader.close();
      bufferedReader.close();
    } catch (IOException e) {
      plugin.getLogger().log(Level.SEVERE, "Error while load language: ", e);
    }
  }

  public String get(String key) {
    return BukkitComponentSerializer.legacy().serialize(
        MiniMessage.miniMessage().deserialize(
            properties.getProperty(key)));
  }

  public Component getAsComponent(String key) {
    return MiniMessage.miniMessage().deserialize(properties.getProperty(key));
  }
}