package io.github.subkek.customdiscs.command.SubCommands;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.SubCommand;
import io.github.subkek.customdiscs.utils.Formatter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReloadCommand implements SubCommand {
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  private final MiniMessage miniMessage = MiniMessage.miniMessage();

  @Override
  public String getName() {
    return "reload";
  }

  @Override
  public String getDescription() {
    return plugin.language.get("reload-command-description");
  }

  @Override
  public String getSyntax() {
    return plugin.language.get("reload-command-syntax");
  }

  @Override
  public boolean hasPermission(CommandSender sender) {
    return sender.hasPermission("customdiscs.reload");
  }

  @Override
  public void perform(Player player, String[] args) {
    if (!hasPermission(player)) {
      player.sendMessage(miniMessage.deserialize(Formatter.format(plugin.language.get("no-permission-error"), true)));
      return;
    }

    plugin.config.reload();
    plugin.language.init(plugin.config.getLocale());
    player.sendMessage(miniMessage.deserialize(Formatter.format(plugin.language.get("config-reloaded"), true)));
  }
}
