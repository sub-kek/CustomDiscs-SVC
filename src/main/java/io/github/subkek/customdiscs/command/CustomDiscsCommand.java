package io.github.subkek.customdiscs.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.subcommand.*;
import io.github.subkek.customdiscs.util.Formatter;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class CustomDiscsCommand extends CommandAPICommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public CustomDiscsCommand() {
    super("customdiscs");

    this.withAliases("cd");
    this.withFullDescription("Main command of CustomDiscs-SVC plugin.");

    this.withSubcommand(new HelpSubCommand(this));
    this.withSubcommand(new ReloadSubCommand());
    this.withSubcommand(new DownloadSubCommand());
    this.withSubcommand(new CreateSubCommand());
    this.withSubcommand(new CreateYtSubCommand());

    this.executes(this::execute);
  }

  public void execute(CommandSender sender, CommandArguments arguments) {
    CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("unknown-command", findSubCommand("help").getSyntax()));
  }

  @NotNull
  private AbstractSubCommand findSubCommand(String commandName) {
    AbstractSubCommand subCommand = null;

    for (CommandAPICommand caSubCommand : getSubcommands()) {
      if (caSubCommand.getName().equals(commandName)) {
        subCommand = (AbstractSubCommand) caSubCommand;
        break;
      }
    }

    if (subCommand == null)
      throw new IllegalArgumentException(Formatter.format(
          "Command with name {0} doesn't exists!", commandName
      ));

    return subCommand;
  }
}
