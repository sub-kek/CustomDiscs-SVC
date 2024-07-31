package io.github.subkek.customdiscs.command;

import io.github.subkek.customdiscs.CustomDiscs;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class CustomDiscsTabCompleter implements TabCompleter {
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  private final CustomDiscsCommand parentCommand;

  @Nullable
  @Override
  public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    List<String> arguments = new ArrayList<>();

    switch (args.length) {
      case 1 -> {
        for (SubCommand subCommand : parentCommand.getSubCommands().values()) {
          if (subCommand.hasPermission(sender)) arguments.add(subCommand.getName());
        }
      }

      case 2 -> {
        switch (args[0]) {
          case "create" -> {
            if (parentCommand.getSubCommands().get("create").hasPermission(sender))
              arguments.add(plugin.getLanguage().string("download-command-2nd-argument"));
          }

          case "createyt" -> {
            if (parentCommand.getSubCommands().get("createyt").hasPermission(sender))
              arguments.add(plugin.getLanguage().string("createyt-command-1st-argument"));
          }

          case "download" -> {
            if (parentCommand.getSubCommands().get("download").hasPermission(sender))
              arguments.add(plugin.getLanguage().string("download-command-1st-argument"));
          }

          default -> arguments.add(plugin.getLanguage().string("unknown-argument-complete"));
        }
      }

      case 3 -> {
        switch (args[0]) {
          case "download" -> {
            if (parentCommand.getSubCommands().get("download").hasPermission(sender))
              arguments.add(plugin.getLanguage().string("download-command-2nd-argument"));
          }

          case "create", "createyt" -> {
            if (parentCommand.getSubCommands().get(args[0]).hasPermission(sender))
              arguments.add(plugin.getLanguage().string("create-commands-2nd-argument"));
          }

          default -> arguments.add(plugin.getLanguage().string("unknown-argument-complete"));
        }
      }

      default -> {
        if (args.length > 2) {
          switch (args[0]) {
            case "create", "createyt" -> {
              if (parentCommand.getSubCommands().get(args[0]).hasPermission(sender))
                arguments.add(plugin.getLanguage().string("create-commands-2nd-argument"));
            }

            default -> arguments.add(plugin.getLanguage().string("unknown-argument-complete"));
          }
        } else {
          arguments.add(plugin.getLanguage().string("unknown-argument-complete"));
        }
      }
    }

    return arguments;
  }
}
