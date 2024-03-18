package io.github.subkek.customdiscs.command;

import org.bukkit.command.CommandSender;

public interface SubCommand {
  String getName();
  String getDescription();
  String getSyntax();
  boolean hasPermission(CommandSender sender);
  boolean canPerform(CommandSender sender);
  void perform(CommandSender sender, String[] args);
}
