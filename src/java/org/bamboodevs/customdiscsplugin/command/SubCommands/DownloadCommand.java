package org.bamboodevs.customdiscsplugin.command.SubCommands;

import org.bamboodevs.customdiscsplugin.CustomDiscs;
import org.bamboodevs.customdiscsplugin.command.SubCommand;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;

public class DownloadCommand extends SubCommand {

    CustomDiscs customDiscs = CustomDiscs.getInstance();

    @Override
    public String getName() {
        return "download";
    }

    @Override
    public String getDescription() {
        return  "§fЗагружает файл c музыкой с указанного URL.";
    }

    @Override
    public String getSyntax() {
        return "§3/cd download <ссылка> <имя.расширение>";
    }

    @Override
    public void perform(Player player, String[] args) {
        // /cd   download    url   filename
        //         [0]       [1]     [2]

        if (!player.hasPermission("customdiscs.download")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав на выполнение этой команды");
            return;
        }

        if (args.length<3) {
            player.sendMessage("§fНеверные аргументы! (§3/cd download <ссылка> <имя.расширение>§f)");
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(customDiscs, () -> {
            try {
                URL fileURL = new URL(args[1]);
                String filename = args[2];
                if (filename.contains("../")) {
                    player.sendMessage(ChatColor.RED + "Неверное имя файла!");
                    return;
                }

                if (!getFileExtension(filename).equals("wav") && !getFileExtension(filename).equals("mp3") && !getFileExtension(filename).equals("flac")) {
                    player.sendMessage("§fФайл должен иметь расширение §3wav§f, §3flac §fили §3mp3§f!");
                    return;
                }

                player.sendMessage(ChatColor.GRAY + "Загрузка файла...");
                Path downloadPath = Path.of(customDiscs.getDataFolder().getPath(), "musicdata", filename);
                File downloadFile = new File(downloadPath.toUri());

                URLConnection connection = fileURL.openConnection();

                if (connection != null) {
                    long size = connection.getContentLengthLong() / 1048576;
                    if (size > customDiscs.getConfig().getInt("max-download-size", 50)) {
                        player.sendMessage(ChatColor.RED + "Размер файла превышает " + customDiscs.getConfig().getInt("max-download-size", 50) + "MB.");
                        return;
                    }
                }

                FileUtils.copyURLToFile(fileURL, downloadFile);

                player.sendMessage("§7Файл успешно загружен");
                player.sendMessage("§fСоздайте диск, выполнив команду §3/cd create "+filename+" \"Название - описание\"");
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "При загрузке произошла ошибка.");
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
