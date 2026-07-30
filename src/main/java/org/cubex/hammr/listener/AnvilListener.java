package org.cubex.hammr.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.cubex.hammr.enhancement.EnhanceManager;
import org.cubex.hammr.enhancement.EnhanceResult;
import org.cubex.hammr.storage.EnhanceData;
import org.cubex.hammr.storage.PDCAdapter;
import org.cubex.hammr.util.ItemChecker;
import java.util.ArrayList;
import java.util.List;

public class AnvilListener implements Listener {

    private static final int GOLD_COST = 1000;
    private final EnhanceManager enhanceManager = new EnhanceManager();

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);

        if (!ItemChecker.isNetheriteEquipment(left)) return;

        if (right == null || right.getType().isAir()
                || (!ItemChecker.isDiamond(right) && !ItemChecker.isNetheriteIngot(right))) {
            event.setResult(null);
            return;
        }

        boolean hasDiamond = ItemChecker.isDiamond(right);
        boolean hasIngot = ItemChecker.isNetheriteIngot(right);

        ItemMeta meta = left.getItemMeta();
        if (meta == null) return;
        EnhanceData data = PDCAdapter.readData(meta);

        if (!isMainEnhancement(left, data, hasDiamond, hasIngot)
                && !isBranchEnhancement(left, data, hasDiamond, hasIngot)) {
            event.setResult(null);
            return;
        }

        boolean isMain = isMainEnhancement(left, data, hasDiamond, hasIngot);
        ItemStack preview = left.clone();
        ItemMeta previewMeta = preview.getItemMeta();

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("◆ " + (isMain ? "主强化" : "分支强化"), NamedTextColor.GOLD, TextDecoration.BOLD));
        lore.add(Component.text("消耗: " + (hasDiamond ? "钻石" : "下界合金锭")
                + " x1 + " + GOLD_COST + " 金币", NamedTextColor.GREEN));
        lore.add(Component.text("点击取出以执行", NamedTextColor.GRAY));

        previewMeta.lore(lore);
        preview.setItemMeta(previewMeta);
        event.setResult(preview);

        if (event.getView() instanceof AnvilView view) {
            view.setRepairCost(0);
            view.setRepairItemCountCost(0);
            view.bypassEnchantmentLevelRestriction(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvilClick(InventoryClickEvent event) {
        if (event.getRawSlot() != 2) return;
        if (!(event.getInventory() instanceof AnvilInventory inv)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir()) return;

        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);

        if (!ItemChecker.isNetheriteEquipment(left)) return;
        if (right == null || right.getType().isAir()) return;

        boolean hasDiamond = ItemChecker.isDiamond(right);
        boolean hasIngot = ItemChecker.isNetheriteIngot(right);
        if (!hasDiamond && !hasIngot) return;

        ItemMeta meta = left.getItemMeta();
        if (meta == null) return;
        EnhanceData data = PDCAdapter.readData(meta);

        if (!isMainEnhancement(left, data, hasDiamond, hasIngot) &&
            !isBranchEnhancement(left, data, hasDiamond, hasIngot)) return;

        EnhanceResult result;
        if (isBranchEnhancement(left, data, hasDiamond, hasIngot)) {
            result = enhanceManager.performBranchEnhance(player, left, hasDiamond);
        } else {
            result = enhanceManager.performMainEnhance(player, left, hasDiamond, hasIngot);
        }

        if (result.message() != null) {
            event.setCancelled(true);
            player.sendMessage(Component.text(result.message(), NamedTextColor.RED));
            return;
        }

        Location anvilLoc = inv.getLocation();
        ItemStack resultItem = result.enhancedItem() != null ? result.enhancedItem() : left;

        if (result.exploded() && anvilLoc != null) {
            event.setCancelled(true);
            player.closeInventory();
            if (resultItem != null) {
                anvilLoc.getWorld().dropItemNaturally(anvilLoc, resultItem);
            }
            destroyAnvil(anvilLoc);
            return;
        }

        event.setCurrentItem(resultItem);
        EnhanceManager.syncInventory(player);
    }

    private boolean isMainEnhancement(ItemStack item, EnhanceData data,
                                       boolean hasDiamond, boolean hasIngot) {
        if (data.isMainMaxed()) return false;
        if (data.mainLevel() < 6 && hasDiamond) return true;
        return data.mainLevel() >= 6 && hasIngot;
    }

    private boolean isBranchEnhancement(ItemStack item, EnhanceData data,
                                         boolean hasDiamond, boolean hasIngot) {
        if (!hasDiamond) return false;
        if (!ItemChecker.hasBranchPool(item)) return false;
        return data.canBranch();
    }

    private void destroyAnvil(Location loc) {
        Block block = loc.getBlock();
        if (block.getType().name().contains("ANVIL")) {
            block.getWorld().createExplosion(loc, 1.0f, false, false);
            block.setType(Material.AIR);
        }
    }
}
