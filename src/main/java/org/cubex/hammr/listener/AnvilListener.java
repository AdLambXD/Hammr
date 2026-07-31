package org.cubex.hammr.listener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataType;
import org.cubex.hammr.HammrEnhance;
import org.cubex.hammr.config.MessageProvider;
import org.cubex.hammr.enhancement.EnhanceManager;
import org.cubex.hammr.enhancement.EnhanceResult;
import org.cubex.hammr.storage.EnhanceData;
import org.cubex.hammr.storage.PDCAdapter;
import org.cubex.hammr.util.ItemChecker;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnvilListener implements Listener {

    private static final NamespacedKey PREVIEW_KEY = new NamespacedKey(HammrEnhance.getInstance(), "preview");

    private MessageProvider msg() {
        return HammrEnhance.getInstance().getMessages();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);

        if (!ItemChecker.isNetheriteEquipment(left)) return;

        if (right == null || right.getType().isAir()) return;

        if (!ItemChecker.isDiamond(right) && !ItemChecker.isNetheriteIngot(right)) {
            event.setResult(null);
            return;
        }

        boolean hasDiamond = ItemChecker.isDiamond(right);
        boolean hasIngot = ItemChecker.isNetheriteIngot(right);

        ItemMeta meta = left.getItemMeta();
        if (meta == null) return;
        EnhanceData data = PDCAdapter.readData(meta);

        if (!isMainEnhancement(data, hasDiamond, hasIngot)
                && !isBranchEnhancement(left, data, hasDiamond, hasIngot)) {
            event.setResult(null);
            return;
        }

        boolean isMain = isMainEnhancement(data, hasDiamond, hasIngot);
        ItemStack preview = left.clone();
        ItemMeta previewMeta = preview.getItemMeta();

        int cost = HammrEnhance.getInstance().getSettings().getCostGold();
        String materialName = msg().get(hasIngot ? "material-name.NETHERITE_INGOT" : "material-name.DIAMOND");

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(msg().get("preview.separator"), NamedTextColor.DARK_GRAY));
        lore.add(Component.text(msg().get(isMain ? "preview.title-main" : "preview.title-branch"), NamedTextColor.GOLD, TextDecoration.BOLD));
        lore.add(Component.text(msg().get("preview.cost", materialName, cost), NamedTextColor.GREEN));
        lore.add(Component.text(msg().get("preview.instruction"), NamedTextColor.GRAY));

        previewMeta.lore(lore);
        previewMeta.getPersistentDataContainer().set(PREVIEW_KEY, PersistentDataType.BOOLEAN, true);
        preview.setItemMeta(previewMeta);
        event.setResult(preview);

        if (event.getView() instanceof AnvilView view) {
            view.setRepairCost(0);
            view.setRepairItemCountCost(1);
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

        // Cancel stale preview clicks
        if (isHammrPreview(current) && !isValidEnhancementContext(left, right)) {
            event.setCancelled(true);
            return;
        }

        if (!ItemChecker.isNetheriteEquipment(left)) return;
        if (right == null || right.getType().isAir()) return;

        boolean hasDiamond = ItemChecker.isDiamond(right);
        boolean hasIngot = ItemChecker.isNetheriteIngot(right);
        if (!hasDiamond && !hasIngot) return;

        ItemMeta meta = left.getItemMeta();
        if (meta == null) return;
        EnhanceData data = PDCAdapter.readData(meta);

        if (!isMainEnhancement(data, hasDiamond, hasIngot) &&
            !isBranchEnhancement(left, data, hasDiamond, hasIngot)) return;

        event.setCancelled(true);

        EnhanceResult result;
        if (isBranchEnhancement(left, data, hasDiamond, hasIngot)) {
            result = EnhanceManager.performBranchEnhance(player, left, hasDiamond);
        } else {
            result = EnhanceManager.performMainEnhance(player, left, hasDiamond, hasIngot);
        }

        if (result.message() != null) {
            player.sendMessage(Component.text(result.message(), NamedTextColor.RED));
            return;
        }

        Location anvilLoc = inv.getLocation();
        ItemStack resultItem = result.enhancedItem() != null ? result.enhancedItem() : left;

        if (result.exploded() && anvilLoc != null) {
            player.closeInventory();
            if (resultItem != null) {
                anvilLoc.getWorld().dropItemNaturally(anvilLoc, resultItem);
            }
            destroyAnvil(anvilLoc);
            return;
        }

        // Consume materials
        inv.setItem(0, null);
        if (right.getAmount() > 1) {
            right.setAmount(right.getAmount() - 1);
            inv.setItem(1, right);
        } else {
            inv.setItem(1, null);
        }

        // Give result to player
        boolean shift = event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT;
        if (shift) {
            Map<Integer, ItemStack> remaining = player.getInventory().addItem(resultItem);
            if (!remaining.isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), resultItem);
            }
        } else {
            player.setItemOnCursor(resultItem);
        }

        EnhanceManager.syncInventory(player);
    }

    private boolean isMainEnhancement(EnhanceData data,
                                       boolean hasDiamond, boolean hasIngot) {
        if (data.isMainMaxed()) return false;
        int threshold = HammrEnhance.getInstance().getSettings().getMainMaterialThreshold();
        if (data.mainLevel() < threshold && hasDiamond) return true;
        return data.mainLevel() >= threshold && hasIngot;
    }

    private boolean isBranchEnhancement(ItemStack item, EnhanceData data,
                                          boolean hasDiamond, boolean hasIngot) {
        if (!hasDiamond) return false;
        if (!ItemChecker.hasBranchPool(item)) return false;
        return data.canBranch();
    }

    private boolean isHammrPreview(ItemStack item) {
        return item.hasItemMeta()
                && item.getItemMeta().getPersistentDataContainer().has(PREVIEW_KEY, PersistentDataType.BOOLEAN);
    }

    private boolean isValidEnhancementContext(ItemStack left, ItemStack right) {
        if (!ItemChecker.isNetheriteEquipment(left)) return false;
        if (right == null || right.getType().isAir()) return false;
        boolean hasDiamond = ItemChecker.isDiamond(right);
        boolean hasIngot = ItemChecker.isNetheriteIngot(right);
        if (!hasDiamond && !hasIngot) return false;
        ItemMeta meta = left.getItemMeta();
        if (meta == null) return false;
        EnhanceData data = PDCAdapter.readData(meta);
        return isMainEnhancement(data, hasDiamond, hasIngot)
                || isBranchEnhancement(left, data, hasDiamond, hasIngot);
    }

    private void destroyAnvil(Location loc) {
        Block block = loc.getBlock();
        if (block.getType().name().contains("ANVIL")) {
            block.getWorld().createExplosion(loc, 1.0f, false, false);
            block.setType(Material.AIR);
        }
    }
}
