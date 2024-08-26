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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.List;
import java.util.StringJoiner;

public class CreateYtCommand implements SubCommand {
  private final CustomDiscs plugin = CustomDiscs.getInstance();

  @Override
  public String getName() {
    return "createyt";
  }

  @Override
  public String getDescription() {
    return plugin.getLanguage().string("createyt-command-description");
  }

  @Override
  public String getSyntax() {
    return plugin.getLanguage().string("createyt-command-syntax");
  }

  @Override
  public boolean hasPermission(CommandSender sender) {
    return sender.hasPermission("customdiscs.createyt");
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
        String customName = readName(args);

        if (customName.equalsIgnoreCase("")) {
          plugin.sendMessage(sender, plugin.getLanguage().PComponent("write-disc-name-error"));
          return;
        }

        ItemStack disc = new ItemStack(player.getInventory().getItemInMainHand());

        ItemMeta meta = LegacyUtil.getItemMeta(disc);

        meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(
            plugin.getLanguage().component("youtube-disc")));
        final TextComponent customLoreSong = Component.text()
            .decoration(TextDecoration.ITALIC, false)
            .content(customName)
            .color(NamedTextColor.GRAY)
            .build();
        meta.addItemFlags(ItemFlag.values());
        meta.setLore(List.of(BukkitComponentSerializer.legacy().serialize(customLoreSong)));
        if (CustomDiscsConfiguration.useCustomModelData)
          meta.setCustomModelData(CustomDiscsConfiguration.customModelData);

        String youtubeUrl = args[1];

        PersistentDataContainer data = meta.getPersistentDataContainer();
        if (data.has(Keys.CUSTOM_DISC.getKey(), Keys.CUSTOM_DISC.getDataType()))
          data.remove(Keys.CUSTOM_DISC.getKey());
        data.set(Keys.YOUTUBE_DISC.getKey(), Keys.YOUTUBE_DISC.getDataType(), youtubeUrl);

        player.getInventory().getItemInMainHand().setItemMeta(meta);

        plugin.sendMessage(sender, plugin.getLanguage().component("disc-youtube-link", youtubeUrl));
        plugin.sendMessage(sender, plugin.getLanguage().component("disc-name-output", customName));
      } else {
        plugin.sendMessage(sender, plugin.getLanguage().PComponent("unknown-arguments-error", plugin.getLanguage().string("createyt-command-syntax")));
      }
    } else {
      plugin.sendMessage(sender, plugin.getLanguage().PComponent("disc-not-in-hand-error"));
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
