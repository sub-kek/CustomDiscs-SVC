package io.github.subkek.customdiscs.command.subcommand;

import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.AbstractSubCommand;
import org.apache.commons.io.FileUtils;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.logging.Level;

public class DownloadSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public DownloadSubCommand() {
    super("download");

    this.withFullDescription(getDescription());
    this.withUsage(getSyntax());

    this.withArguments(new StringArgument("url"));
    this.withArguments(new StringArgument("filename"));

    this.executes(this::execute);
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

  public void execute(CommandSender sender, CommandArguments arguments) {
    if (!hasPermission(sender)) {
      CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("no-permission-error"));
      return;
    }

    plugin.getFoliaLib().getScheduler().runAsync(task -> {
      try {
        URL fileURL = new URL(getArgumentValue(arguments, "url", String.class));
        String filename = getArgumentValue(arguments, "filename", String.class);

        if (filename.contains("../")) {
          CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("invalid-file-name"));
          return;
        }

        if (!getFileExtension(filename).equals("wav") && !getFileExtension(filename).equals("mp3") &&
            !getFileExtension(filename).equals("flac")) {
          CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("unknown-extension-error"));
          return;
        }

        CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("downloading"));
        Path downloadPath = Path.of(plugin.getDataFolder().getPath(), "musicdata", filename);
        File downloadFile = new File(downloadPath.toUri());

        URLConnection connection = fileURL.openConnection();

        if (connection != null) {
          long size = connection.getContentLengthLong() / 1048576;
          if (size > plugin.getCDConfig().getMaxDownloadSize()) {
            CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("download-file-large-error",
                String.valueOf(plugin.getCDConfig().getMaxDownloadSize())));
            return;
          }
        }

        FileUtils.copyURLToFile(fileURL, downloadFile);

        CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("download-successful"));
        CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("create-disc-tooltip",
            plugin.getLanguage().string("create-command-syntax")));
      } catch (Throwable e) {
        CustomDiscs.error("Error while download music: ", e);
        CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("download-error"));
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
