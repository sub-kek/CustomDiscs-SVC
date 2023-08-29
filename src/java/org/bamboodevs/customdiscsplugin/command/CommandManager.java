package org.bamboodevs.customdiscsplugin.command;

import org.bamboodevs.customdiscsplugin.command.SubCommands.CreateCommand;
import org.bamboodevs.customdiscsplugin.command.SubCommands.CreateYtCommand;
import org.bamboodevs.customdiscsplugin.command.SubCommands.DownloadCommand;
import org.bamboodevs.customdiscsplugin.command.SubCommands.ReloadCommand;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CommandManager implements CommandExecutor, TabCompleter {

    private final ArrayList<SubCommand> subCommands = new ArrayList<>();

    public CommandManager() {
        subCommands.add(new CreateCommand());
        subCommands.add(new DownloadCommand());
        subCommands.add(new ReloadCommand());
        subCommands.add(new CreateYtCommand());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Только игрок может использовать эту команду!");
            return true;
        }

        Player player = (Player) sender;

        if (args.length > 0) {
            for (int i = 0; i < getSubCommands().size(); i++) {
                if (args[0].equalsIgnoreCase(getSubCommands().get(i).getName())) {
                    getSubCommands().get(i).perform(player, args);
                }
            }
        } else {
            player.sendMessage(ChatColor.AQUA + "§fКастомные пластинки");
            for (int i = 0; i < getSubCommands().size(); i++) {
                player.sendMessage(getSubCommands().get(i).getSyntax() + "§f: " + getSubCommands().get(i).getDescription());
            }
            player.sendMessage("\n");
            return true;
        }

        return true;
    }

    public ArrayList<SubCommand> getSubCommands() {
        return subCommands;
    }
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (args.length == 1) {
            List<String> arguments = new ArrayList<>();
            for (int i = 0; i < getSubCommands().size(); i++) {
                arguments.add(getSubCommands().get(i).getName());
            }
            return arguments;
        }

        return null;
    }
}
