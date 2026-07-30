package org.cubex.hammr.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.enchantments.Enchantment;
import org.cubex.hammr.storage.EnhanceData;

public final class LoreBuilder {

    private static final NamedTextColor GOLD = NamedTextColor.GOLD;
    private static final NamedTextColor GREEN = NamedTextColor.GREEN;
    private static final NamedTextColor GRAY = NamedTextColor.GRAY;
    private static final int BAR_TOTAL = 25;

    public static java.util.List<Component> buildLore(EnhanceData data) {
        var lore = new java.util.ArrayList<Component>();
        StringBuilder sb = new StringBuilder();

        String levelTag = "[+" + data.mainLevel();
        if (data.branchLevel() > 0) {
            levelTag += "(" + data.branchLevel() + ")";
        }
        levelTag += "]";
        sb.append("§6§l").append(levelTag);
        lore.add(Component.text(sb.toString()));

        if (data.mainLevel() < 10) {
            StringBuilder bar = new StringBuilder();
            double pct = data.xpProgress();
            int filled = (int) Math.round(pct * BAR_TOTAL);
            bar.append("§a");
            for (int i = 0; i < filled && i < BAR_TOTAL; i++) bar.append('|');
            bar.append("§8");
            for (int i = filled; i < BAR_TOTAL; i++) bar.append('|');
            bar.append(" §f").append(Math.round(pct * 100)).append('%');
            lore.add(Component.text(bar.toString()));
        }

        return lore;
    }

    public static Enchantment getMainEnchant(org.bukkit.Material material) {
        String n = material.name();
        if (n.contains("SWORD") || n.contains("AXE")) return Enchantment.SHARPNESS;
        if (n.contains("PICKAXE") || n.contains("SHOVEL") || n.contains("HOE")) return Enchantment.EFFICIENCY;
        if (n.contains("HELMET") || n.contains("CHESTPLATE") ||
            n.contains("LEGGINGS") || n.contains("BOOTS")) return Enchantment.PROTECTION;
        return null;
    }

    public static String getEnchantDisplayName(Enchantment ench) {
        String key = ench.getKey().getKey();
        return switch (key) {
            case "sharpness" -> "锋利";
            case "efficiency" -> "效率";
            case "protection" -> "保护";
            case "fire_aspect" -> "火焰附加";
            case "bane_of_arthropods" -> "节肢杀手";
            case "smite" -> "亡灵杀手";
            case "projectile_protection" -> "弹射物保护";
            case "fire_protection" -> "火焰保护";
            case "blast_protection" -> "爆炸保护";
            default -> key;
        };
    }

    private LoreBuilder() {}
}
