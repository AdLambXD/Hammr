package org.cubex.hammr.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.cubex.hammr.HammrEnhance;
import org.cubex.hammr.enhancement.EnhanceManager;
import org.cubex.hammr.util.ItemChecker;

public class XpListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        int amount = event.getAmount();
        if (amount <= 0) return;
        int xp = HammrEnhance.getInstance().getSettings().getXpFromExpOrb();
        if (xp <= 0) return;

        addXp(player.getInventory().getItemInMainHand(), xp);
        addXp(player.getInventory().getItemInOffHand(), xp);
        for (ItemStack armor : player.getInventory().getArmorContents()) {
            addXp(armor, xp);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        int xp = HammrEnhance.getInstance().getSettings().getBlockBreakXp();
        if (xp <= 0) return;

        ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (ItemChecker.isArmor(item)) return;
        addXp(item, xp);
    }

    /**
     * 只有下界合金装备可能带强化数据。先做一次廉价的材质判断，
     * 避免每颗经验球 / 每次挖掘都为无关物品克隆一次 ItemMeta。
     */
    private void addXp(ItemStack item, int xp) {
        if (!ItemChecker.isNetheriteEquipment(item)) return;
        EnhanceManager.addItemXp(item, xp);
    }
}
