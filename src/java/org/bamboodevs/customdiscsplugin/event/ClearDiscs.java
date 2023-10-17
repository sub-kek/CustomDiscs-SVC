package org.bamboodevs.customdiscsplugin.event;

import org.bamboodevs.customdiscsplugin.CustomDiscs;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class ClearDiscs implements Listener {
    private final CustomDiscs plugin = CustomDiscs.getInstance();

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity clickedEntity = event.getRightClicked();

        if (!clickedEntity.getType().equals(EntityType.ITEM_FRAME)) return;

        ItemFrame itemFrame = (ItemFrame) clickedEntity;

        if (!isMusicDisc(itemFrame.getItem())) return;

        if (isAxe(player)) {
            ItemStack newItem = new ItemStack(itemFrame.getItem().getType());
            ItemMeta itemMeta = newItem.getItemMeta();
            itemMeta.setDisplayName(plugin.language.get("cleared-disc"));
            itemMeta.getPersistentDataContainer().set(new NamespacedKey(CustomDiscs.getInstance(), "cleared"), PersistentDataType.STRING, "true");
            newItem.setItemMeta(itemMeta);

            itemFrame.setItem(new ItemStack(Material.AIR));

            Location location = itemFrame.getLocation();
            Entity itemEntity = location.getWorld().dropItem(location, newItem);
            itemEntity.setVelocity(player.getFacing().getDirection().multiply(-0.2));

            event.setCancelled(true);
        }

        if (player.getInventory().getItemInMainHand().getType().equals(Material.GLOWSTONE_DUST)) {
            ItemStack handItemStack = player.getInventory().getItemInMainHand();
            handItemStack.setAmount(handItemStack.getAmount() - 1);
            player.getInventory().setItemInMainHand(handItemStack);

            ItemStack item = itemFrame.getItem();

            if (!item.hasItemMeta()) return;

            ItemMeta itemMeta = item.getItemMeta();
            PersistentDataContainer data = itemMeta.getPersistentDataContainer();

            if (!data.get(new NamespacedKey(CustomDiscs.getInstance(), "cleared"), PersistentDataType.STRING).equals("true")) return;

            data.set(new NamespacedKey(CustomDiscs.getInstance(), "cleared"), PersistentDataType.STRING, "false");
            itemMeta.setDisplayName(plugin.language.get("processed-disc"));
            item.setItemMeta(itemMeta);

            itemFrame.setItem(new ItemStack(Material.AIR));

            Location location = itemFrame.getLocation();
            Entity itemEntity = location.getWorld().dropItem(location, item);
            itemEntity.setVelocity(player.getFacing().getDirection().multiply(-0.2));

            event.setCancelled(true);
        }
    }

    private boolean isMusicDisc(ItemStack item) {
        return  item.getType().equals(Material.MUSIC_DISC_13) ||
                item.getType().equals(Material.MUSIC_DISC_CAT) ||
                item.getType().equals(Material.MUSIC_DISC_BLOCKS) ||
                item.getType().equals(Material.MUSIC_DISC_CHIRP) ||
                item.getType().equals(Material.MUSIC_DISC_FAR) ||
                item.getType().equals(Material.MUSIC_DISC_MALL) ||
                item.getType().equals(Material.MUSIC_DISC_MELLOHI) ||
                item.getType().equals(Material.MUSIC_DISC_STAL) ||
                item.getType().equals(Material.MUSIC_DISC_STRAD) ||
                item.getType().equals(Material.MUSIC_DISC_WARD) ||
                item.getType().equals(Material.MUSIC_DISC_11) ||
                item.getType().equals(Material.MUSIC_DISC_WAIT) ||
                item.getType().equals(Material.MUSIC_DISC_PIGSTEP);
    }

    private boolean isAxe(Player p) {
        return  p.getInventory().getItemInMainHand().getType().equals(Material.IRON_AXE) ||
                p.getInventory().getItemInMainHand().getType().equals(Material.GOLDEN_AXE) ||
                p.getInventory().getItemInMainHand().getType().equals(Material.DIAMOND_AXE) ||
                p.getInventory().getItemInMainHand().getType().equals(Material.NETHERITE_AXE);
    }
}
