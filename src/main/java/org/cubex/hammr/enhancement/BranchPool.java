package org.cubex.hammr.enhancement;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class BranchPool {

    private static final Map<String, List<String>> POOL = Map.of(
            "SWORD", List.of("fire_aspect", "bane_of_arthropods", "smite"),
            "AXE", List.of("fire_aspect", "bane_of_arthropods", "smite"),
            "HELMET", List.of("projectile_protection", "fire_protection", "blast_protection"),
            "CHESTPLATE", List.of("projectile_protection", "fire_protection", "blast_protection"),
            "LEGGINGS", List.of("projectile_protection", "fire_protection", "blast_protection"),
            "BOOTS", List.of("projectile_protection", "fire_protection", "blast_protection")
    );

    @Nullable
    public static String random(String type) {
        List<String> pool = POOL.get(type);
        if (pool == null || pool.isEmpty()) return null;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    @Nullable
    public static Enchantment toEnchantment(@Nullable String key) {
        if (key == null || key.isEmpty()) return null;
        return Registry.ENCHANTMENT.get(NamespacedKey.minecraft(key));
    }

    public static boolean hasPool(String type) {
        return POOL.containsKey(type);
    }

    @Nullable
    public static List<String> getPoolKeys(String type) {
        return POOL.get(type);
    }

    private BranchPool() {}
}
