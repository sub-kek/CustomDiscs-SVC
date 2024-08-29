package io.github.subkek.customdiscs.command.subcommand;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.AbstractSubCommand;
import io.github.subkek.customdiscs.command.CustomDiscsCommand;
import org.bukkit.command.CommandSender;

public class HelpSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();
  private final CustomDiscsCommand cdCommand;

  public HelpSubCommand(CustomDiscsCommand cdCommand) {
    super("help");

    this.cdCommand = cdCommand;

    this.withFullDescription(getDescription());
    this.withUsage(getSyntax());

    this.executes(this::execute);
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

  public void execute(CommandSender sender, CommandArguments arguments) {
    if (!hasPermission(sender)) {
      CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("no-permission-error"));
      return;
    }

    CustomDiscs.sendMessage(sender, plugin.getLanguage().component("help-header"));
    for (CommandAPICommand caSubCommand : cdCommand.getSubcommands()) {
      AbstractSubCommand subCommand = (AbstractSubCommand) caSubCommand;
      if (subCommand.hasPermission(sender)) {
        CustomDiscs.sendMessage(sender, plugin.getLanguage().component("help-command", subCommand.getSyntax(), subCommand.getDescription()));
      }
    }

    CustomDiscs.sendMessage(sender, plugin.getLanguage().component("help-footer"));
  }
}
