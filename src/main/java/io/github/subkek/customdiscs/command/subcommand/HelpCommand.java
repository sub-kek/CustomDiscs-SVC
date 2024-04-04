package io.github.subkek.customdiscs.command.subcommand;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.CustomDiscsCommand;
import io.github.subkek.customdiscs.command.SubCommand;
import io.github.subkek.customdiscs.config.CustomDiscsConfiguration;
import io.github.subkek.customdiscs.util.Formatter;
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
    return plugin.language.get("help-command-description");
  }

  @Override
  public String getSyntax() {
    return plugin.language.get("help-command-syntax");
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
      sender.sendMessage(Formatter.format(plugin.language.get("no-permission-error"), true));
      return;
    }

    if (!canPerform(sender)) {
      sender.sendMessage(Formatter.format(plugin.language.get("cant-perform-command-error"), true));
      return;
    }

    sender.sendMessage(plugin.language.get("help-header"));
    for (SubCommand subCommand : customDiscsCommand.getSubCommands().values()) {
      if (subCommand.hasPermission(sender)) {
        sender.sendMessage(Formatter.format(plugin.language.get("help-command"),subCommand.getSyntax(), subCommand.getDescription()));
      }
    }

    if (CustomDiscsConfiguration.discCleaning)
      sender.sendMessage(plugin.language.get("help-disc-cleaning"));
    sender.sendMessage(plugin.language.get("help-footer"));
  }
}
