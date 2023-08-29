package org.bamboodevs.customdiscsplugin.command.SubCommands;

import org.bamboodevs.customdiscsplugin.CustomDiscs;
import org.bamboodevs.customdiscsplugin.command.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ReloadCommand extends SubCommand {

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "§fПерезагружает конфиг.";
    }

    @Override
    public String getSyntax() {
        return  "§3/cd reload";
    }

    @Override
    public void perform(Player player, String[] args) {
        if (!player.hasPermission("customdiscs.reload")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на использовать эту команду!");
            return;
        }

        CustomDiscs.getInstance().reloadConfig();
        player.sendMessage("§fКонфигурация перезагружена");
    }

}
