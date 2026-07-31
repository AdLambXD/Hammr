package org.cubex.hammr.enhancement;

import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.cubex.hammr.HammrEnhance;
import org.jetbrains.annotations.Nullable;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class BranchPool {

    @Nullable
    public static String random(String type) {
        List<String> pool = getPool(type);
        if (pool == null || pool.isEmpty()) return null;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    @Nullable
    public static Enchantment toEnchantment(@Nullable String key) {
        if (key == null || key.isEmpty()) return null;
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(NamespacedKey.minecraft(key));
    }

    public static boolean hasPool(String type) {
        return HammrEnhance.getInstance().getSettings().hasBranchPool(type);
    }

    @Nullable
    public static List<String> getPoolKeys(String type) {
        return getPool(type);
    }

    private static List<String> getPool(String type) {
        return HammrEnhance.getInstance().getSettings().getBranchPool(type);
    }

    private BranchPool() {}
}
