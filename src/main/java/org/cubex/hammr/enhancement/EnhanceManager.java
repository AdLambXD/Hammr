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

    private static final int[] MAIN_RATES = {95, 95, 95, 95, 95, 85, 75, 60, 40, 20};
    private static final int[] BRANCH_RATES = {40, 35, 25, 15, 10};
    private static final int GOLD_COST = 1000;
    private static final double LEVEL_DOWN_CHANCE = 0.25;
    private static final double EXPLOSION_CHANCE = 0.10;

    public EnhanceResult performMainEnhance(Player player, ItemStack item,
                                             boolean hasDiamond, boolean hasIngot) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return EnhanceResult.error("物品数据异常");

        EnhanceData data = PDCAdapter.readData(meta);

        if (data.isMainMaxed()) {
            return EnhanceResult.error("主强化已达最高等级！");
        }

        boolean needsIngot = data.mainLevel() >= 6;
        if (needsIngot && !hasIngot) {
            return EnhanceResult.error("需要下界合金锭！");
        }
        if (!needsIngot && !hasDiamond) {
            return EnhanceResult.error("需要钻石！");
        }

        if (!checkAndDeductGold(player)) {
            return EnhanceResult.error("金币不足！需要 " + GOLD_COST + " 金币。");
        }

        int rateIndex = Math.min(data.mainLevel(), MAIN_RATES.length - 1);
        boolean success = ThreadLocalRandom.current().nextInt(100) < MAIN_RATES[rateIndex];

        ItemStack result = item.clone();
        ItemMeta resultMeta = result.getItemMeta();

        if (success) {
            return applyMainSuccess(player, result, resultMeta, data);
        } else {
            return applyMainFailure(player, result, resultMeta, data, needsIngot ? "NETHERITE_INGOT" : "DIAMOND");
        }
    }

    private EnhanceResult applyMainSuccess(Player player, ItemStack item,
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

        EnhanceData newData = new EnhanceData(newLevel, data.branches());
        PDCAdapter.writeData(meta, newData);
        meta.lore(LoreBuilder.buildLore(newData));
        item.setItemMeta(meta);

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        if (newLevel >= 10) {
            broadcastAnnouncement(player, item);
        }

        sendActionBar(player, Component.text()
                .append(Component.text("✦ 强化成功！当前等级: +", NamedTextColor.GREEN))
                .append(Component.text(String.valueOf(newLevel), NamedTextColor.GREEN, TextDecoration.BOLD))
                .build());
        return EnhanceResult.success(item, newLevel, data.branchLevel());
    }

    private EnhanceResult applyMainFailure(Player player, ItemStack item,
                                           ItemMeta meta, EnhanceData data, String material) {
        boolean levelDown = ThreadLocalRandom.current().nextDouble() < LEVEL_DOWN_CHANCE;
        boolean explode = ThreadLocalRandom.current().nextDouble() < EXPLOSION_CHANCE;

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

            EnhanceData newData = new EnhanceData(newMain, data.branches());
            PDCAdapter.writeData(meta, newData);
            meta.lore(LoreBuilder.buildLore(newData));
        }

        item.setItemMeta(meta);
        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);

        Component failMsg = Component.text("✘ 强化失败", NamedTextColor.RED);
        if (levelDown) {
            failMsg = failMsg.append(Component.text("，等级降为 +", NamedTextColor.RED))
                    .append(Component.text(String.valueOf(newMain), NamedTextColor.RED, TextDecoration.BOLD));
        }
        if (explode) {
            failMsg = failMsg.append(Component.text("，铁砧爆炸！", NamedTextColor.RED));
        }
        sendActionBar(player, failMsg);

        return EnhanceResult.failure(levelDown, explode, newMain, data.branchLevel(), item);
    }

    public EnhanceResult performBranchEnhance(Player player, ItemStack item, boolean hasDiamond) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return EnhanceResult.error("物品数据异常");

        EnhanceData data = PDCAdapter.readData(meta);

        if (data.mainLevel() < 8) {
            return EnhanceResult.error("主强化需达到 8 级才可进行分支强化！");
        }
        if (data.isBranchMaxed()) {
            return EnhanceResult.error("分支强化已达最高等级！");
        }
        if (!hasDiamond) {
            return EnhanceResult.error("需要钻石！");
        }
        if (!checkAndDeductGold(player)) {
            return EnhanceResult.error("金币不足！需要 " + GOLD_COST + " 金币。");
        }

        String equipType = ItemChecker.getEquipType(item);
        if (!BranchPool.hasPool(equipType)) {
            return EnhanceResult.error("该装备无法进行分支强化！");
        }

        int rateIndex = Math.min(data.branchCount(), BRANCH_RATES.length - 1);
        boolean success = ThreadLocalRandom.current().nextInt(100) < BRANCH_RATES[rateIndex];

        ItemStack result = item.clone();
        ItemMeta resultMeta = result.getItemMeta();

        if (success) {
            return applyBranchSuccess(player, result, resultMeta, data, equipType);
        } else {
            return applyBranchFailure(player, result, resultMeta, data);
        }
    }

    private EnhanceResult applyBranchSuccess(Player player, ItemStack item,
                                             ItemMeta meta, EnhanceData data, String equipType) {
        List<String> pool = BranchPool.getPoolKeys(equipType);
        if (pool == null || pool.isEmpty()) {
            return EnhanceResult.error("分支池为空！");
        }

        List<String> available = new ArrayList<>();
        for (String key : pool) {
            if (data.branches().getOrDefault(key, 0) < 6) {
                available.add(key);
            }
        }
        if (available.isEmpty()) {
            return EnhanceResult.error("所有分支已达最高等级！");
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

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

        String enchName = LoreBuilder.getEnchantDisplayName(newEnch);
        sendActionBar(player, Component.text()
                .append(Component.text("✦ 分支强化成功！", NamedTextColor.GREEN))
                .append(Component.text(enchName + " " + RomanNumber.toRoman(newLevel), NamedTextColor.GREEN, TextDecoration.BOLD))
                .build());
        return EnhanceResult.success(item, data.mainLevel(), newData.branchLevel());
    }

    private EnhanceResult applyBranchFailure(Player player, ItemStack item,
                                             ItemMeta meta, EnhanceData data) {
        boolean explode = ThreadLocalRandom.current().nextDouble() < EXPLOSION_CHANCE;
        item.setItemMeta(meta);

        player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);

        Component brFailMsg = Component.text("✘ 分支强化失败", NamedTextColor.RED);
        if (explode) {
            brFailMsg = brFailMsg.append(Component.text("，铁砧爆炸！", NamedTextColor.RED));
        }
        sendActionBar(player, brFailMsg);

        return EnhanceResult.failure(false, explode, data.mainLevel(), data.branchLevel(), item);
    }

    private void clearAllEnhancements(ItemMeta meta, EnhanceData data, Enchantment mainEnch) {
        if (mainEnch != null) meta.removeEnchant(mainEnch);
        for (var entry : data.branches().entrySet()) {
            Enchantment branchEnch = BranchPool.toEnchantment(entry.getKey());
            if (branchEnch != null) meta.removeEnchant(branchEnch);
        }
    }

    private boolean checkAndDeductGold(Player player) {
        var eco = HammrEnhance.getInstance().getEconomyManager();
        if (!eco.isEnabled()) return true;
        if (!eco.hasBalance(player, GOLD_COST)) return false;
        eco.withdraw(player, GOLD_COST);
        return true;
    }

    private void broadcastAnnouncement(Player player, ItemStack item) {
        var msg = Component.text()
                .append(Component.text("[!] ", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text(player.getName(), NamedTextColor.YELLOW))
                .append(Component.text(" 成功将 ", NamedTextColor.GREEN))
                .append(Component.text(getItemSimpleName(item), NamedTextColor.WHITE))
                .append(Component.text(" 强化至 ", NamedTextColor.GREEN))
                .append(Component.text("+10", NamedTextColor.GOLD, TextDecoration.BOLD))
                .append(Component.text("！", NamedTextColor.GREEN))
                .build();
        HammrEnhance.getInstance().getServer().broadcast(msg);
    }

    private String getItemSimpleName(ItemStack item) {
        return switch (item.getType()) {
            case NETHERITE_SWORD -> "下界合金剑";
            case NETHERITE_AXE -> "下界合金斧";
            case NETHERITE_PICKAXE -> "下界合金镐";
            case NETHERITE_SHOVEL -> "下界合金锹";
            case NETHERITE_HOE -> "下界合金锄";
            case NETHERITE_HELMET -> "下界合金头盔";
            case NETHERITE_CHESTPLATE -> "下界合金胸甲";
            case NETHERITE_LEGGINGS -> "下界合金护腿";
            case NETHERITE_BOOTS -> "下界合金靴子";
            default -> item.getType().name();
        };
    }

    private void sendActionBar(Player player, Component message) {
        player.sendActionBar(message);
    }

    public static void syncInventory(org.bukkit.entity.Player player) {
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

        EnhanceData actualData = new EnhanceData(actualMainLevel, actualBranches);
        EnhanceData storedData = PDCAdapter.readData(meta);

        if (!actualData.equals(storedData)) {
            PDCAdapter.writeData(meta, actualData);
        }
        meta.lore(LoreBuilder.buildLore(actualData));
        item.setItemMeta(meta);
    }

    public static int getMainSuccessRate(int level) {
        if (level < 1) return MAIN_RATES[0];
        if (level >= MAIN_RATES.length) return MAIN_RATES[MAIN_RATES.length - 1];
        return MAIN_RATES[level - 1];
    }

    public static int getBranchSuccessRate(int level) {
        if (level < 1) return BRANCH_RATES[0];
        if (level >= BRANCH_RATES.length) return BRANCH_RATES[BRANCH_RATES.length - 1];
        return BRANCH_RATES[level - 1];
    }
}
