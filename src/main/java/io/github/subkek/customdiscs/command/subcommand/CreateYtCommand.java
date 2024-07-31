package io.github.subkek.customdiscs.command.subcommand;

import io.github.subkek.customdiscs.CustomDiscs;
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

    if (isMusicDisc(player)) {
      if (args.length >= 3) {
        String customName = readName(args);

        if (customName.equalsIgnoreCase("")) {
          plugin.sendMessage(sender, plugin.getLanguage().PComponent("write-disc-name-error"));
          return;
        }

        ItemStack disc = new ItemStack(player.getInventory().getItemInMainHand());

        ItemMeta meta = disc.getItemMeta();

        meta.setDisplayName(plugin.getLanguage().string("youtube-disc"));
        final TextComponent customLoreSong = Component.text()
            .decoration(TextDecoration.ITALIC, false)
            .content(customName)
            .color(NamedTextColor.GRAY)
            .build();
        meta.addItemFlags(ItemFlag.values());
        meta.setLore(List.of(BukkitComponentSerializer.legacy().serialize(customLoreSong)));

        String youtubeUrl = args[1];

        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey discMeta = new NamespacedKey(CustomDiscs.getInstance(), "customdisc");
        if (data.has(discMeta, PersistentDataType.STRING))
          data.remove(new NamespacedKey(CustomDiscs.getInstance(), "customdisc"));
        data.set(new NamespacedKey(CustomDiscs.getInstance(), "customdiscyt"), PersistentDataType.STRING, youtubeUrl);

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

  private boolean isMusicDisc(Player p) {
    return p.getInventory().getItemInMainHand().getType().toString().contains("MUSIC_DISC");
  }
}
