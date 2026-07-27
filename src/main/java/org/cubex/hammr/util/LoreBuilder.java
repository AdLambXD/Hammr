package org.cubex.hammr.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.cubex.hammr.enhancement.BranchPool;
import org.cubex.hammr.storage.EnhanceData;
import java.util.ArrayList;
import java.util.List;

public final class LoreBuilder {

    private static final NamedTextColor GOLD = NamedTextColor.GOLD;
    private static final NamedTextColor GRAY = NamedTextColor.GRAY;
    private static final NamedTextColor AQUA = NamedTextColor.AQUA;
    private static final NamedTextColor WHITE = NamedTextColor.WHITE;

    public static List<Component> buildLore(ItemStack item, EnhanceData data) {
        List<Component> lore = new ArrayList<>();

        String prefix = "[+" + data.mainLevel() + "] ";
        Component nameComp = getItemDisplayComponent(item);
        lore.add(Component.text(prefix, GOLD, TextDecoration.BOLD).append(nameComp));

        if (data.hasMain()) {
            Component mainLine = buildMainEnchantLine(item, data);
            if (mainLine != null) {
                lore.add(mainLine);
            }
        }

        if (ItemChecker.hasBranchPool(item)) {
            if (data.hasBranch()) {
                Enchantment branchEnch = BranchPool.toEnchantment(data.branchType());
                if (branchEnch != null) {
                    String name = getEnchantDisplayName(branchEnch);
                    lore.add(Component.text(name + " " + RomanNumber.toRoman(data.branchLevel()), AQUA));
                }
            } else if (data.mainLevel() >= 8) {
                lore.add(Component.text("分支: 无", GRAY));
            }
        }

        ItemMeta durabilityMeta = item.getItemMeta();
        int maxDura = item.getType().getMaxDurability();
        int currentDura = maxDura - (durabilityMeta instanceof Damageable d ? d.getDamage() : 0);
        lore.add(Component.text("耐久度: " + currentDura + "/" + maxDura, GRAY));

        return lore;
    }

    private static Component buildMainEnchantLine(ItemStack item, EnhanceData data) {
        Enchantment mainEnch = getMainEnchant(item.getType());
        if (mainEnch == null) return null;

        ItemMeta meta = item.getItemMeta();
        int totalLevel = (meta != null) ? meta.getEnchantLevel(mainEnch) : 0;
        if (totalLevel <= 0) return null;

        String name = getEnchantDisplayName(mainEnch);
        return Component.text(name + " " + RomanNumber.toRoman(totalLevel), GRAY);
    }

    public static Enchantment getMainEnchant(Material material) {
        String n = material.name();
        if (n.contains("SWORD") || n.contains("AXE")) {
            return Enchantment.SHARPNESS;
        }
        if (n.contains("PICKAXE") || n.contains("SHOVEL") || n.contains("HOE")) {
            return Enchantment.EFFICIENCY;
        }
        if (n.contains("HELMET") || n.contains("CHESTPLATE") ||
            n.contains("LEGGINGS") || n.contains("BOOTS")) {
            return Enchantment.PROTECTION;
        }
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

    private static Component getItemDisplayComponent(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return meta.displayName();
        }
        return Component.text(getDefaultItemName(item.getType()), WHITE);
    }

    private static String getDefaultItemName(Material material) {
        return switch (material) {
            case NETHERITE_SWORD -> "下界合金剑";
            case NETHERITE_AXE -> "下界合金斧";
            case NETHERITE_PICKAXE -> "下界合金镐";
            case NETHERITE_SHOVEL -> "下界合金锹";
            case NETHERITE_HOE -> "下界合金锄";
            case NETHERITE_HELMET -> "下界合金头盔";
            case NETHERITE_CHESTPLATE -> "下界合金胸甲";
            case NETHERITE_LEGGINGS -> "下界合金护腿";
            case NETHERITE_BOOTS -> "下界合金靴子";
            default -> material.name();
        };
    }

    private LoreBuilder() {}
}
