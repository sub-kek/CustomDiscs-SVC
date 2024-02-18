package io.github.subkek.customdiscs.command.SubCommands;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.utils.Formatter;
import org.bukkit.entity.Player;

public class ReloadCommand extends io.github.subkek.customdiscs.command.SubCommand {
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
