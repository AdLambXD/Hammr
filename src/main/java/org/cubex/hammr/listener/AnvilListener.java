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
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataType;
import org.cubex.hammr.HammrEnhance;
import org.cubex.hammr.config.MessageProvider;
import org.cubex.hammr.enhancement.BranchPool;
import org.cubex.hammr.enhancement.EnhanceManager;
import org.cubex.hammr.enhancement.EnhanceResult;
import org.cubex.hammr.storage.EnhanceData;
import org.cubex.hammr.storage.PDCAdapter;
import org.cubex.hammr.util.ItemChecker;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class AnvilListener implements Listener {

    private static final NamespacedKey PREVIEW_KEY = new NamespacedKey(HammrEnhance.getInstance(), "preview");
    public static final String PERMISSION_USE = "hammr.use";

    /** 被本插件打开过越级附魔豁免的铁砧界面；用弱引用避免界面关闭后残留 */
    private final Set<InventoryView> bypassedViews =
            Collections.newSetFromMap(new WeakHashMap<>());

    private MessageProvider msg() {
        return HammrEnhance.getInstance().getMessages();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        AnvilInventory inv = event.getInventory();
        ItemStack left = inv.getItem(0);
        ItemStack right = inv.getItem(1);

        // 越级附魔豁免标记会残留在铁砧界面上：玩家放入装备+钻石让本插件打开豁免后，
        // 再把钻石换成附魔书，就能用原版合成突破附魔等级上限。
        // 本事件在原版算完结果之后才触发，所以除了复位标记，还要作废这一次可能在
        // 残留豁免下算出的结果(本插件自己的预览会在下面重新写回)。
        if (bypassedViews.remove(event.getView()) && event.getView() instanceof AnvilView staleView) {
            staleView.bypassEnchantmentLevelRestriction(false);
            event.setResult(null);
        }

        // 无权限者连预览都不显示，铁砧保持原版行为
        if (!event.getView().getPlayer().hasPermission(PERMISSION_USE)) return;

        if (!ItemChecker.isNetheriteEquipment(left)) return;

        if (right == null || right.getType().isAir()) return;

        boolean hasDiamond = ItemChecker.isDiamond(right);
        boolean hasIngot = ItemChecker.isNetheriteIngot(right);
        if (!hasDiamond && !hasIngot) return;

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

        // 与实际扣费保持一致：配置了 cost-gold-per-level 时预览必须显示当前实际主附魔等级的价格
        int cost = HammrEnhance.getInstance().getSettings()
                .getCostGold(EnhanceManager.effectiveMainLevel(meta, left.getType(), data));
        String materialName = msg().get(hasIngot ? "material-name.NETHERITE_INGOT" : "material-name.DIAMOND");

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text(msg().get("preview.separator"), NamedTextColor.DARK_GRAY));
        lore.add(Component.text(msg().get(isMain ? "preview.title-main" : "preview.title-branch"), NamedTextColor.GOLD, TextDecoration.BOLD));
        lore.add(Component.text(msg().get("preview.cost", materialName, String.valueOf(cost)), NamedTextColor.GREEN));
        lore.add(Component.text(msg().get("preview.instruction"), NamedTextColor.GRAY));

        previewMeta.lore(lore);
        previewMeta.getPersistentDataContainer().set(PREVIEW_KEY, PersistentDataType.BOOLEAN, true);
        preview.setItemMeta(previewMeta);
        event.setResult(preview);

        if (event.getView() instanceof AnvilView view) {
            view.setRepairCost(0);
            view.setRepairItemCountCost(1);
            view.bypassEnchantmentLevelRestriction(true);
            bypassedViews.add(event.getView());
        }
    }

    /**
     * 玩家在铁砧里放入装备+材料出现本插件预览后，未点击结果槽直接关闭界面，
     * Paper 会把结果槽里的预览物品退还玩家。此时若不清掉预览就会"原件退回 + 预览
     * 退还"双份复制装备。本事件在界面关闭时清掉残留预览并复位越级附魔豁免。
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAnvilClose(InventoryCloseEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory inv)) return;
        if (!(event.getView() instanceof AnvilView view)) return;

        boolean reset = false;
        ItemStack current = inv.getItem(2);
        if (current != null && isHammrPreview(current)) {
            inv.setItem(2, null);
            reset = true;
        }
        if (bypassedViews.remove(event.getView())) {
            view.bypassEnchantmentLevelRestriction(false);
            reset = true;
        }
        if (reset && event.getPlayer() instanceof Player player) {
            player.updateInventory();
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

        // 无权限时不允许取走插件生成的预览物品
        if (!player.hasPermission(PERMISSION_USE)) {
            if (isHammrPreview(current)) {
                event.setCancelled(true);
                player.sendMessage(Component.text(msg().get("error.no-permission"), NamedTextColor.RED));
            }
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

        if (!isMainEnhancement(left, data, hasDiamond, hasIngot) &&
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
        ItemStack resultItem = result.enhancedItem() != null ? result.enhancedItem() : left.clone();

        // 必须先消耗输入槽：关闭铁砧时 Bukkit 会把输入槽里的物品退还玩家，
        // 若留到爆炸分支之后再清空就会出现"原件退回 + 成品掉落"的复制。
        inv.setItem(0, null);
        // 结果槽同样要清空：预览物品留到关闭铁砧会被退还，与成品叠加造成复制
        inv.setItem(2, null);
        if (right.getAmount() > 1) {
            right.setAmount(right.getAmount() - 1);
            inv.setItem(1, right);
        } else {
            inv.setItem(1, null);
        }

        boolean shift = event.getClick() == ClickType.SHIFT_LEFT || event.getClick() == ClickType.SHIFT_RIGHT;

        // 事件已被取消，此时同步改动背包/光标会与客户端脱同步，统一放到下一 tick 执行
        runNextTick(() -> {
            if (result.exploded() && anvilLoc != null) {
                player.closeInventory();
                destroyAnvil(anvilLoc);
                // 先爆炸再掉落，避免成品被爆炸摧毁
                anvilLoc.getWorld().dropItemNaturally(anvilLoc, resultItem);
                return;
            }
            giveResult(player, resultItem, shift);
            EnhanceManager.syncInventory(player);
            player.updateInventory();
        });
    }

    private void runNextTick(Runnable task) {
        HammrEnhance plugin = HammrEnhance.getInstance();
        plugin.getServer().getScheduler().runTask(plugin, task);
    }

    /**
     * 把成品交给玩家：光标已有物品时不覆盖(否则会吞掉玩家原有的物品)，
     * 改为放入背包，背包满则掉落在脚下。
     */
    private void giveResult(Player player, ItemStack resultItem, boolean shift) {
        // 玩家在这一 tick 内掉线时直接掉落，避免成品凭空消失
        if (!player.isOnline()) {
            player.getWorld().dropItemNaturally(player.getLocation(), resultItem);
            return;
        }

        ItemStack cursor = player.getItemOnCursor();
        boolean cursorFree = cursor == null || cursor.getType().isAir();

        if (!shift && cursorFree) {
            player.setItemOnCursor(resultItem);
            return;
        }

        Map<Integer, ItemStack> remaining = player.getInventory().addItem(resultItem);
        for (ItemStack leftover : remaining.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
    }

    private boolean isMainEnhancement(ItemStack item, EnhanceData data,
                                       boolean hasDiamond, boolean hasIngot) {
        if (data.isMainMaxed()) return false;
        // 材料门槛按实际主附魔等级判定：预附魔/合成的真实等级达标后就该用高级材料
        ItemMeta meta = item.getItemMeta();
        int effective = meta == null ? data.mainLevel()
                : EnhanceManager.effectiveMainLevel(meta, item.getType(), data);
        int threshold = HammrEnhance.getInstance().getSettings().getMainMaterialThreshold();
        if (effective < threshold && hasDiamond) return true;
        return effective >= threshold && hasIngot;
    }

    private boolean isBranchEnhancement(ItemStack item, EnhanceData data,
                                          boolean hasDiamond, boolean hasIngot) {
        if (!hasDiamond) return false;
        if (!ItemChecker.hasBranchPool(item)) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        // 满级门槛用持久值(底子快照+PDC 累计)，防止磨刀石清掉真实附魔后重置门槛；
        // 主等级门槛用实际附魔等级判定
        return data.canBranch(EnhanceManager.effectiveMainLevel(meta, item.getType(), data),
                BranchPool.effectiveTotalLevel(meta, item.getType(), data));
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
        return isMainEnhancement(left, data, hasDiamond, hasIngot)
                || isBranchEnhancement(left, data, hasDiamond, hasIngot);
    }

    private void destroyAnvil(Location loc) {
        Block block = loc.getBlock();
        if (block.getType().name().contains("ANVIL")) {
            float radius = HammrEnhance.getInstance().getSettings().getExplosionRadius();
            block.getWorld().createExplosion(loc, radius, false, false);
            block.setType(Material.AIR);
        }
    }
}
