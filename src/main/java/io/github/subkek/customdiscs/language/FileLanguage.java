package io.github.subkek.customdiscs.language;

import io.github.subkek.customdiscs.CustomDiscs;

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
            CustomDiscs.LOGGER.error(e.getStackTrace());
        }
    }

    public String get(String key) {
        return properties.getProperty(key).replace("&", "§");
    }
}