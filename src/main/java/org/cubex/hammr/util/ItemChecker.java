package org.cubex.hammr.util;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import java.util.Set;

public final class ItemChecker {

    private static final Set<Material> WEAPONS = Set.of(
            Material.NETHERITE_SWORD, Material.NETHERITE_AXE
    );
    private static final Set<Material> TOOLS = Set.of(
            Material.NETHERITE_PICKAXE, Material.NETHERITE_SHOVEL, Material.NETHERITE_HOE
    );
    private static final Set<Material> ARMOR = Set.of(
            Material.NETHERITE_HELMET, Material.NETHERITE_CHESTPLATE,
            Material.NETHERITE_LEGGINGS, Material.NETHERITE_BOOTS
    );
    private static final Set<Material> ALL = new java.util.HashSet<>();

    static {
        ALL.addAll(WEAPONS);
        ALL.addAll(TOOLS);
        ALL.addAll(ARMOR);
    }

    public static boolean isNetheriteEquipment(ItemStack item) {
        return item != null && !item.getType().isAir() && ALL.contains(item.getType());
    }

    public static boolean isWeapon(ItemStack item) {
        return item != null && WEAPONS.contains(item.getType());
    }

    public static boolean isTool(ItemStack item) {
        return item != null && TOOLS.contains(item.getType());
    }

    public static boolean isArmor(ItemStack item) {
        return item != null && ARMOR.contains(item.getType());
    }

    public static boolean hasBranchPool(ItemStack item) {
        return isWeapon(item) || isArmor(item);
    }

    public static boolean isDiamond(ItemStack item) {
        return item != null && item.getType() == Material.DIAMOND;
    }

    public static boolean isNetheriteIngot(ItemStack item) {
        return item != null && item.getType() == Material.NETHERITE_INGOT;
    }

    public static String getEquipType(ItemStack item) {
        if (item == null) return "TOOL";
        return getEquipType(item.getType());
    }

    public static String getEquipType(Material material) {
        if (material == null) return "TOOL";
        return switch (material) {
            case NETHERITE_SWORD -> "SWORD";
            case NETHERITE_AXE -> "AXE";
            case NETHERITE_HELMET -> "HELMET";
            case NETHERITE_CHESTPLATE -> "CHESTPLATE";
            case NETHERITE_LEGGINGS -> "LEGGINGS";
            case NETHERITE_BOOTS -> "BOOTS";
            default -> "TOOL";
        };
    }

    private ItemChecker() {}
}
