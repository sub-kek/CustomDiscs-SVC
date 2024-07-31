package io.github.subkek.customdiscs.command.subcommand;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.CustomDiscsCommand;
import io.github.subkek.customdiscs.command.SubCommand;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.CommandSender;

@RequiredArgsConstructor
public class HelpCommand implements SubCommand {
  private final CustomDiscsCommand customDiscsCommand;
  private final CustomDiscs plugin = CustomDiscs.getInstance();

  @Override
  public String getName() {
    return "help";
  }

  @Override
  public String getDescription() {
    return plugin.getLanguage().string("help-command-description");
  }

  @Override
  public String getSyntax() {
    return plugin.getLanguage().string("help-command-syntax");
  }

  @Override
  public boolean hasPermission(CommandSender sender) {
    return sender.hasPermission("customdiscs.help");
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

    plugin.sendMessage(sender, plugin.getLanguage().component("help-header"));
    for (SubCommand subCommand : customDiscsCommand.getSubCommands().values()) {
      if (subCommand.hasPermission(sender)) {
        plugin.sendMessage(sender, plugin.getLanguage().component("help-command", subCommand.getSyntax(), subCommand.getDescription()));
      }
    }

    plugin.sendMessage(sender, plugin.getLanguage().component("help-footer"));
  }
}
