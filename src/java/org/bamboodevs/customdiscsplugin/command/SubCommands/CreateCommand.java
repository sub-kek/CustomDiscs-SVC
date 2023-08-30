package org.bamboodevs.customdiscsplugin.command.SubCommands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bamboodevs.customdiscsplugin.CustomDiscs;
import org.bamboodevs.customdiscsplugin.command.SubCommand;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CreateCommand extends SubCommand {

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "§fСоздает диск с музыкой.";
    }

    @Override
    public String getSyntax() {
        return  "§3/cd create <Имя файла> \"Название - описание\"";
    }

    @Override
    public void perform(Player player, String[] args) {
        if (isMusicDisc(player)) {
            if (args.length >= 3) {

                if (!player.hasPermission("customdiscs.create")) {
                    player.sendMessage(ChatColor.RED + "У вас нет прав на использовать эту команду!");
                    return;
                }

                // /cd create test.mp3 "test"
                //      [0]     [1]     [2]
                //Find file, if file not there then say "file not there"
                String songname = "";
                String filename = args[1];
                if (filename.contains("../")) {
                    player.sendMessage(ChatColor.RED + "Неверное имя файла!");
                    return;
                }

                if (customName(readQuotes(args)).equalsIgnoreCase("")) {
                    player.sendMessage("§fНеобходимо указать имя диска.");
                    return;
                }

                File getDirectory = new File(CustomDiscs.getInstance().getDataFolder(), "musicdata");
                File songFile = new File(getDirectory.getPath(), filename);
                if (songFile.exists()) {
                    if (getFileExtension(filename).equals("wav") || getFileExtension(filename).equals("mp3") || getFileExtension(filename).equals("flac")) {
                        songname = args[1];
                    } else {
                        player.sendMessage("§fФайл может быть только в формате §3wav§f, §3flac §fили §3mp3");
                        return;
                    }
                } else {
                    player.sendMessage("§fФайл не найден!");
                    return;
                }

                //Sets the lore of the item to the quotes from the command.
                ItemStack disc = new ItemStack(player.getInventory().getItemInMainHand());

                if (isBurned(disc)) {
                    player.sendMessage("§fНа записанный или необработанный очищеный диск нельзя записать музыку");
                    return;
                }

                ItemMeta meta = disc.getItemMeta();

                meta.setDisplayName("§rМузыкальная пластинка");

                @Nullable List<Component> itemLore = new ArrayList<>();
                final TextComponent customLoreSong = Component.text()
                        .decoration(TextDecoration.ITALIC, false)
                        .content(customName(readQuotes(args)))
                        .color(NamedTextColor.GRAY)
                        .build();
                itemLore.add(customLoreSong);
                meta.addItemFlags(ItemFlag.values());
                meta.lore(itemLore);

                PersistentDataContainer data = meta.getPersistentDataContainer();
                NamespacedKey discYtMeta = new NamespacedKey(CustomDiscs.getInstance(), "customdiscyt");
                if (data.has(discYtMeta, PersistentDataType.STRING))
                    data.remove(new NamespacedKey(CustomDiscs.getInstance(), "customdiscyt"));
                data.set(new NamespacedKey(CustomDiscs.getInstance(), "customdisc"), PersistentDataType.STRING, filename);

                disc.addItemFlags();

                player.getInventory().getItemInMainHand().setItemMeta(meta);

                player.sendMessage("§fИмя файла: §3" + songname);
                player.sendMessage("§fИмя диска: §3" + customName(readQuotes(args)));

            } else {
                player.sendMessage("§fНедостаточно аргументов! (§3/cd create <Имя файла> \"Название - описание\"§f)");
            }
        } else {
            player.sendMessage("§fСначала возьмите диск в руку!");
        }
    }

    private String getFileExtension(String s) {
        int index = s.lastIndexOf(".");
        if (index > 0) {
            return s.substring(index + 1);
        } else {
            return "";
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

        return p.getInventory().getItemInMainHand().getType().equals(Material.MUSIC_DISC_13) ||
                p.getInventory().getItemInMainHand().getType().equals(Material.MUSIC_DISC_CAT) ||
                p.getInventory().getItemInMainHand().getType().equals(Material.MUSIC_DISC_BLOCKS) ||
                p.getInventory().getItemInMainHand().getType().equals(Material.MUSIC_DISC_CHIRP) ||
                p.getInventory().getItemInMainHand().getType().equals(Material.MUSIC_DISC_FAR) ||
                p.getInventory().getItemInMainHand().getType().equals(Material.MUSIC_DISC_MALL) ||
                p.getInventory().getItemInMainHand().getType().equals(Material.MUSIC_DISC_MELLOHI) ||
                p.getInventory().getItemInMainHand().getType().equals(Material.MUSIC_DISC_STAL) ||
                p.getInventory().getItemInMainHand().getType().equals(Material.MUSIC_DISC_STRAD) ||
                p.getInventory().getItemInMainHand().getType().equals(Material.MUSIC_DISC_WARD) ||
                p.getInventory().getItemInMainHand().getType().equals(Material.MUSIC_DISC_11) ||
                p.getInventory().getItemInMainHand().getType().equals(Material.MUSIC_DISC_WAIT) ||
                p.getInventory().getItemInMainHand().getType().equals(Material.MUSIC_DISC_PIGSTEP);
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
