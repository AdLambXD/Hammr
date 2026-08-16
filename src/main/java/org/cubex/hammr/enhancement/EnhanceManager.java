package org.cubex.hammr.enhancement;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.cubex.hammr.HammrEnhance;
import org.cubex.hammr.config.ConfigSettings;
import org.cubex.hammr.config.MessageProvider;
import org.cubex.hammr.storage.EnhanceData;
import org.cubex.hammr.storage.PDCAdapter;
import org.cubex.hammr.util.ItemChecker;
import org.cubex.hammr.util.LoreBuilder;
import org.cubex.hammr.util.RomanNumber;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class EnhanceManager {

    private EnhanceManager() {}

    private static ConfigSettings cfg() {
        return HammrEnhance.getInstance().getSettings();
    }

    private static MessageProvider msg() {
        return HammrEnhance.getInstance().getMessages();
    }

    public static EnhanceResult performMainEnhance(Player player, ItemStack item,
                                                    boolean hasDiamond, boolean hasIngot) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return EnhanceResult.error(msg().get("error.meta-null"));

        EnhanceData data = PDCAdapter.readData(meta);

        if (data.isMainMaxed()) {
            return EnhanceResult.error(msg().get("error.main-maxed"));
        }

        boolean needsIngot = data.mainLevel() >= cfg().getMainMaterialThreshold();
        if (needsIngot && !hasIngot) {
            return EnhanceResult.error(msg().get("error.need-ingot"));
        }
        if (!needsIngot && !hasDiamond) {
            return EnhanceResult.error(msg().get("error.need-diamond"));
        }

        // 先跑完全部校验再扣钱，否则任何一个后置校验失败都会把金币吞掉
        if (!hasEnoughXp(data)) {
            return EnhanceResult.error(msg().get("error.insufficient-xp", String.valueOf(data.xpRequired())));
        }

        if (!checkAndDeductGold(player, data.mainLevel())) {
            return EnhanceResult.error(msg().get("error.insufficient-gold", String.valueOf(cfg().getCostGold(data.mainLevel()))));
        }

        int rateIndex = Math.min(data.mainLevel(), cfg().getMainSuccessRates().length - 1);
        boolean success = ThreadLocalRandom.current().nextInt(100) < cfg().getMainSuccessRate(rateIndex);

        ItemStack result = item.clone();
        ItemMeta resultMeta = result.getItemMeta();

        if (success) {
            return applyMainSuccess(player, result, resultMeta, data);
        } else {
            return applyMainFailure(player, result, resultMeta, data, needsIngot ? "NETHERITE_INGOT" : "DIAMOND");
        }
    }

    private static EnhanceResult applyMainSuccess(Player player, ItemStack item,
                                                  ItemMeta meta, EnhanceData data) {
        int newLevel = data.mainLevel() + 1;

        EnhanceData newData = new EnhanceData(newLevel, data.branches(), 0);
        writeItem(meta, item.getType(), newData);
        item.setItemMeta(meta);

        if (cfg().isSoundEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        if (newLevel >= cfg().getMainMaxLevel()) {
            broadcastAnnouncement(player, item);
        }

        sendActionBar(player, msg().getComponent("actionbar.main-success", NamedTextColor.GREEN, TextDecoration.BOLD, String.valueOf(newLevel)));
        return EnhanceResult.success(item, newLevel, BranchPool.totalLevel(meta, item.getType(), newData));
    }

    private static EnhanceResult applyMainFailure(Player player, ItemStack item,
                                                  ItemMeta meta, EnhanceData data, String material) {
        // 没有等级可掉时不触发掉级，避免清空物品上原有的原版附魔
        boolean levelDown = data.hasMain()
                && ThreadLocalRandom.current().nextDouble() < cfg().getLevelDownChance();
        boolean explode = ThreadLocalRandom.current().nextDouble() < cfg().getExplosionChance();

        int newMain = data.mainLevel();

        if (levelDown) {
            newMain = data.mainLevel() - 1;
            EnhanceData newData = newMain == 0
                    ? EnhanceData.EMPTY
                    : new EnhanceData(newMain, data.branches(), data.xpPoints());
            writeItem(meta, item.getType(), newData);
        }

        item.setItemMeta(meta);
        if (cfg().isSoundEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }

        Component failMsg = msg().getComponent("actionbar.main-failure", NamedTextColor.RED);
        if (levelDown) {
            failMsg = failMsg.append(msg().getComponent("actionbar.main-level-down", NamedTextColor.RED, String.valueOf(newMain)));
        }
        if (explode) {
            failMsg = failMsg.append(msg().getComponent("actionbar.main-explode", NamedTextColor.RED));
        }
        sendActionBar(player, failMsg);

        return EnhanceResult.failure(levelDown, explode, newMain, BranchPool.totalLevel(meta, item.getType(), data), item);
    }

    public static EnhanceResult performBranchEnhance(Player player, ItemStack item, boolean hasDiamond) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return EnhanceResult.error(msg().get("error.meta-null"));

        EnhanceData data = PDCAdapter.readData(meta);

        int reqMainLevel = cfg().getBranchMinMainLevel();
        if (data.mainLevel() < reqMainLevel) {
            return EnhanceResult.error(msg().get("error.low-main-for-branch", reqMainLevel));
        }
        // 分支等级 = 分支池附魔的真实等级之和(含强化前的原版基础附魔)，与主强化按等级判定一致；
        // 满级门槛用持久值(底子快照+PDC 累计)兜底，防止磨刀石清掉真实附魔后重置门槛无限刷级
        int totalBranchLevel = BranchPool.effectiveTotalLevel(meta, item.getType(), data);
        if (data.isBranchMaxed(totalBranchLevel)) {
            return EnhanceResult.error(msg().get("error.branch-maxed"));
        }
        if (!hasDiamond) {
            return EnhanceResult.error(msg().get("error.need-diamond"));
        }

        String equipType = ItemChecker.getEquipType(item);
        if (!BranchPool.hasPool(equipType)) {
            return EnhanceResult.error(msg().get("error.no-branch-pool"));
        }

        List<String> pool = BranchPool.getPoolKeys(equipType);
        if (pool == null || pool.isEmpty()) {
            return EnhanceResult.error(msg().get("error.empty-pool"));
        }
        List<String> available = availableBranches(pool, data, meta);
        if (available.isEmpty()) {
            return EnhanceResult.error(msg().get("error.all-branches-maxed"));
        }

        // 同上：分支池校验全部通过后才扣钱
        if (!checkAndDeductGold(player, data.mainLevel())) {
            return EnhanceResult.error(msg().get("error.insufficient-gold", String.valueOf(cfg().getCostGold(data.mainLevel()))));
        }

        int rateIndex = Math.min(totalBranchLevel, cfg().getBranchSuccessRates().length - 1);
        boolean success = ThreadLocalRandom.current().nextInt(100) < cfg().getBranchSuccessRate(rateIndex);

        ItemStack result = item.clone();
        ItemMeta resultMeta = result.getItemMeta();

        if (success) {
            return applyBranchSuccess(player, result, resultMeta, data, available);
        } else {
            return applyBranchFailure(player, result, resultMeta, data);
        }
    }

    /** 池中尚未满级、且附魔键可正常解析的分支 */
    private static List<String> availableBranches(List<String> pool, EnhanceData data, ItemMeta meta) {
        int branchMaxLevel = cfg().getBranchMaxLevel();
        List<String> available = new ArrayList<>();
        for (String key : pool) {
            // 配置里写错的附魔键直接跳过，避免抽到无法解析的分支
            if (BranchPool.toEnchantment(key) == null) continue;
            Enchantment ench = BranchPool.toEnchantment(key);
            // 用含原版基础附魔的真实等级判断是否满级，与分支等级口径一致
            if (meta.getEnchantLevel(ench) < branchMaxLevel) {
                available.add(key);
            }
        }
        return available;
    }

    private static EnhanceResult applyBranchSuccess(Player player, ItemStack item,
                                                    ItemMeta meta, EnhanceData data, List<String> available) {
        String branchType = available.get(ThreadLocalRandom.current().nextInt(available.size()));

        int branchMaxLevel = cfg().getBranchMaxLevel();
        // 单分支 PDC 累计值封顶，避免磨刀石循环让同一分支越界
        int newLevel = Math.min(data.branches().getOrDefault(branchType, 0) + 1, branchMaxLevel);
        Enchantment newEnch = BranchPool.toEnchantment(branchType);

        EnhanceData newData = data.withBranch(branchType, newLevel);
        writeItem(meta, item.getType(), newData);
        item.setItemMeta(meta);

        if (cfg().isSoundEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        String enchName = LoreBuilder.getEnchantDisplayName(newEnch);
        sendActionBar(player, msg().getComponent("actionbar.branch-success", NamedTextColor.GREEN, enchName + " " + RomanNumber.toRoman(newLevel)));
        return EnhanceResult.success(item, data.mainLevel(), BranchPool.totalLevel(meta, item.getType(), newData));
    }
    private static EnhanceResult applyBranchFailure(Player player, ItemStack item,
                                                    ItemMeta meta, EnhanceData data) {
        boolean explode = ThreadLocalRandom.current().nextDouble() < cfg().getExplosionChance();
        item.setItemMeta(meta);

        if (cfg().isSoundEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }

        Component brFailMsg = msg().getComponent("actionbar.branch-failure", NamedTextColor.RED);
        if (explode) {
            brFailMsg = brFailMsg.append(msg().getComponent("actionbar.branch-explode", NamedTextColor.RED));
        }
        sendActionBar(player, brFailMsg);

        return EnhanceResult.failure(false, explode, data.mainLevel(), BranchPool.totalLevel(meta, item.getType(), data), item);
    }

    /**
     * 以 PDC 数据为唯一数据源，把强化数据写回物品：PDC → 附魔 → Lore 单向同步。
     * 调用方负责 {@code item.setItemMeta(meta)}。
     */
    public static void writeItem(ItemMeta meta, Material type, EnhanceData data) {
        captureBaseEnchants(meta, type);
        applyEnchantments(meta, type, data);
        PDCAdapter.writeData(meta, data);
        LoreBuilder.applyLore(meta, type, data);

        // 强化清零后物品已被还原成原版状态，快照可以丢弃
        if (data.mainLevel() <= 0 && !data.hasBranch()) {
            PDCAdapter.clearBaseEnchants(meta);
        }
    }

    /**
     * 首次锻造时把物品自带的原版附魔等级记下来。强化等级叠加在这个底子之上，
     * 锻造一把原版锋利 V 的剑得到的是锋利 VI 而不是锋利 I，
     * 同时 PDC 中仍只记录本插件给出的等级，不会让原版附魔白嫖强化等级。
     *
     * 老版本锻造过、但没有快照的物品：用「当前附魔等级 - 已记录的强化等级」反推底子，
     * 升级插件后这些装备的实际附魔等级不会发生跳变。
     */
    private static void captureBaseEnchants(ItemMeta meta, Material type) {
        if (PDCAdapter.hasBaseEnchants(meta)) return;

        EnhanceData stored = PDCAdapter.readData(meta);
        String mainKey = cfg().getMainEnchantKey(ItemChecker.getEquipType(type));

        Map<String, Integer> bases = new LinkedHashMap<>();
        for (String key : managedEnchantKeys(type, stored.branches())) {
            Enchantment ench = BranchPool.toEnchantment(key);
            if (ench == null) continue;
            int applied = key.equals(mainKey) ? stored.mainLevel() : 0;
            applied += stored.branches().getOrDefault(key, 0);
            int base = meta.getEnchantLevel(ench) - applied;
            if (base > 0) bases.put(key, base);
        }
        PDCAdapter.writeBaseEnchants(meta, bases);
    }

    /**
     * 让物品上由本插件管理的附魔等于「原版底子 + PDC 记录的强化等级」，
     * 归零的则移除。Lore 会在写入后直接读取实际主附魔等级用于显示。
     */
    private static void applyEnchantments(ItemMeta meta, Material type, EnhanceData data) {
        Map<String, Integer> bases = PDCAdapter.readBaseEnchants(meta);

        Map<String, Integer> target = new LinkedHashMap<>(bases);
        String mainKey = cfg().getMainEnchantKey(ItemChecker.getEquipType(type));
        if (mainKey != null && !mainKey.isEmpty()) {
            target.merge(mainKey, data.mainLevel(), Integer::sum);
        }
        for (var entry : data.branches().entrySet()) {
            target.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }

        for (String key : managedEnchantKeys(type, bases, data.branches())) {
            Enchantment ench = BranchPool.toEnchantment(key);
            if (ench == null) continue;
            int level = target.getOrDefault(key, 0);
            if (level > 0) {
                meta.addEnchant(ench, level, true);
            } else {
                meta.removeEnchant(ench);
            }
        }
    }

    /** 本插件负责维护的附魔键：主附魔 + 分支池 + 快照与强化数据里出现过的 */
    @SafeVarargs
    private static Set<String> managedEnchantKeys(Material type, Map<String, Integer>... extra) {
        String equipType = ItemChecker.getEquipType(type);
        Set<String> keys = new LinkedHashSet<>();

        String mainKey = cfg().getMainEnchantKey(equipType);
        if (mainKey != null && !mainKey.isEmpty()) keys.add(mainKey);

        List<String> poolKeys = BranchPool.getPoolKeys(equipType);
        if (poolKeys != null) keys.addAll(poolKeys);

        for (Map<String, Integer> map : extra) keys.addAll(map.keySet());
        return keys;
    }

    /** 物品经验只在强化成功时清零，这里只做校验 */
    private static boolean hasEnoughXp(EnhanceData data) {
        int req = data.xpRequired();
        if (req <= 0) return true;
        return data.xpPoints() >= req;
    }

    private static boolean checkAndDeductGold(Player player, int level) {
        var eco = HammrEnhance.getInstance().getEconomyManager();
        if (!eco.isEnabled()) return true;
        int cost = cfg().getCostGold(level);
        if (cost <= 0) return true;
        if (!eco.hasBalance(player, cost)) return false;
        // 扣款失败(经济插件拒绝/并发)时必须中止，否则等于免费强化还凭空给收入账户转账
        if (!eco.withdraw(player, cost)) return false;
        eco.depositToAccount(cfg().getIncomeAccount(), cost);
        return true;
    }

    private static void broadcastAnnouncement(Player player, ItemStack item) {
        if (!cfg().isBroadcastOnMaxLevel()) return;
        int maxLevel = cfg().getMainMaxLevel();
        Component msg = Component.text()
                .append(Component.text("[!] ", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" 成功将 ", NamedTextColor.GREEN))
                .append(Component.text(getItemSimpleName(item), NamedTextColor.WHITE))
                .append(Component.text(" 强化至 ", NamedTextColor.GREEN))
                .append(Component.text("+" + maxLevel, NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("！", NamedTextColor.GREEN))
                .build();
        HammrEnhance.getInstance().getServer().broadcast(msg);
    }

    private static String getItemSimpleName(ItemStack item) {
        return switch (item.getType()) {
            case NETHERITE_SWORD -> msg().get("item-name.NETHERITE_SWORD");
            case NETHERITE_AXE -> msg().get("item-name.NETHERITE_AXE");
            case NETHERITE_PICKAXE -> msg().get("item-name.NETHERITE_PICKAXE");
            case NETHERITE_SHOVEL -> msg().get("item-name.NETHERITE_SHOVEL");
            case NETHERITE_HOE -> msg().get("item-name.NETHERITE_HOE");
            case NETHERITE_HELMET -> msg().get("item-name.NETHERITE_HELMET");
            case NETHERITE_CHESTPLATE -> msg().get("item-name.NETHERITE_CHESTPLATE");
            case NETHERITE_LEGGINGS -> msg().get("item-name.NETHERITE_LEGGINGS");
            case NETHERITE_BOOTS -> msg().get("item-name.NETHERITE_BOOTS");
            default -> item.getType().name();
        };
    }

    private static void sendActionBar(Player player, Component message) {
        player.sendActionBar(message);
    }

    public static void syncInventory(Player player) {
        for (ItemStack item : player.getInventory().getContents()) {
            syncItem(item);
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            syncItem(item);
        }
        syncItem(player.getInventory().getItemInOffHand());
    }

    private static void syncItem(ItemStack item) {
        if (!ItemChecker.isNetheriteEquipment(item)) return;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        // 从未被本插件锻造过的装备一律不碰：原版附魔不再被"追认"成强化等级
        if (!PDCAdapter.isEnhanced(meta)) return;

        EnhanceData data = PDCAdapter.readData(meta);
        writeItem(meta, item.getType(), data);
        item.setItemMeta(meta);
    }

    public static void addItemXp(ItemStack item, int amount) {
        if (!item.hasItemMeta()) return;   // 未锻造过的物品无需克隆 ItemMeta
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        if (!PDCAdapter.isEnhanced(meta)) return;
        EnhanceData data = PDCAdapter.readData(meta);
        if (!data.hasMain() && !data.hasBranch()) return;
        if (data.isMainMaxed()) return;   // 主等级已满级不再吸收经验
        int req = data.xpRequired();
        if (req <= 0) return;
        int newXp = Math.min(data.xpPoints() + amount, req);
        if (newXp == data.xpPoints()) return;
        EnhanceData newData = data.withXP(newXp);
        PDCAdapter.writeData(meta, newData);
        LoreBuilder.applyLore(meta, item.getType(), newData);
        item.setItemMeta(meta);
    }

    public static int getMainSuccessRate(int level) {
        var settings = HammrEnhance.getInstance().getSettings();
        if (level < 1) return settings.getMainSuccessRates()[0];
        if (level >= settings.getMainSuccessRates().length) return settings.getMainSuccessRates()[settings.getMainSuccessRates().length - 1];
        return settings.getMainSuccessRate(level);
    }

    public static int getBranchSuccessRate(int level) {
        var settings = HammrEnhance.getInstance().getSettings();
        if (level < 1) return settings.getBranchSuccessRates()[0];
        if (level >= settings.getBranchSuccessRates().length) return settings.getBranchSuccessRates()[settings.getBranchSuccessRates().length - 1];
        return settings.getBranchSuccessRate(level);
    }
}
