package io.github.subkek.customdiscs.command.SubCommands;

import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.SubCommand;
import io.github.subkek.customdiscs.config.CustomDiscsConfiguration;
import io.github.subkek.customdiscs.utils.Formatter;
import org.bukkit.command.CommandSender;
import org.codehaus.plexus.util.FileUtils;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;

public class DownloadCommand implements SubCommand {
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
      sender.sendMessage(Formatter.format(plugin.language.get("no-permission-error"), true));
      return;
    }

    if (!canPerform(sender)) {
      sender.sendMessage(Formatter.format(plugin.language.get("cant-perform-command-error"), true));
      return;
    }

    if (args.length < 3) {
      sender.sendMessage(Formatter.format(plugin.language.get("unknown-arguments-error"), true, plugin.language.get("download-command-syntax")));
      return;
    }

    Runnable downloadTask = () -> {
      try {
        URL fileURL = new URL(args[1]);
        String filename = args[2];
        if (filename.contains("../")) {
          sender.sendMessage(Formatter.format(plugin.language.get("invalid-file-name"), true));
          return;
        }

        if (!getFileExtension(filename).equals("wav") && !getFileExtension(filename).equals("mp3") && !getFileExtension(filename).equals("flac")) {
          sender.sendMessage(Formatter.format(plugin.language.get("unknown-extension-error"), true));
          return;
        }

        sender.sendMessage(Formatter.format(plugin.language.get("downloading"), true));
        Path downloadPath = Path.of(plugin.getDataFolder().getPath(), "musicdata", filename);
        File downloadFile = new File(downloadPath.toUri());

        URLConnection connection = fileURL.openConnection();

        if (connection != null) {
          long size = connection.getContentLengthLong() / 1048576;
          if (size > CustomDiscsConfiguration.maxDownloadSize) {
            sender.sendMessage(Formatter.format(plugin.language.get("download-file-large-error"), true, String.valueOf(CustomDiscsConfiguration.maxDownloadSize)));
            return;
          }
        }

        FileUtils.copyURLToFile(fileURL, downloadFile);

        sender.sendMessage(Formatter.format(plugin.language.get("download-successful"), true));
        sender.sendMessage(Formatter.format(plugin.language.get("create-disc-tooltip"), true, plugin.language.get("create-command-syntax")));
      } catch (Exception e) {
        sender.sendMessage(Formatter.format(plugin.language.get("download-error"), true));
      }
    };

    if (plugin.isFolia()) {plugin.getServer().getAsyncScheduler().runNow(plugin, t -> downloadTask.run());}
    else {plugin.getServer().getScheduler().runTaskAsynchronously(plugin, downloadTask);}
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
