package org.cubex.hammr.storage;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.cubex.hammr.HammrEnhance;
import org.jetbrains.annotations.NotNull;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class PDCAdapter {

    private static final NamespacedKey MAIN_KEY = key("enhance_main_level");
    private static final NamespacedKey BRANCHES_KEY = key("enhance_branches");
    private static final NamespacedKey XP_KEY = key("enhance_xp_points");

    private static NamespacedKey key(String name) {
        return new NamespacedKey(HammrEnhance.getInstance(), name);
    }

    public static void writeData(@NotNull ItemMeta meta, @NotNull EnhanceData data) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        setInt(pdc, MAIN_KEY, data.mainLevel());
        setBranches(pdc, BRANCHES_KEY, data.branches());
        setInt(pdc, XP_KEY, data.xpPoints());
    }

    @NotNull
    public static EnhanceData readData(@NotNull ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        int main = pdc.getOrDefault(MAIN_KEY, PersistentDataType.INTEGER, 0);
        Map<String, Integer> branches = getBranches(pdc, BRANCHES_KEY);
        int xp = pdc.getOrDefault(XP_KEY, PersistentDataType.INTEGER, 0);
        return new EnhanceData(main, branches, xp);
    }

    public static boolean isEnhanced(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().has(MAIN_KEY, PersistentDataType.INTEGER);
    }

    private static void setBranches(PersistentDataContainer pdc, NamespacedKey key, Map<String, Integer> branches) {
        if (branches.isEmpty()) {
            pdc.remove(key);
            return;
        }
        List<String> list = new ArrayList<>();
        for (var entry : branches.entrySet()) {
            list.add(entry.getKey() + ":" + entry.getValue());
        }
        pdc.set(key, PersistentDataType.LIST.strings(), list);
    }

    private static Map<String, Integer> getBranches(PersistentDataContainer pdc, NamespacedKey key) {
        List<String> list = pdc.get(key, PersistentDataType.LIST.strings());
        if (list == null || list.isEmpty()) return new LinkedHashMap<>();
        Map<String, Integer> result = new LinkedHashMap<>();
        for (String entry : list) {
            int sep = entry.lastIndexOf(':');
            if (sep < 1) continue;
            String type = entry.substring(0, sep);
            try {
                int level = Integer.parseInt(entry.substring(sep + 1));
                if (level > 0) result.put(type, level);
            } catch (NumberFormatException e) {
                HammrEnhance.getInstance().getLogger().log(Level.WARNING, "Failed to parse branch data entry: " + entry, e);
            }
        }
        return result;
    }

    private static void setInt(PersistentDataContainer pdc, NamespacedKey key, int value) {
        if (value > 0) {
            pdc.set(key, PersistentDataType.INTEGER, value);
        } else {
            pdc.remove(key);
        }
    }

    private PDCAdapter() {}
}
