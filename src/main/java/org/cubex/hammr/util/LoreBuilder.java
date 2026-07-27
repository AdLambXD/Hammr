package org.cubex.hammr.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.cubex.hammr.storage.EnhanceData;
import java.util.ArrayList;
import java.util.List;

public final class LoreBuilder {

    private static final NamedTextColor GOLD = NamedTextColor.GOLD;

    public static List<Component> buildLore(EnhanceData data) {
        List<Component> lore = new ArrayList<>();

        String levelTag = "[+" + data.mainLevel();
        if (data.branchLevel() > 0) {
            levelTag += "(" + data.branchLevel() + ")";
        }
        levelTag += "]";
        lore.add(Component.text(levelTag, GOLD, TextDecoration.BOLD));

        return lore;
    }

    public static Enchantment getMainEnchant(Material material) {
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
