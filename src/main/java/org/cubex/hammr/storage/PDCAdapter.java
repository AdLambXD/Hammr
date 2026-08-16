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
    /** 首次锻造时物品自带的原版附魔等级快照，强化等级叠加在它之上 */
    private static final NamespacedKey BASE_KEY = key("enhance_base_enchants");

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

    /**
     * 该物品是否被本插件锻造过。三个键任意存在即视为有强化数据，
     * 仅凭原版附魔(附魔台/村民/指令)不算。
     * 原版附魔快照不参与判定：它只是伴生数据，不代表物品被强化过。
     */
    public static boolean isEnhanced(@NotNull ItemMeta meta) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        return pdc.has(MAIN_KEY, PersistentDataType.INTEGER)
                || pdc.has(BRANCHES_KEY, PersistentDataType.LIST.strings())
                || pdc.has(XP_KEY, PersistentDataType.INTEGER);
    }

    /** 读取物品自带的原版附魔等级快照，键为附魔 ID */
    @NotNull
    public static Map<String, Integer> readBaseEnchants(@NotNull ItemMeta meta) {
        return getBranches(meta.getPersistentDataContainer(), BASE_KEY);
    }

    /** 快照是否已记录。空快照(物品原本没有附魔)也要留键，否则每次写入都会重新推断 */
    public static boolean hasBaseEnchants(@NotNull ItemMeta meta) {
        return meta.getPersistentDataContainer().has(BASE_KEY, PersistentDataType.LIST.strings());
    }

    public static void writeBaseEnchants(@NotNull ItemMeta meta, @NotNull Map<String, Integer> bases) {
        List<String> list = new ArrayList<>();
        for (var entry : bases.entrySet()) {
            list.add(entry.getKey() + ":" + entry.getValue());
        }
        meta.getPersistentDataContainer().set(BASE_KEY, PersistentDataType.LIST.strings(), list);
    }

    /** 强化归零后物品回到原版状态，快照不再需要保留 */
    public static void clearBaseEnchants(@NotNull ItemMeta meta) {
        meta.getPersistentDataContainer().remove(BASE_KEY);
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
