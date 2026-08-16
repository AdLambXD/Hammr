package org.cubex.hammr.enhancement;

import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.meta.ItemMeta;
import org.cubex.hammr.HammrEnhance;
import org.cubex.hammr.storage.EnhanceData;
import org.cubex.hammr.util.ItemChecker;
import org.jetbrains.annotations.Nullable;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class BranchPool {

    @Nullable
    public static String random(String type) {
        List<String> pool = getPool(type);
        if (pool == null || pool.isEmpty()) return null;
        return pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
    }

    /**
     * 分支等级：物品上分支池内全部附魔的真实等级之和。
     * 强化前就有的原版分支附魔(如亡灵杀手V)也计入，与物品实际附魔等级一致。
     */
    public static int totalLevel(ItemMeta meta, Material type, EnhanceData data) {
        if (meta == null) return 0;
        List<String> pool = getPool(ItemChecker.getEquipType(type));
        Set<String> keys = new LinkedHashSet<>();
        if (pool != null) keys.addAll(pool);
        keys.addAll(data.branches().keySet());
        int total = 0;
        for (String key : keys) {
            Enchantment ench = toEnchantment(key);
            if (ench == null) continue;
            int level = meta.getEnchantLevel(ench);
            if (level > 0) total += level;
        }
        return total;
    }

    @Nullable
    public static Enchantment toEnchantment(@Nullable String key) {
        if (key == null || key.isEmpty()) return null;
        try {
            return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)
                    .get(NamespacedKey.minecraft(key));
        } catch (IllegalArgumentException e) {
            // 配置里写了非法字符(如大写)时 NamespacedKey 会抛异常，不能让它冒到事件里
            return null;
        }
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
