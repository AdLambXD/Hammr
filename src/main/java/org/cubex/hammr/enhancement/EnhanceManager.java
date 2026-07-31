package org.cubex.hammr.enhancement;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
import java.util.List;
import java.util.Map;
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

        if (!checkAndDeductGold(player, data.mainLevel())) {
            return EnhanceResult.error(msg().get("error.insufficient-gold", cfg().getCostGold(data.mainLevel())));
        }

        if (!checkAndDeductXp(player, data)) {
            return EnhanceResult.error(msg().get("error.insufficient-xp", data.xpRequired()));
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

        Enchantment mainEnch = LoreBuilder.getMainEnchant(item.getType());
        if (mainEnch != null) {
            int current = meta.getEnchantLevel(mainEnch);
            meta.addEnchant(mainEnch, current + 1, true);
        }

        for (var entry : data.branches().entrySet()) {
            Enchantment branchEnch = BranchPool.toEnchantment(entry.getKey());
            if (branchEnch != null) {
                meta.addEnchant(branchEnch, entry.getValue(), true);
            }
        }

        EnhanceData newData = new EnhanceData(newLevel, data.branches(), 0);
        PDCAdapter.writeData(meta, newData);
        meta.lore(LoreBuilder.buildLore(newData));
        item.setItemMeta(meta);

        if (cfg().isSoundEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        if (newLevel >= cfg().getMainMaxLevel()) {
            broadcastAnnouncement(player, item);
        }

        sendActionBar(player, msg().getComponent("actionbar.main-success", NamedTextColor.GREEN, TextDecoration.BOLD, String.valueOf(newLevel)));
        return EnhanceResult.success(item, newLevel, data.branchLevel());
    }

    private static EnhanceResult applyMainFailure(Player player, ItemStack item,
                                                  ItemMeta meta, EnhanceData data, String material) {
        boolean levelDown = ThreadLocalRandom.current().nextDouble() < cfg().getLevelDownChance();
        boolean explode = ThreadLocalRandom.current().nextDouble() < cfg().getExplosionChance();

        int newMain = data.mainLevel();
        if (levelDown) {
            newMain = Math.max(0, data.mainLevel() - 1);
        }

        Enchantment mainEnch = LoreBuilder.getMainEnchant(item.getType());

        if (newMain == 0) {
            clearAllEnhancements(meta, data, mainEnch);
            EnhanceData empty = EnhanceData.EMPTY;
            PDCAdapter.writeData(meta, empty);
            meta.lore(new ArrayList<>());
        } else if (levelDown && mainEnch != null) {
            int current = meta.getEnchantLevel(mainEnch);
            if (current > 1) {
                meta.addEnchant(mainEnch, current - 1, true);
            } else {
                meta.removeEnchant(mainEnch);
            }

            for (var entry : data.branches().entrySet()) {
                Enchantment branchEnch = BranchPool.toEnchantment(entry.getKey());
                if (branchEnch != null) {
                    meta.addEnchant(branchEnch, entry.getValue(), true);
                }
            }

            EnhanceData newData = new EnhanceData(newMain, data.branches(), data.xpPoints());
            PDCAdapter.writeData(meta, newData);
            meta.lore(LoreBuilder.buildLore(newData));
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

        return EnhanceResult.failure(levelDown, explode, newMain, data.branchLevel(), item);
    }

    public static EnhanceResult performBranchEnhance(Player player, ItemStack item, boolean hasDiamond) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return EnhanceResult.error(msg().get("error.meta-null"));

        EnhanceData data = PDCAdapter.readData(meta);

        int reqMainLevel = cfg().getBranchMinMainLevel();
        if (data.mainLevel() < reqMainLevel) {
            return EnhanceResult.error(msg().get("error.low-main-for-branch", reqMainLevel));
        }
        if (data.isBranchMaxed()) {
            return EnhanceResult.error(msg().get("error.branch-maxed"));
        }
        if (!hasDiamond) {
            return EnhanceResult.error(msg().get("error.need-diamond"));
        }
        if (!checkAndDeductGold(player, data.mainLevel())) {
            return EnhanceResult.error(msg().get("error.insufficient-gold", cfg().getCostGold(data.mainLevel())));
        }

        String equipType = ItemChecker.getEquipType(item);
        if (!BranchPool.hasPool(equipType)) {
            return EnhanceResult.error(msg().get("error.no-branch-pool"));
        }

        int rateIndex = Math.min(data.branchCount(), cfg().getBranchSuccessRates().length - 1);
        boolean success = ThreadLocalRandom.current().nextInt(100) < cfg().getBranchSuccessRate(rateIndex);

        ItemStack result = item.clone();
        ItemMeta resultMeta = result.getItemMeta();

        if (success) {
            return applyBranchSuccess(player, result, resultMeta, data, equipType);
        } else {
            return applyBranchFailure(player, result, resultMeta, data);
        }
    }

    private static EnhanceResult applyBranchSuccess(Player player, ItemStack item,
                                                    ItemMeta meta, EnhanceData data, String equipType) {
        List<String> pool = BranchPool.getPoolKeys(equipType);
        if (pool == null || pool.isEmpty()) {
            return EnhanceResult.error(msg().get("error.empty-pool"));
        }

        int branchMaxLevel = cfg().getBranchMaxLevel();
        List<String> available = new ArrayList<>();
        for (String key : pool) {
            if (data.branches().getOrDefault(key, 0) < branchMaxLevel) {
                available.add(key);
            }
        }
        if (available.isEmpty()) {
            return EnhanceResult.error(msg().get("error.all-branches-maxed"));
        }
        String branchType = available.get(ThreadLocalRandom.current().nextInt(available.size()));

        int newLevel = data.branches().getOrDefault(branchType, 0) + 1;

        Enchantment newEnch = BranchPool.toEnchantment(branchType);
        if (newEnch != null) {
            meta.addEnchant(newEnch, newLevel, true);
        }

        Enchantment mainEnch = LoreBuilder.getMainEnchant(item.getType());
        if (mainEnch != null && data.hasMain()) {
            int mainLevel = meta.getEnchantLevel(mainEnch);
            if (mainLevel == 0) {
                meta.addEnchant(mainEnch, data.mainLevel(), true);
            }
        }

        EnhanceData newData = data.withBranch(branchType, newLevel);
        PDCAdapter.writeData(meta, newData);
        meta.lore(LoreBuilder.buildLore(newData));
        item.setItemMeta(meta);

        if (cfg().isSoundEnabled()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        }

        String enchName = LoreBuilder.getEnchantDisplayName(newEnch);
        sendActionBar(player, msg().getComponent("actionbar.branch-success", NamedTextColor.GREEN, enchName + " " + RomanNumber.toRoman(newLevel)));
        return EnhanceResult.success(item, data.mainLevel(), newData.branchLevel());
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

        return EnhanceResult.failure(false, explode, data.mainLevel(), data.branchLevel(), item);
    }

    private static void clearAllEnhancements(ItemMeta meta, EnhanceData data, Enchantment mainEnch) {
        if (mainEnch != null) meta.removeEnchant(mainEnch);
        for (var entry : data.branches().entrySet()) {
            Enchantment branchEnch = BranchPool.toEnchantment(entry.getKey());
            if (branchEnch != null) meta.removeEnchant(branchEnch);
        }
    }

    private static boolean checkAndDeductXp(Player player, EnhanceData data) {
        int req = data.xpRequired();
        if (req <= 0) return true;
        if (data.xpPoints() >= req) return true;
        if (player.getLevel() < req) return false;
        player.setLevel(player.getLevel() - req);
        return true;
    }

    private static boolean checkAndDeductGold(Player player, int level) {
        var eco = HammrEnhance.getInstance().getEconomyManager();
        if (!eco.isEnabled()) return true;
        int cost = cfg().getCostGold(level);
        if (!eco.hasBalance(player, cost)) return false;
        eco.withdraw(player, cost);
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

        Enchantment mainEnch = LoreBuilder.getMainEnchant(item.getType());
        int actualMainLevel = (mainEnch != null) ? meta.getEnchantLevel(mainEnch) : 0;

        String equipType = ItemChecker.getEquipType(item);
        Map<String, Integer> actualBranches = new LinkedHashMap<>();

        List<String> poolKeys = BranchPool.getPoolKeys(equipType);
        if (poolKeys != null) {
            for (String key : poolKeys) {
                Enchantment ench = BranchPool.toEnchantment(key);
                if (ench != null && meta.hasEnchant(ench)) {
                    actualBranches.put(key, meta.getEnchantLevel(ench));
                }
            }
        }

        if (actualMainLevel == 0 && actualBranches.isEmpty()) return;

        EnhanceData storedData = PDCAdapter.readData(meta);
        EnhanceData actualData = new EnhanceData(actualMainLevel, actualBranches, storedData.xpPoints());

        if (!actualData.equals(storedData)) {
            PDCAdapter.writeData(meta, actualData);
        }
        meta.lore(LoreBuilder.buildLore(actualData));
        item.setItemMeta(meta);
    }

    public static void addItemXp(ItemStack item, int amount) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;
        EnhanceData data = PDCAdapter.readData(meta);
        if (!data.hasMain() && !data.hasBranch()) return;
        int req = data.xpRequired();
        if (req <= 0) return;
        int newXp = Math.min(data.xpPoints() + amount, req);
        if (newXp == data.xpPoints()) return;
        EnhanceData newData = data.withXP(newXp);
        PDCAdapter.writeData(meta, newData);
        meta.lore(LoreBuilder.buildLore(newData));
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
