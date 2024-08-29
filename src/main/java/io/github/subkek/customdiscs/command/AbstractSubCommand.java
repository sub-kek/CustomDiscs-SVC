package io.github.subkek.customdiscs.command;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.subkek.customdiscs.util.Formatter;
import org.bukkit.command.CommandSender;

public abstract class AbstractSubCommand extends CommandAPICommand {
  public AbstractSubCommand(String commandName) {
    super(commandName);
  }

  protected <T> T getArgumentValue(CommandArguments arguments, String nodeName, Class<T> argumentType) {
    T value;
    if ((value = arguments.getByClass(nodeName, argumentType)) == null)
      throw new IllegalArgumentException(Formatter.format(
          "Couldn't find argument {0} with name", nodeName
      ));
    return value;
  }

  public abstract String getDescription();

  public abstract String getSyntax();

  public abstract boolean hasPermission(CommandSender sender);
}
