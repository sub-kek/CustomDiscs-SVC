package io.github.subkek.customdiscs.command.subcommand;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.SubCommand;
import io.github.subkek.customdiscs.config.CustomDiscsConfiguration;
import io.github.subkek.customdiscs.util.Formatter;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements SubCommand {
  private final CustomDiscs plugin = CustomDiscs.getInstance();

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
  public boolean canPerform(CommandSender sender) {
    return true;
  }

  @Override
  public void perform(CommandSender sender, String[] args) {
    if (!hasPermission(sender)) {
      sender.sendMessage(Formatter.format(plugin.language.get("no-permission-error"), true));
      return;
    }

    if (!canPerform(sender)) {
      sender.sendMessage(Formatter.format(plugin.language.get("cant-perform-command-error"), true));
      return;
    }

    CustomDiscsConfiguration.load();
    plugin.language.init();
    sender.sendMessage(Formatter.format(plugin.language.get("config-reloaded"), true));
  }
}
