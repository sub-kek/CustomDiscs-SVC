package io.github.subkek.customdiscs.command.subcommand;

import dev.jorel.commandapi.executors.CommandArguments;
import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.command.AbstractSubCommand;
import io.github.subkek.customdiscs.config.CustomDiscsConfiguration;
import org.bukkit.command.CommandSender;

public class ReloadSubCommand extends AbstractSubCommand {
  private final CustomDiscs plugin = CustomDiscs.getPlugin();

  public ReloadSubCommand() {
    super("help");

    this.withFullDescription(getDescription());
    this.withUsage(getSyntax());

    this.executes(this::execute);
  }

  @Override
  public String getDescription() {
    return plugin.getLanguage().string("reload-command-description");
  }

  @Override
  public String getSyntax() {
    return plugin.getLanguage().string("reload-command-syntax");
  }

  @Override
  public boolean hasPermission(CommandSender sender) {
    return sender.hasPermission("customdiscs.reload");
  }

  public void execute(CommandSender sender, CommandArguments arguments) {
    if (!hasPermission(sender)) {
      CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("no-permission-error"));
      return;
    }

    CustomDiscsConfiguration.load();
    plugin.getLanguage().init();
    CustomDiscs.sendMessage(sender, plugin.getLanguage().PComponent("config-reloaded"));
  }
}
