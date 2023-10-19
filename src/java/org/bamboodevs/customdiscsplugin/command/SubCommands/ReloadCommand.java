package org.bamboodevs.customdiscsplugin.command.SubCommands;

import org.bamboodevs.customdiscsplugin.CustomDiscs;
import org.bamboodevs.customdiscsplugin.command.SubCommand;
import org.bamboodevs.customdiscsplugin.utils.Formatter;
import org.bukkit.entity.Player;

public class ReloadCommand extends SubCommand {
    private final CustomDiscs plugin = CustomDiscs.getInstance();

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return plugin.language.get("reload-command-description");
    }

    @Override
    public String getSyntax() {
        return  plugin.language.get("reload-command-syntax");
    }

    @Override
    public void perform(Player player, String[] args) {
        if (!player.hasPermission("customdiscs.reload")) {
            player.sendMessage(Formatter.format(plugin.language.get("no-permission-error"), true));
            return;
        }

        plugin.config.reload();
        plugin.language.init(plugin.config.getLocale());
        player.sendMessage(Formatter.format(plugin.language.get("config-reloaded"), true));
    }
}
