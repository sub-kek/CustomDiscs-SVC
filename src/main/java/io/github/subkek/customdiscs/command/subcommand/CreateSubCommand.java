package io.github.subkek.customdiscs.command.subcommand;

import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.Keys;
import io.github.subkek.customdiscs.LegacyUtil;
import io.github.subkek.customdiscs.command.AbstractSubCommand;
import io.github.subkek.customdiscs.config.CustomDiscsConfiguration;
import net.kyori.adventure.platform.bukkit.BukkitComponentSerializer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class CreateSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public CreateSubCommand() {
    super("create");

    this.withArguments(new StringArgument("filename").replaceSuggestions(ArgumentSuggestions.stringCollection((sender) -> {
      File musicDataFolder = new File(this.plugin.getDataFolder(), "musicdata");
      if (!musicDataFolder.isDirectory()) {
        return List.of();
      }

      File[] files = musicDataFolder.listFiles();
      if (files == null) {
        return List.of();
      }

      return Arrays.stream(files).filter(file -> !file.isDirectory()).map(File::getName).toList();
    })));
    this.withArguments(new TextArgument("song_name"));

    this.executesPlayer(this::executePlayer);
    this.executes(this::execute);
  }

  @Override
  public String getDescription() {
    return plugin.getLanguage().string("create-command-description");
  }

  @Override
  public String getSyntax() {
    return plugin.getLanguage().string("create-command-syntax");
  }

  @Override
  public boolean hasPermission(CommandSender sender) {
    return sender.hasPermission("customdiscs.create");
  }

  private void executePlayer(Player player, CommandArguments arguments) {
    if (!hasPermission(player)) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("no-permission-error"));
      return;
    }

    if (!LegacyUtil.isMusicDiscInHand(player)) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("disc-not-in-hand-error"));
      return;
    }

    String filename = getArgumentValue(arguments, "filename", String.class);
    if (filename.contains("../")) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("invalid-file-name"));
      return;
    }

    String customName = getArgumentValue(arguments, "song_name", String.class);

    if (customName.isEmpty()) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("write-disc-name-error"));
      return;
    }

    File getDirectory = new File(CustomDiscs.getPlugin().getDataFolder(), "musicdata");
    File songFile = new File(getDirectory.getPath(), filename);
    if (songFile.exists()) {
      if (!getFileExtension(filename).equals("wav") && !getFileExtension(filename).equals("mp3") && !getFileExtension(filename).equals("flac")) {
        CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("unknown-extension-error"));
        return;
      }
    } else {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("file-not-found"));
      return;
    }

    //Sets the lore of the item to the quotes from the command.
    ItemStack disc = new ItemStack(player.getInventory().getItemInMainHand());

    ItemMeta meta = LegacyUtil.getItemMeta(disc);

    meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(
        plugin.getLanguage().component("simple-disc")));
    final TextComponent customLoreSong = Component.text()
        .decoration(TextDecoration.ITALIC, false)
        .content(customName)
        .color(NamedTextColor.GRAY)
        .build();
    meta.addItemFlags(ItemFlag.values());
    meta.setLore(List.of(BukkitComponentSerializer.legacy().serialize(customLoreSong)));
    if (CustomDiscsConfiguration.useCustomModelData)
      meta.setCustomModelData(CustomDiscsConfiguration.customModelData);

    PersistentDataContainer data = meta.getPersistentDataContainer();
    NamespacedKey discYtMeta = Keys.YOUTUBE_DISC.getKey();
    if (data.has(discYtMeta, PersistentDataType.STRING))
      data.remove(Keys.YOUTUBE_DISC.getKey());
    data.set(Keys.CUSTOM_DISC.getKey(), Keys.CUSTOM_DISC.getDataType(), filename);

    player.getInventory().getItemInMainHand().setItemMeta(meta);

    CustomDiscs.sendMessage(player, plugin.getLanguage().component("disc-file-output", filename));
    CustomDiscs.sendMessage(player, plugin.getLanguage().component("disc-name-output", customName));
  }


  private void execute(CommandSender sender, CommandArguments arguments) {
    CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("cant-perform-command-error"));
  }

  private String getFileExtension(String s) {
    int index = s.lastIndexOf(".");
    if (index > 0) {
      return s.substring(index + 1);
    } else {
      return "";
    }
  }
}
