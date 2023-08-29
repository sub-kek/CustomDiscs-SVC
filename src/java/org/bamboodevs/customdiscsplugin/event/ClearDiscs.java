package org.bamboodevs.customdiscsplugin.event;

import org.bukkit.Location;
import org.bukkit.Material;
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

public class ClearDiscs implements Listener {
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerInteract(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity clickedEntity = event.getRightClicked();

        if (!clickedEntity.getType().equals(EntityType.ITEM_FRAME)) return;

        ItemFrame itemFrame = (ItemFrame) clickedEntity;

        if (!isMusicDisc(itemFrame.getItem())) return;
        if (!isAxe(player)) return;

        ItemStack newItem = new ItemStack(itemFrame.getItem().getType());
        ItemMeta itemMeta = newItem.getItemMeta();
        itemMeta.setDisplayName("§rОчищеная пластинка");
        newItem.setItemMeta(itemMeta);

        itemFrame.setItem(new ItemStack(Material.AIR));

        Location location = itemFrame.getLocation();

        Entity itemEntity = location.getWorld().dropItem(location, newItem);

        itemEntity.setVelocity(player.getFacing().getDirection().multiply(-0.2));

        event.setCancelled(true);
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
