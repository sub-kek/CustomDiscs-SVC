package io.github.subkek.customdiscs.command.subcommand;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.SubCommand;
import io.github.subkek.customdiscs.config.CustomDiscsConfiguration;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements SubCommand {
  private final CustomDiscs plugin = CustomDiscs.getInstance();

  @Override
  public String getName() {
    return "reload";
  }

  @Override
  public String getDescription() {
    return plugin.getLanguage().string("reload-command-description");
  }

  @Override
  public String getSyntax() {
    return plugin.getLanguage().string("reload-command-syntax");
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
      plugin.sendMessage(sender, plugin.getLanguage().PComponent("no-permission-error"));
      return;
    }

    if (!canPerform(sender)) {
      plugin.sendMessage(sender, plugin.getLanguage().PComponent("cant-perform-command-error"));
      return;
    }

    CustomDiscsConfiguration.load();
    plugin.getLanguage().init();
    plugin.sendMessage(sender, plugin.getLanguage().PComponent("config-reloaded"));
  }
}
