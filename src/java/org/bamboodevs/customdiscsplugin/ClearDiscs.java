package org.bamboodevs.customdiscsplugin;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Jukebox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ClearDiscs extends JavaPlugin implements Listener {

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        Block clickedBlock = event.getClickedBlock();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();

        if (action == Action.RIGHT_CLICK_BLOCK &&
                clickedBlock != null &&
                clickedBlock.getType() == Material.JUKEBOX &&
                mainHandItem.getType() == Material.DIAMOND_AXE) {

            Jukebox jukebox = (Jukebox) clickedBlock.getState();
            ItemStack record = jukebox.getRecord();

            if (record.getType().isRecord()) {
                String displayName = "Обработанная пластинка";
                record.getItemMeta().setDisplayName(displayName);
                jukebox.setRecord(record);

            }
        }
    }
}
