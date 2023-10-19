package org.bamboodevs.customdiscsplugin.command.SubCommands;

import org.apache.commons.io.FileUtils;
import org.bamboodevs.customdiscsplugin.CustomDiscs;
import org.bamboodevs.customdiscsplugin.command.SubCommand;
import org.bamboodevs.customdiscsplugin.utils.Formatter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;

public class DownloadCommand extends SubCommand {
    private final CustomDiscs plugin = CustomDiscs.getInstance();

    @Override
    public String getName() {
        return "download";
    }

    @Override
    public String getDescription() {
        return plugin.language.get("download-command-description");
    }

    @Override
    public String getSyntax() {
        return plugin.language.get("download-command-syntax");
    }

    @Override
    public void perform(Player player, String[] args) {
        // /cd   download    url   filename
        //         [0]       [1]     [2]

        if (!player.hasPermission("customdiscs.download")) {
            player.sendMessage(Formatter.format(plugin.language.get("no-permission-error"), true));
            return;
        }

        if (args.length<3) {
            player.sendMessage(Formatter.format(plugin.language.get("unknown-arguments-error"), true, plugin.language.get("download-command-syntax")));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                URL fileURL = new URL(args[1]);
                String filename = args[2];
                if (filename.contains("../")) {
                    player.sendMessage(Formatter.format(plugin.language.get("invalid-file-name"), true));
                    return;
                }

                if (!getFileExtension(filename).equals("wav") && !getFileExtension(filename).equals("mp3") && !getFileExtension(filename).equals("flac")) {
                    player.sendMessage(Formatter.format(plugin.language.get("unknown-extension-error"), true));
                    return;
                }

                player.sendMessage(Formatter.format(plugin.language.get("downloading"), true));
                Path downloadPath = Path.of(plugin.getDataFolder().getPath(), "musicdata", filename);
                File downloadFile = new File(downloadPath.toUri());

                URLConnection connection = fileURL.openConnection();

                if (connection != null) {
                    long size = connection.getContentLengthLong() / 1048576;
                    if (size > plugin.config.getMaxDownloadSize()) {
                        player.sendMessage(Formatter.format(plugin.language.get("download-file-large-error"), true, String.valueOf(plugin.config.getMaxDownloadSize())));
                        return;
                    }
                }

                FileUtils.copyURLToFile(fileURL, downloadFile);

                player.sendMessage(Formatter.format(plugin.language.get("download-successful"), true));
                player.sendMessage(Formatter.format(plugin.language.get("create-disc-tooltip"), true, plugin.language.get("create-command-syntax")));
            } catch (Exception e) {
                player.sendMessage(Formatter.format(plugin.language.get("download-error"), true));
            }
        });
    }

    private String getFileExtension(String s) {
        int index = s.lastIndexOf(".");
        if (index > 0) {
            return s.substring(index + 1);
        } else {
            return "";
        }
    }

}
