package io.github.subkek.customdiscs.language;

import io.github.subkek.customdiscs.CustomDiscs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

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
      CustomDiscs.LOGGER.error("Error", e);
    }
  }

  public String get(String key) {
    return properties.getProperty(key);
  }

  public Component getAsComponent(String key) {
    return MiniMessage.miniMessage().deserialize(properties.getProperty(key));
  }
}