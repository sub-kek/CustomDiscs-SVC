package io.github.subkek.customdiscs.util;

import io.github.subkek.customdiscs.*;
import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.nio.file.Path;
import java.util.Objects;

public class PlayUtil {
  private static final CustomDiscs plugin = CustomDiscs.getPlugin();

  public static void playStandard(Block block, ItemStack disc) {
    plugin.discsPlayed++;

    ItemMeta discMeta = LegacyUtil.getItemMeta(disc);

    String soundFileName = discMeta.getPersistentDataContainer()
        .get(Keys.CUSTOM_DISC.getKey(), Keys.CUSTOM_DISC.getDataType());

    Path soundFilePath = Path.of(plugin.getDataFolder().getPath(), "musicdata", soundFileName);

    if (soundFilePath.toFile().exists()) {
      String songName = Objects.requireNonNull(discMeta.getLore()).get(0);
      songName = songName.replace("§7", "<gray>");

      Component customActionBarSongPlaying = plugin.getLanguage().component("now-playing", songName);

      PlayerManager.getInstance().play(soundFilePath, block, customActionBarSongPlaying);
    }
  }

  public static void playLava(Block block, ItemStack disc) {
    plugin.discsPlayed++;

    ItemMeta discMeta = LegacyUtil.getItemMeta(disc);

    String soundLink = discMeta.getPersistentDataContainer()
        .get(Keys.YOUTUBE_DISC.getKey(), Keys.YOUTUBE_DISC.getDataType());

    String songName = Objects.requireNonNull(discMeta.getLore()).get(0);
    songName = songName.replace("§7", "<gray>");

    Component customActionBarSongPlaying = plugin.getLanguage().component("now-playing", songName);

    LavaPlayerManager.getInstance().play(block, soundLink, customActionBarSongPlaying);
  }
}
