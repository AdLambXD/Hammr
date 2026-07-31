package org.cubex.hammr.util;

import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.cubex.hammr.HammrEnhance;
import org.cubex.hammr.storage.EnhanceData;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;

public final class LoreBuilder {

    public static java.util.List<Component> buildLore(EnhanceData data) {
        var lore = new java.util.ArrayList<Component>();
        var settings = HammrEnhance.getInstance().getSettings();
        StringBuilder sb = new StringBuilder();

        String levelTag = "[+" + data.mainLevel();
        if (data.branchLevel() > 0) {
            levelTag += "(" + data.branchLevel() + ")";
        }
        levelTag += "]";
        sb.append("§6§l").append(levelTag);
        lore.add(Component.text(sb.toString()));

        if (data.mainLevel() < settings.getMainMaxLevel()) {
            StringBuilder bar = new StringBuilder();
            double pct = data.xpProgress();
            int width = settings.getProgressBarWidth();
            int filled = (int) Math.round(pct * width);
            String c = settings.getProgressBarChar();

            bar.append(settings.getProgressBarFilledColor());
            for (int i = 0; i < filled && i < width; i++) bar.append(c);
            bar.append(settings.getProgressBarEmptyColor());
            for (int i = filled; i < width; i++) bar.append(c);
            bar.append(settings.getProgressBarSuffixColor());
            bar.append(String.format(settings.getProgressBarSuffixFormat(), Math.round(pct * 100)));
            lore.add(Component.text(bar.toString()));
        }

        return lore;
    }

    public static Enchantment getMainEnchant(org.bukkit.Material material) {
        String equipType = ItemChecker.getEquipType(material);
        String key = HammrEnhance.getInstance().getSettings().getMainEnchantKey(equipType);
        if (key == null || key.isEmpty()) return null;
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(NamespacedKey.minecraft(key));
    }

    public static String getEnchantDisplayName(Enchantment ench) {
        String key = ench.getKey().getKey();
        return HammrEnhance.getInstance().getMessages().get("enchant-name." + key, key);
    }

    private LoreBuilder() {}
}
