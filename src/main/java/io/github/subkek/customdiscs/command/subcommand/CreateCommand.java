package io.github.subkek.customdiscs.command.subcommand;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.Keys;
import io.github.subkek.customdiscs.LegacyUtil;
import io.github.subkek.customdiscs.command.SubCommand;
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
import java.util.List;
import java.util.StringJoiner;

public class CreateCommand implements SubCommand {
  private final CustomDiscs plugin = CustomDiscs.getInstance();

  @Override
  public String getName() {
    return "create";
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

  @Override
  public boolean canPerform(CommandSender sender) {
    return sender instanceof Player;
  }

  @Override
  public void perform(CommandSender sender, String[] args) {
    if (!hasPermission(sender)) {
      plugin.sendMessage(sender, plugin.getLanguage().PComponent("no-permission-error"));
      return;
    }

    if (!canPerform(sender)) {
      plugin.sendMessage(sender, plugin.getLanguage().PComponent("cant-perform-command-error"));
      return;
    }

    Player player = (Player) sender;

    if (LegacyUtil.isMusicDiscInHand(player)) {
      if (args.length >= 3) {
        String songName;
        String filename = args[1];
        if (filename.contains("../")) {
          plugin.sendMessage(sender, plugin.getLanguage().PComponent("invalid-file-name"));
          return;
        }

        String customName = readName(args);

        if (customName.equalsIgnoreCase("")) {
          plugin.sendMessage(sender, plugin.getLanguage().PComponent("write-disc-name-error"));
          return;
        }

        File getDirectory = new File(CustomDiscs.getInstance().getDataFolder(), "musicdata");
        File songFile = new File(getDirectory.getPath(), filename);
        if (songFile.exists()) {
          if (getFileExtension(filename).equals("wav") || getFileExtension(filename).equals("mp3") || getFileExtension(filename).equals("flac")) {
            songName = args[1];
          } else {
            plugin.sendMessage(sender, plugin.getLanguage().PComponent("unknown-extension-error"));
            return;
          }
        } else {
          plugin.sendMessage(sender, plugin.getLanguage().PComponent("file-not-found"));
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

        plugin.sendMessage(sender, plugin.getLanguage().component("disc-file-output", songName));
        plugin.sendMessage(sender, plugin.getLanguage().component("disc-name-output", customName));

      } else {
        plugin.sendMessage(sender, plugin.getLanguage().PComponent("unknown-arguments-error", plugin.getLanguage().string("create-command-syntax")));
      }
    } else {
      plugin.sendMessage(sender, plugin.getLanguage().PComponent("disc-not-in-hand-error"));
    }
  }

  private String getFileExtension(String s) {
    int index = s.lastIndexOf(".");
    if (index > 0) {
      return s.substring(index + 1);
    } else {
      return "";
    }
  }

  private String readName(String[] args) {
    StringJoiner name = new StringJoiner(" ");

    for (int i = 0; i < args.length; i++) {
      if (i > 1) name.add(args[i]);
    }

    return name.toString();
  }
}
