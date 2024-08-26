package io.github.subkek.customdiscs.command.subcommand;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.SubCommand;
import io.github.subkek.customdiscs.config.CustomDiscsConfiguration;
import org.apache.commons.io.FileUtils;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.logging.Level;

public class DownloadCommand implements SubCommand {
  private final CustomDiscs plugin = CustomDiscs.getInstance();

  @Override
  public String getName() {
    return "download";
  }

  @Override
  public String getDescription() {
    return plugin.getLanguage().string("download-command-description");
  }

  @Override
  public String getSyntax() {
    return plugin.getLanguage().string("download-command-syntax");
  }

  @Override
  public boolean hasPermission(CommandSender sender) {
    return sender.hasPermission("customdiscs.download");
  }

  @Override
  public boolean canPerform(CommandSender sender) {
    return true;
  }

  @Override
  public void perform(CommandSender sender, String[] args) {
    if (!hasPermission(sender)) {
      plugin.sendMessage(sender, plugin.getLanguage().PComponent("no-permission-error"));
      return;
    }

    if (!canPerform(sender)) {
      plugin.sendMessage(sender, plugin.getLanguage().PComponent("cant-perform-command-error"));
      return;
    }

    if (args.length < 3) {
      plugin.sendMessage(sender, plugin.getLanguage().PComponent("unknown-arguments-error", plugin.getLanguage().string("download-command-syntax")));
      return;
    }

    plugin.getFoliaLib().getScheduler().runAsync(task -> {
      try {
        URL fileURL = new URL(args[1]);
        String filename = args[2];
        if (filename.contains("../")) {
          plugin.sendMessage(sender, plugin.getLanguage().PComponent("invalid-file-name"));
          return;
        }

        if (!getFileExtension(filename).equals("wav") && !getFileExtension(filename).equals("mp3") && !getFileExtension(filename).equals("flac")) {
          plugin.sendMessage(sender, plugin.getLanguage().PComponent("unknown-extension-error"));
          return;
        }

        plugin.sendMessage(sender, plugin.getLanguage().PComponent("downloading"));
        Path downloadPath = Path.of(plugin.getDataFolder().getPath(), "musicdata", filename);
        File downloadFile = new File(downloadPath.toUri());

        URLConnection connection = fileURL.openConnection();

        if (connection != null) {
          long size = connection.getContentLengthLong() / 1048576;
          if (size > CustomDiscsConfiguration.maxDownloadSize) {
            plugin.sendMessage(sender, plugin.getLanguage().PComponent("download-file-large-error", String.valueOf(CustomDiscsConfiguration.maxDownloadSize)));
            return;
          }
        }

        FileUtils.copyURLToFile(fileURL, downloadFile);

        plugin.sendMessage(sender, plugin.getLanguage().PComponent("download-successful"));
        plugin.sendMessage(sender, plugin.getLanguage().PComponent("create-disc-tooltip", plugin.getLanguage().string("create-command-syntax")));
      } catch (Throwable e) {
        plugin.getLogger().log(Level.SEVERE, "Error while download music: ", e);
        plugin.sendMessage(sender, plugin.getLanguage().PComponent("download-error"));
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
