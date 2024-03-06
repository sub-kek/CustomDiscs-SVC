package io.github.subkek.customdiscs.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface SubCommand {
  String getName();

  String getDescription();

  String getSyntax();
  boolean hasPermission(CommandSender sender);

  void perform(Player player, String[] args);
}
