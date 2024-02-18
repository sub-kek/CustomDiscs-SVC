package io.github.subkek.customdiscs.command.SubCommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import io.github.subkek.customdiscs.CustomDiscs;
import io.github.subkek.customdiscs.utils.Formatter;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CreateYtCommand extends io.github.subkek.customdiscs.command.SubCommand {
    private final CustomDiscs plugin = CustomDiscs.getInstance();

    @Override
    public String getName() {
        return "createyt";
    }

    @Override
    public String getDescription() {
        return plugin.language.get("createyt-command-description");
    }

    @Override
    public String getSyntax() {
        return plugin.language.get("createyt-command-syntax");
    }

    @Override
    public void perform(Player player, String[] args) {
        if (isMusicDisc(player)) {
            if (args.length >= 3) {

                if (!player.hasPermission("customdiscs.createyt")) {
                    player.sendMessage(Formatter.format(plugin.language.get("no-permission-error"), true));
                    return;
                }

                // /cd create test.mp3 "test"
                //      [0]     [1]     [2]
                //Find file, if file not there then say "file not there"

                if (customName(readQuotes(args)).equalsIgnoreCase("")) {
                    player.sendMessage(Formatter.format(plugin.language.get("write-disc-name-error"), true));
                    return;
                }

                //Sets the lore of the item to the quotes from the command.
                ItemStack disc = new ItemStack(player.getInventory().getItemInMainHand());

                if (isBurned(disc) && plugin.config.getDiscCleaning()) {
                    player.sendMessage(Formatter.format(plugin.language.get("disc-already-burned-error"), true));
                    return;
                }

                ItemMeta meta = disc.getItemMeta();

                meta.setDisplayName(plugin.language.get("youtube-disc"));

                @Nullable List<Component> itemLore = new ArrayList<>();
                final TextComponent customLoreSong = Component.text()
                        .decoration(TextDecoration.ITALIC, false)
                        .content(customName(readQuotes(args)))
                        .color(NamedTextColor.GRAY)
                        .build();
                itemLore.add(customLoreSong);
                meta.addItemFlags(ItemFlag.values());
                meta.lore(itemLore);

                String youtubeUrl = args[1];

                PersistentDataContainer data = meta.getPersistentDataContainer();
                NamespacedKey discMeta = new NamespacedKey(CustomDiscs.getInstance(), "customdisc");
                if (data.has(discMeta, PersistentDataType.STRING))
                    data.remove(new NamespacedKey(CustomDiscs.getInstance(), "customdisc"));
                data.set(new NamespacedKey(CustomDiscs.getInstance(), "customdiscyt"), PersistentDataType.STRING, youtubeUrl);

                player.getInventory().getItemInMainHand().setItemMeta(meta);

                player.sendMessage(Formatter.format(plugin.language.get("disc-youtube-link"), youtubeUrl));
                player.sendMessage(Formatter.format(plugin.language.get("disc-name-output"), customName(readQuotes(args))));
            } else {
                player.sendMessage(Formatter.format(plugin.language.get("unknown-arguments-error"), true, plugin.language.get("createyt-command-syntax")));
            }
        } else {
            player.sendMessage(Formatter.format(plugin.language.get("disc-not-in-hand-error"), true));
        }
    }

    private ArrayList<String> readQuotes(String[] args) {
        ArrayList<String> quotes = new ArrayList<>();
        String temp = "";
        boolean inQuotes = false;

        for (String s : args) {
            if (s.startsWith("\"") && s.endsWith("\"")) {
                temp += s.substring(1, s.length()-1);
                quotes.add(temp);
            } else if (s.startsWith("\"")) {
                temp += s.substring(1);
                quotes.add(temp);
                inQuotes = true;
            } else if (s.endsWith("\"")) {
                temp += s.substring(0, s.length()-1);
                quotes.add(temp);
                inQuotes = false;
            } else if (inQuotes) {
                temp += s;
                quotes.add(temp);
            }
            temp = "";
        }

        return quotes;
    }

    private String customName(ArrayList<String> q) {

        StringBuffer sb = new StringBuffer();

        for (String s : q) {
            sb.append(s);
            sb.append(" ");
        }

        if (sb.isEmpty()) {
            return sb.toString();
        } else {
            return sb.toString().substring(0, sb.length()-1);
        }
    }

    private boolean isMusicDisc(Player p) {

        return p.getInventory().getItemInMainHand().getType().toString().contains("MUSIC_DISC");
    }

    private boolean isBurned(ItemStack item) {
        if (!item.hasItemMeta()) return false;

        ItemMeta itemMeta = item.getItemMeta();
        PersistentDataContainer data = itemMeta.getPersistentDataContainer();

        if (data.has(new NamespacedKey(CustomDiscs.getInstance(), "cleared"), PersistentDataType.STRING)) {
            return  data.get(new NamespacedKey(CustomDiscs.getInstance(), "cleared"), PersistentDataType.STRING).equals("true") ||
                    data.has(new NamespacedKey(CustomDiscs.getInstance(), "customdisc"), PersistentDataType.STRING) ||
                    data.has(new NamespacedKey(CustomDiscs.getInstance(), "customdiscyt"), PersistentDataType.STRING);
        }

        return  data.has(new NamespacedKey(CustomDiscs.getInstance(), "customdisc"), PersistentDataType.STRING) ||
                data.has(new NamespacedKey(CustomDiscs.getInstance(), "customdiscyt"), PersistentDataType.STRING);
    }
}
