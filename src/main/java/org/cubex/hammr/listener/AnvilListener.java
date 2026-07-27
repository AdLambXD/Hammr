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

    @EventHandler(priority = EventPriority.HIGH)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
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

        String type;
        if (isMainEnhancement(left, data, hasDiamond, hasIngot)) {
            type = "主强化";
        } else if (isBranchEnhancement(left, data, hasDiamond, hasIngot)) {
            type = "分支强化";
        } else {
            return;
        }

        ItemStack preview = left.clone();
        ItemMeta previewMeta = preview.getItemMeta();

        List<Component> lore = previewMeta.hasLore()
                ? new ArrayList<>(previewMeta.lore())
                : new ArrayList<>();

        if (!lore.isEmpty()) {
            lore.add(Component.empty());
        }
        lore.add(Component.text("━━━━━━━━━━━━━━", NamedTextColor.DARK_GRAY));
        lore.add(Component.text("◆ " + type, NamedTextColor.GOLD, TextDecoration.BOLD));
        lore.add(Component.text("消耗: 1x " + (hasDiamond ? "钻石" : "下界合金锭")
                + " + " + GOLD_COST + " 金币", NamedTextColor.GREEN));
        lore.add(Component.text("点击取出以执行强化", NamedTextColor.GRAY));

        previewMeta.lore(lore);
        preview.setItemMeta(previewMeta);
        event.setResult(preview);

        // Set XP cost so the output slot stays active
        setRepairCost(event, 1);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onAnvilClick(InventoryClickEvent event) {
        if (event.getRawSlot() != 2) return;
        if (!(event.getInventory() instanceof AnvilInventory inv)) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType().isAir()) return;

        event.setCancelled(true);

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

        EnhanceResult result;
        if (isBranchEnhancement(left, data, hasDiamond, hasIngot)) {
            result = enhanceManager.performBranchEnhance(player, left, hasDiamond);
        } else if (isMainEnhancement(left, data, hasDiamond, hasIngot)) {
            result = enhanceManager.performMainEnhance(player, left, hasDiamond, hasIngot);
        } else {
            player.sendMessage(Component.text("无法进行强化！", NamedTextColor.RED));
            return;
        }

        if (result.message() != null) {
            player.sendMessage(Component.text(result.message(), NamedTextColor.RED));
            return;
        }

        consumeMaterial(inv, hasDiamond, hasIngot);

        Location anvilLoc = inv.getLocation();
        ItemStack resultItem = result.enhancedItem() != null
                ? result.enhancedItem() : left;

        // Clear all slots
        inv.setItem(0, null);
        inv.setItem(1, null);
        inv.setItem(2, null);

        player.closeInventory();

        if (result.exploded() && anvilLoc != null) {
            // Drop materials still in the anvil
            if (resultItem != null) {
                anvilLoc.getWorld().dropItemNaturally(anvilLoc, resultItem);
            }
            destroyAnvil(anvilLoc);
        } else {
            // Give to player
            if (resultItem != null) {
                var leftover = player.getInventory().addItem(resultItem);
                if (!leftover.isEmpty()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover.get(0));
                }
            }
        }

        damageAnvil(anvilLoc);
    }

    private void setRepairCost(PrepareAnvilEvent event, int cost) {
        try {
            AnvilInventory inv = event.getInventory();
            inv.getClass().getMethod("setRepairCost", int.class).invoke(inv, cost);
        } catch (Exception ignored) {
        }
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

    private void consumeMaterial(AnvilInventory inv, boolean hasDiamond, boolean hasIngot) {
        ItemStack right = inv.getItem(1);
        if (right == null) return;

        int amount = right.getAmount() - 1;
        if (amount <= 0) {
            inv.setItem(1, null);
        } else {
            right.setAmount(amount);
        }
    }

    private void destroyAnvil(Location loc) {
        Block block = loc.getBlock();
        if (block.getType().name().contains("ANVIL")) {
            block.getWorld().createExplosion(loc, 1.0f, false, false);
            block.setType(Material.AIR);
            loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
        }
    }

    private void damageAnvil(Location loc) {
        if (loc == null) return;
        // Don't damage if already destroyed by explosion
        Block block = loc.getBlock();
        if (block.getType() == Material.AIR) return;

        Material next = switch (block.getType()) {
            case ANVIL -> Material.CHIPPED_ANVIL;
            case CHIPPED_ANVIL -> Material.DAMAGED_ANVIL;
            default -> null;
        };

        if (next != null) {
            block.setType(next);
        } else if (block.getType() == Material.DAMAGED_ANVIL) {
            block.setType(Material.AIR);
            loc.getWorld().playSound(loc, org.bukkit.Sound.BLOCK_ANVIL_BREAK, 1.0f, 1.0f);
        }
    }
}
