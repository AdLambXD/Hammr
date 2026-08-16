package org.cubex.hammr.util;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.cubex.hammr.HammrEnhance;
import org.cubex.hammr.storage.EnhanceData;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

public final class LoreBuilder {

    /**
     * 将强化 Lore 写入物品。数据为空(从未锻造/已移除强化)时直接清除 Lore，
     * 避免未锻造的装备被打上 [+0] 标记。
     */
    public static void applyLore(org.bukkit.inventory.meta.ItemMeta meta, EnhanceData data) {
        var lore = buildLore(data);
        meta.lore(lore.isEmpty() ? null : lore);
    }

    public static java.util.List<Component> buildLore(EnhanceData data) {
        var lore = new java.util.ArrayList<Component>();
        if (data.mainLevel() <= 0 && !data.hasBranch()) return lore;

        var settings = HammrEnhance.getInstance().getSettings();
        StringBuilder sb = new StringBuilder();

        String levelTag = "[+" + data.mainLevel();
        if (data.branchLevel() > 0) {
            levelTag += "(" + data.branchLevel() + ")";
        }
        levelTag += "]";
        sb.append("§6§l").append(levelTag);
        lore.add(Component.text(sb.toString()));

        if (data.mainLevel() < settings.getMainMaxLevel()) {
            StringBuilder bar = new StringBuilder();
            double pct = data.xpProgress();
            int width = settings.getProgressBarWidth();
            int filled = (int) Math.round(pct * width);
            String c = settings.getProgressBarChar();

            bar.append(settings.getProgressBarFilledColor());
            for (int i = 0; i < filled && i < width; i++) bar.append(c);
            bar.append(settings.getProgressBarEmptyColor());
            for (int i = filled; i < width; i++) bar.append(c);
            bar.append(settings.getProgressBarSuffixColor());
            bar.append(formatSuffix(settings.getProgressBarSuffixFormat(), Math.round(pct * 100)));
            lore.add(Component.text(bar.toString()));
        }

        return lore;
    }

    /** 配置里的格式串写错时退回默认写法，不能让异常从事件处理里抛出去 */
    private static String formatSuffix(String format, long percent) {
        try {
            return String.format(format, percent);
        } catch (java.util.IllegalFormatException e) {
            HammrEnhance.getInstance().getLogger().warning(
                    "Invalid progress-bar.suffix-format: " + format);
            return " " + percent + "%";
        }
    }

    public static Enchantment getMainEnchant(org.bukkit.Material material) {
        String equipType = ItemChecker.getEquipType(material);
        String key = HammrEnhance.getInstance().getSettings().getMainEnchantKey(equipType);
        if (key == null || key.isEmpty()) return null;
        try {
            return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                    .get(NamespacedKey.minecraft(key));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String getEnchantDisplayName(Enchantment ench) {
        String key = ench.getKey().getKey();
        return HammrEnhance.getInstance().getMessages().get("enchant-name." + key, key);
    }

    private LoreBuilder() {}
}
