package io.github.subkek.customdiscs.command;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.SubCommands.CreateCommand;
import io.github.subkek.customdiscs.command.SubCommands.CreateYtCommand;
import io.github.subkek.customdiscs.command.SubCommands.DownloadCommand;
import io.github.subkek.customdiscs.command.SubCommands.ReloadCommand;
import io.github.subkek.customdiscs.utils.Formatter;
import lombok.Getter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CustomDiscsCommand implements CommandExecutor, TabCompleter {
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  @Getter private final HashMap<String, SubCommand> subCommands = new HashMap<>();

  public CustomDiscsCommand() {
    subCommands.put("create", new CreateCommand());
    subCommands.put("download", new DownloadCommand());
    subCommands.put("reload", new ReloadCommand());
    subCommands.put("createyt", new CreateYtCommand());
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(Formatter.format(plugin.language.get("only-player-command-error"), true));
      return true;
    }

    if (args.length > 0) {
      for (SubCommand subCommand : getSubCommands().values()) {
        if (subCommand.getName().equals(args[0])) {
          subCommand.perform(player, args);
          break;
        }
      }
    } else {
      player.sendMessage(plugin.language.get("help-header"));
      for (SubCommand subCommand : getSubCommands().values()) {
        player.sendMessage(Formatter.format(plugin.language.get("help-command"),subCommand.getSyntax(), subCommand.getDescription()));
      }
      if (plugin.config.isDiscCleaning())
        player.sendMessage(plugin.language.get("help-disc-cleaning"));
      player.sendMessage(plugin.language.get("help-footer"));
      return true;
    }
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    List<String> arguments = new ArrayList<>();

    switch (args.length) {
      case 1 -> {
        for (SubCommand subCommand : getSubCommands().values()) {
          if (subCommand.hasPermission(sender)) arguments.add(subCommand.getName());
        }
      }

      case 2 -> {
        switch (args[0]) {
          case "create" -> {
            if (getSubCommands().get("create").hasPermission(sender)) arguments.add(plugin.language.get("download-command-2nd-argument"));
          }

          case "createyt" -> {
            if (getSubCommands().get("createyt").hasPermission(sender)) arguments.add(plugin.language.get("createyt-command-1st-argument"));
          }

          case "download" -> {
            if (getSubCommands().get("download").hasPermission(sender)) arguments.add(plugin.language.get("download-command-1st-argument"));
          }

          default -> arguments.add(plugin.language.get("unknown-argument-complete"));
        }
      }

      case 3 -> {
        switch (args[0]) {
          case "download" -> {
            if (getSubCommands().get("download").hasPermission(sender)) arguments.add(plugin.language.get("download-command-2nd-argument"));
          }

          default -> arguments.add(plugin.language.get("unknown-argument-complete"));
        }
      }

      default -> {
        if (args.length > 2 && (args[0].equals("create") || args[0].equals("createyt"))) {
            if (getSubCommands().get(args[0]).hasPermission(sender)) arguments.add(plugin.language.get("create-commands-2nd-argument"));
        } else {
          arguments.add(plugin.language.get("unknown-argument-complete"));
        }
      }
    }

    return arguments;
  }
}