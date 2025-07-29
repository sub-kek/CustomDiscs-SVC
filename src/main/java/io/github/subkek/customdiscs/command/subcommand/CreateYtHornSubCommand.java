package io.github.subkek.customdiscs.command.subcommand;

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

import java.util.List;

public class CreateYtHornSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public CreateYtHornSubCommand() {
    super("createythorn");

    this.withFullDescription(getDescription());
    this.withUsage(getUsage());

    this.withArguments(new TextArgument("youtube_url"));
    this.withArguments(new TextArgument("horn_name"));

    this.executesPlayer(this::executePlayer);
    this.executes(this::execute);
  }

  @Override
  public String getDescription() {
    return plugin.getLanguage().string("command.createythorn.description");
  }

  @Override
  public String getSyntax() {
    return plugin.getLanguage().string("command.createythorn.syntax");
  }

  @Override
  public boolean hasPermission(CommandSender sender) {
    return sender.hasPermission("customdiscs.createythorn");
  }

  @Override
  public void executePlayer(Player player, CommandArguments arguments) {
    if (!hasPermission(player)) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.no-permission"));
      return;
    }

    if (!plugin.youtubeSupport) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("error.command.youtube-disabled"));
      return;
    }

    if (!isGoatHornInHand(player)) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("command.createhorn.messages.error.not-holding-horn"));
      return;
    }

    String youtubeUrl = getArgumentValue(arguments, "youtube_url", String.class);
    String customName = getArgumentValue(arguments, "horn_name", String.class);

    if (customName.isEmpty()) {
      CustomDiscs.sendMessage(player, plugin.getLanguage().PComponent("command.createhorn.messages.error.horn-name-empty"));
      return;
    }

    if (!isValidYouTubeUrl(youtubeUrl)) {
      CustomDiscs.sendMessage(player, Component.text("Invalid YouTube URL!", NamedTextColor.RED));
      return;
    }

    // Sets the lore of the item to the quotes from the command.
    ItemStack horn = new ItemStack(player.getInventory().getItemInMainHand());

    ItemMeta meta = LegacyUtil.getItemMeta(horn);

    meta.setDisplayName(BukkitComponentSerializer.legacy().serialize(
        plugin.getLanguage().component("horn-name.youtube")));
    
    final TextComponent customLoreHorn = Component.text()
        .decoration(TextDecoration.ITALIC, false)
        .content(customName)
        .color(NamedTextColor.GRAY)
        .build();
    
    meta.addItemFlags(ItemFlag.values());
    meta.setLore(List.of(BukkitComponentSerializer.legacy().serialize(customLoreHorn)));

    PersistentDataContainer data = meta.getPersistentDataContainer();
    NamespacedKey hornFileMeta = Keys.CUSTOM_HORN.getKey();
    if (data.has(hornFileMeta, PersistentDataType.STRING))
      data.remove(Keys.CUSTOM_HORN.getKey());
    data.set(Keys.YOUTUBE_HORN.getKey(), Keys.YOUTUBE_HORN.getDataType(), youtubeUrl);

    player.getInventory().getItemInMainHand().setItemMeta(meta);

    CustomDiscs.sendMessage(player, plugin.getLanguage().component("command.createythorn.messages.link", youtubeUrl));
    CustomDiscs.sendMessage(player, plugin.getLanguage().component("command.createhorn.messages.name", customName));
  }

  @Override
  public void execute(CommandSender sender, CommandArguments arguments) {
    CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("error.command.cant-perform"));
  }

  private boolean isGoatHornInHand(Player player) {
    return player.getInventory().getItemInMainHand().getType().toString().equals("GOAT_HORN");
  }

  private boolean isValidYouTubeUrl(String url) {
    return url.contains("youtube.com") || url.contains("youtu.be");
  }
} 