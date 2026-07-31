package org.cubex.hammr.config;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.cubex.hammr.HammrEnhance;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigSettings {

    private int[] mainSuccessRates;
    private int[] branchSuccessRates;
    private int costGold;
    private int[] costGoldPerLevel;
    private double levelDownChance;
    private double explosionChance;
    private int mainMaxLevel;
    private int branchMaxLevel;
    private int branchMinMainLevel;
    private int xpMultiplier;
    private int blockBreakXp;
    private int mainMaterialThreshold;
    private boolean broadcastOnMaxLevel;
    private boolean soundEnabled;
    private float explosionRadius;
    private int xpFromExpOrb;
    private Material mainLowMaterial;
    private Material mainHighMaterial;
    private Material branchMaterial;

    // branch pools
    private Map<String, List<String>> branchPools;

    // main enchant mapping
    private Map<String, String> mainEnchantMapping;

    // progress bar
    private int progressBarWidth;
    private String progressBarFilledColor;
    private String progressBarEmptyColor;
    private String progressBarSuffixColor;
    private String progressBarChar;
    private String progressBarSuffixFormat;

    public ConfigSettings() {
        reload();
    }

    public void reload() {
        HammrEnhance plugin = HammrEnhance.getInstance();
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        FileConfiguration cfg = plugin.getConfig();

        mainSuccessRates = cfg.getIntegerList("main-success-rates")
                .stream().mapToInt(Integer::intValue).toArray();
        if (mainSuccessRates.length == 0) {
            mainSuccessRates = new int[]{95, 95, 95, 95, 95, 85, 75, 60, 40, 20};
        }

        branchSuccessRates = cfg.getIntegerList("branch-success-rates")
                .stream().mapToInt(Integer::intValue).toArray();
        if (branchSuccessRates.length == 0) {
            branchSuccessRates = new int[]{40, 35, 25, 15, 10};
        }

        costGold = cfg.getInt("cost-gold", 1000);

        List<Integer> perLevel = cfg.getIntegerList("cost-gold-per-level");
        costGoldPerLevel = perLevel.stream().mapToInt(Integer::intValue).toArray();

        levelDownChance = cfg.getDouble("level-down-chance", 0.25);
        explosionChance = cfg.getDouble("explosion-chance", 0.10);

        mainMaxLevel = cfg.getInt("main-max-level", 10);
        branchMaxLevel = cfg.getInt("branch-max-level", 6);
        branchMinMainLevel = cfg.getInt("branch-min-main-level", 8);
        xpMultiplier = cfg.getInt("xp-multiplier", 50);
        blockBreakXp = cfg.getInt("block-break-xp", 1);
        mainMaterialThreshold = cfg.getInt("main-material-threshold", 6);
        xpFromExpOrb = cfg.getInt("xp-from-exp-orb", 1);

        broadcastOnMaxLevel = cfg.getBoolean("broadcast-on-max-level", true);
        soundEnabled = cfg.getBoolean("sound-enabled", true);

        explosionRadius = (float) cfg.getDouble("explosion-radius", 1.0);

        String mainLow = cfg.getString("materials.main-low", "DIAMOND");
        String mainHigh = cfg.getString("materials.main-high", "NETHERITE_INGOT");
        String branch = cfg.getString("materials.branch", "DIAMOND");
        mainLowMaterial = Material.getMaterial(mainLow);
        mainHighMaterial = Material.getMaterial(mainHigh);
        branchMaterial = Material.getMaterial(branch);
        if (mainLowMaterial == null) mainLowMaterial = Material.DIAMOND;
        if (mainHighMaterial == null) mainHighMaterial = Material.NETHERITE_INGOT;
        if (branchMaterial == null) branchMaterial = Material.DIAMOND;

        // branch pools
        branchPools = new LinkedHashMap<>();
        ConfigurationSection poolsSection = cfg.getConfigurationSection("branch-pools");
        if (poolsSection != null) {
            for (String key : poolsSection.getKeys(false)) {
                List<String> pool = poolsSection.getStringList(key);
                if (!pool.isEmpty()) {
                    branchPools.put(key, pool);
                }
            }
        }
        if (branchPools.isEmpty()) {
            branchPools.put("SWORD", List.of("fire_aspect", "bane_of_arthropods", "smite"));
            branchPools.put("AXE", List.of("fire_aspect", "bane_of_arthropods", "smite"));
            branchPools.put("HELMET", List.of("projectile_protection", "fire_protection", "blast_protection"));
            branchPools.put("CHESTPLATE", List.of("projectile_protection", "fire_protection", "blast_protection"));
            branchPools.put("LEGGINGS", List.of("projectile_protection", "fire_protection", "blast_protection"));
            branchPools.put("BOOTS", List.of("projectile_protection", "fire_protection", "blast_protection"));
        }

        // main enchant mapping
        mainEnchantMapping = new LinkedHashMap<>();
        ConfigurationSection mappingSection = cfg.getConfigurationSection("main-enchant-mapping");
        if (mappingSection != null) {
            for (String key : mappingSection.getKeys(false)) {
                String value = mappingSection.getString(key);
                if (value != null && !value.isEmpty()) {
                    mainEnchantMapping.put(key, value);
                }
            }
        }
        if (mainEnchantMapping.isEmpty()) {
            mainEnchantMapping.put("SWORD", "sharpness");
            mainEnchantMapping.put("AXE", "sharpness");
            mainEnchantMapping.put("PICKAXE", "efficiency");
            mainEnchantMapping.put("SHOVEL", "efficiency");
            mainEnchantMapping.put("HOE", "efficiency");
            mainEnchantMapping.put("HELMET", "protection");
            mainEnchantMapping.put("CHESTPLATE", "protection");
            mainEnchantMapping.put("LEGGINGS", "protection");
            mainEnchantMapping.put("BOOTS", "protection");
        }

        // progress bar
        progressBarWidth = cfg.getInt("progress-bar.width", 25);
        progressBarFilledColor = cfg.getString("progress-bar.filled-color", "§a");
        progressBarEmptyColor = cfg.getString("progress-bar.empty-color", "§8");
        progressBarSuffixColor = cfg.getString("progress-bar.suffix-color", "§f");
        progressBarChar = cfg.getString("progress-bar.char", "|");
        progressBarSuffixFormat = cfg.getString("progress-bar.suffix-format", " %s%%");
    }

    // --- existing getters ---

    public int[] getMainSuccessRates() {
        return mainSuccessRates;
    }

    public int getMainSuccessRate(int level) {
        if (level < 1) return mainSuccessRates[0];
        if (level >= mainSuccessRates.length) return mainSuccessRates[mainSuccessRates.length - 1];
        return mainSuccessRates[level - 1];
    }

    public int[] getBranchSuccessRates() {
        return branchSuccessRates;
    }

    public int getBranchSuccessRate(int level) {
        if (level < 1) return branchSuccessRates[0];
        if (level >= branchSuccessRates.length) return branchSuccessRates[branchSuccessRates.length - 1];
        return branchSuccessRates[level - 1];
    }

    public int getCostGold(int level) {
        if (costGoldPerLevel != null && costGoldPerLevel.length > 0) {
            int idx = Math.min(level, costGoldPerLevel.length) - 1;
            if (idx < 0) idx = 0;
            return costGoldPerLevel[idx];
        }
        return costGold;
    }

    public int getCostGold() {
        return costGold;
    }

    public double getLevelDownChance() {
        return levelDownChance;
    }

    public double getExplosionChance() {
        return explosionChance;
    }

    public int getMainMaxLevel() {
        return mainMaxLevel;
    }

    public int getBranchMaxLevel() {
        return branchMaxLevel;
    }

    public int getBranchMinMainLevel() {
        return branchMinMainLevel;
    }

    public int getXpMultiplier() {
        return xpMultiplier;
    }

    public int getBlockBreakXp() {
        return blockBreakXp;
    }

    public int getMainMaterialThreshold() {
        return mainMaterialThreshold;
    }

    public int getXpFromExpOrb() {
        return xpFromExpOrb;
    }

    public boolean isBroadcastOnMaxLevel() {
        return broadcastOnMaxLevel;
    }

    public boolean isSoundEnabled() {
        return soundEnabled;
    }

    public float getExplosionRadius() {
        return explosionRadius;
    }

    public Material getMainLowMaterial() {
        return mainLowMaterial;
    }

    public Material getMainHighMaterial() {
        return mainHighMaterial;
    }

    public Material getBranchMaterial() {
        return branchMaterial;
    }

    public int getXpRequired(int mainLevel) {
        return mainLevel * xpMultiplier;
    }

    // --- branch pools ---

    public Map<String, List<String>> getBranchPools() {
        return branchPools;
    }

    public List<String> getBranchPool(String equipType) {
        return branchPools.get(equipType);
    }

    public boolean hasBranchPool(String equipType) {
        return branchPools.containsKey(equipType);
    }

    // --- main enchant mapping ---

    public Map<String, String> getMainEnchantMapping() {
        return mainEnchantMapping;
    }

    public String getMainEnchantKey(String equipType) {
        return mainEnchantMapping.get(equipType);
    }

    // --- progress bar ---

    public int getProgressBarWidth() {
        return progressBarWidth;
    }

    public String getProgressBarFilledColor() {
        return progressBarFilledColor;
    }

    public String getProgressBarEmptyColor() {
        return progressBarEmptyColor;
    }

    public String getProgressBarSuffixColor() {
        return progressBarSuffixColor;
    }

    public String getProgressBarChar() {
        return progressBarChar;
    }

    public String getProgressBarSuffixFormat() {
        return progressBarSuffixFormat;
    }
}
