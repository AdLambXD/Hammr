package org.cubex.hammr.listener;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.cubex.hammr.enhancement.EnhanceManager;

public class XpListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        int amount = event.getAmount();
        if (amount <= 0) return;

        ItemStack main = player.getInventory().getItemInMainHand();
        if (main.getType() != Material.AIR) {
            EnhanceManager.addItemXp(main, amount);
        }

        ItemStack off = player.getInventory().getItemInOffHand();
        if (off.getType() != Material.AIR) {
            EnhanceManager.addItemXp(off, amount);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) return;

        Material type = item.getType();
        boolean isTool = type.name().contains("_PICKAXE") || type.name().contains("_AXE")
                || type.name().contains("_SHOVEL") || type.name().contains("_HOE");
        if (!isTool) return;

        EnhanceManager.addItemXp(item, 1);
    }
}
