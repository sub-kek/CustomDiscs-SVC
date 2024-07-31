package io.github.subkek.customdiscs.command;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.subcommand.*;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

public class CustomDiscsCommand implements CommandExecutor {
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  @Getter private final HashMap<String, SubCommand> subCommands = new HashMap<>();

  public CustomDiscsCommand() {
    subCommands.put("create", new CreateCommand());
    subCommands.put("download", new DownloadCommand());
    subCommands.put("reload", new ReloadCommand());
    subCommands.put("createyt", new CreateYtCommand());
    subCommands.put("help", new HelpCommand(this));
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (args.length > 0) {
      for (SubCommand subCommand : getSubCommands().values()) {
        if (subCommand.getName().equals(args[0])) {
          subCommand.perform(sender, args);
          return true;
        }
      }
    }
    plugin.sendMessage(sender, plugin.getLanguage().PComponent("unknown-command", getSubCommands().get("help").getSyntax()));
    return true;
  }
}