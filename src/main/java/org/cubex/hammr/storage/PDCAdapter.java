package org.cubex.hammr.storage;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.cubex.hammr.HammrEnhance;
import org.jetbrains.annotations.NotNull;

public final class PDCAdapter {

    private static final NamespacedKey MAIN_KEY = key("enhance_main_level");
    private static final NamespacedKey BRANCH_KEY = key("enhance_branch_level");
    private static final NamespacedKey BRANCH_TYPE_KEY = key("enhance_branch_type");

    private static NamespacedKey key(String name) {
        return new NamespacedKey(HammrEnhance.getInstance(), name);
    }

    public static void writeData(@NotNull ItemMeta meta, @NotNull EnhanceData data) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        setInt(pdc, MAIN_KEY, data.mainLevel());
        setInt(pdc, BRANCH_KEY, data.branchLevel());
        setString(pdc, BRANCH_TYPE_KEY, data.branchType());
    }

    @NotNull
    public static EnhanceData readData(@NotNull ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int main = pdc.getOrDefault(MAIN_KEY, PersistentDataType.INTEGER, 0);
        int branch = pdc.getOrDefault(BRANCH_KEY, PersistentDataType.INTEGER, 0);
        String type = pdc.get(BRANCH_TYPE_KEY, PersistentDataType.STRING);
        return new EnhanceData(main, branch, type);
    }

    public static boolean isEnhanced(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().has(MAIN_KEY, PersistentDataType.INTEGER);
    }

    private static void setInt(PersistentDataContainer pdc, NamespacedKey key, int value) {
        if (value > 0) {
            pdc.set(key, PersistentDataType.INTEGER, value);
        } else {
            pdc.remove(key);
        }
    }

    private static void setString(PersistentDataContainer pdc, NamespacedKey key, String value) {
        if (value != null && !value.isEmpty()) {
            pdc.set(key, PersistentDataType.STRING, value);
        } else {
            pdc.remove(key);
        }
    }

    private PDCAdapter() {}
}
