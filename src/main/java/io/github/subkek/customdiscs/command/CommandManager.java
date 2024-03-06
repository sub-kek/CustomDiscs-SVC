package io.github.subkek.customdiscs.command;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.SubCommands.CreateCommand;
import io.github.subkek.customdiscs.command.SubCommands.CreateYtCommand;
import io.github.subkek.customdiscs.command.SubCommands.DownloadCommand;
import io.github.subkek.customdiscs.command.SubCommands.ReloadCommand;
import io.github.subkek.customdiscs.utils.Formatter;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter {
  private final CustomDiscs plugin = CustomDiscs.getInstance();
  @Getter private final ArrayList<SubCommand> subCommands = new ArrayList<>();
  private final MiniMessage miniMessage = MiniMessage.miniMessage();

  public CommandManager() {
    subCommands.add(new CreateCommand());
    subCommands.add(new DownloadCommand());
    subCommands.add(new ReloadCommand());
    subCommands.add(new CreateYtCommand());
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    if (!(sender instanceof Player player)) {
      sender.sendMessage(miniMessage.deserialize(Formatter.format(plugin.language.get("only-player-command-error"), true)));
      return true;
    }

    if (args.length > 0) {
      for (int i = 0; i < getSubCommands().size(); i++) {
        if (args[0].equalsIgnoreCase(getSubCommands().get(i).getName())) {
          getSubCommands().get(i).perform(player, args);
        }
      }
    } else {
      player.sendMessage(plugin.language.getAsComponent("help-header"));
      for (int i = 0; i < getSubCommands().size(); i++) {
        player.sendMessage(miniMessage.deserialize(Formatter.format(plugin.language.get("help-command"), getSubCommands().get(i).getSyntax(), getSubCommands().get(i).getDescription())));
      }
      if (plugin.config.isDiscCleaning())
        player.sendMessage(plugin.language.getAsComponent("help-disc-cleaning"));
      player.sendMessage(plugin.language.getAsComponent("help-footer"));
      return true;
    }
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
    List<String> arguments = new ArrayList<>();

    switch (args.length) {
      case 1 -> {
        for (SubCommand subCommand : getSubCommands()) {
          if (subCommand.hasPermission(sender)) arguments.add(subCommand.getName());
        }
      }

      default -> arguments.add(plugin.language.get("unknown-argument-complete"));
    }

    return arguments;
  }
}
