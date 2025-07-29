package io.github.subkek.customdiscs.command.subcommand;

import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.Keys;
import io.github.subkek.customdiscs.command.AbstractSubCommand;
import io.github.subkek.customdiscs.util.LegacyUtil;
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

public class CreateHornSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public CreateHornSubCommand() {
    super("createhorn");

    this.withFullDescription(getDescription());
    this.withUsage(getUsage());

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
    this.withArguments(new TextArgument("horn_name"));

    this.executesPlayer(this::executePlayer);
    this.executes(this::execute);
  }

  @Override
  public String getDescription() {
    return plugin.getLanguage().string("command.createhorn.description");
  }

  @Override
  public String getSyntax() {
    return plugin.getLanguage().string("command.createhorn.syntax");
  }

  @Override
  public boolean hasPermission(CommandSender sender) {
    return sender.hasPermission("customdiscs.createhorn");
  }

  @Override
  public void executePlayer(Player player, CommandArguments arguments) {
    if (!hasPermission(player)) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.no-permission"));
      return;
    }

    if (!isGoatHornInHand(player)) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("command.createhorn.messages.error.not-holding-horn"));
      return;
    }

    String filename = getArgumentValue(arguments, "filename", String.class);
    if (filename.contains("../")) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.invalid-filename"));
      return;
    }

    String customName = getArgumentValue(arguments, "horn_name", String.class);

    if (customName.isEmpty()) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("command.createhorn.messages.error.horn-name-empty"));
      return;
    }

    File getDirectory = new File(CustomDiscs.getPlugin().getDataFolder(), "musicdata");
    File soundFile = new File(getDirectory.getPath(), filename);
    if (soundFile.exists()) {
      if (!getFileExtension(filename).equals("wav") && !getFileExtension(filename).equals("mp3") && !getFileExtension(filename).equals("flac")) {
        CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.unknown-extension"));
        return;
      }
    } else {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.file.not-found"));
      return;
    }

    // Sets the lore of the item to the quotes from the command.
    ItemStack horn = new ItemStack(player.getInventory().getItemInMainHand());

    ItemMeta meta = LegacyUtil.getItemMeta(horn);

    meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(
        plugin.getLanguage().component("horn-name.simple")));
    
    final TextComponent customLoreHorn = Component.text()
        .decoration(TextDecoration.ITALIC, false)
        .content(customName)
        .color(NamedTextColor.GRAY)
        .build();
    
    meta.addItemFlags(ItemFlag.values());
    meta.setLore(List.of(BukkitComponentSerializer.legacy().serialize(customLoreHorn)));

    PersistentDataContainer data = meta.getPersistentDataContainer();
    NamespacedKey hornYtMeta = Keys.YOUTUBE_HORN.getKey();
    if (data.has(hornYtMeta, PersistentDataType.STRING))
      data.remove(Keys.YOUTUBE_HORN.getKey());
    data.set(Keys.CUSTOM_HORN.getKey(), Keys.CUSTOM_HORN.getDataType(), filename);

    player.getInventory().getItemInMainHand().setItemMeta(meta);

    CustomDiscs.sendMessage(player, plugin.getLanguage().component("command.createhorn.messages.file", filename));
    CustomDiscs.sendMessage(player, plugin.getLanguage().component("command.createhorn.messages.name", customName));
  }

  @Override
  public void execute(CommandSender sender, CommandArguments arguments) {
    CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("error.command.cant-perform"));
  }

  private boolean isGoatHornInHand(Player player) {
    return player.getInventory().getItemInMainHand().getType().toString().equals("GOAT_HORN");
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