package org.cubex.hammr.storage;

import org.cubex.hammr.HammrEnhance;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record EnhanceData(int mainLevel, Map<String, Integer> branches, int xpPoints) {

    public static final EnhanceData EMPTY = new EnhanceData(0, Collections.emptyMap());

    public EnhanceData(int mainLevel, Map<String, Integer> branches) {
        this(mainLevel, branches, 0);
    }

    public EnhanceData(int mainLevel, int branchLevel, String branchType) {
        this(mainLevel, branchType != null && branchLevel > 0
                ? Map.of(branchType, branchLevel)
                : Collections.emptyMap());
    }

    public EnhanceData(int mainLevel, int branchLevel, String branchType, int xpPoints) {
        this(mainLevel, branchType != null && branchLevel > 0
                ? Map.of(branchType, branchLevel)
                : Collections.emptyMap(), xpPoints);
    }

    public int xpRequired() {
        return HammrEnhance.getInstance().getSettings().getXpRequired(mainLevel);
    }

    public double xpProgress() {
        int req = xpRequired();
        if (req <= 0) return 1.0;
        return Math.min(1.0, (double) xpPoints / req);
    }

    public int branchLevel() {
        return branches.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int branchCount() {
        return branches.size();
    }

    public boolean hasMain() {
        return mainLevel > 0;
    }

    public boolean isMainMaxed() {
        return mainLevel >= HammrEnhance.getInstance().getSettings().getMainMaxLevel();
    }

    public boolean hasBranch() {
        return !branches.isEmpty();
    }

    public boolean isBranchMaxed() {
        return branchLevel() >= HammrEnhance.getInstance().getSettings().getBranchMaxLevel();
    }

    public boolean canBranch() {
        var s = HammrEnhance.getInstance().getSettings();
        return mainLevel >= s.getBranchMinMainLevel() && branchLevel() < s.getBranchMaxLevel();
    }

    public EnhanceData withXP(int newXp) {
        return new EnhanceData(mainLevel, branches, newXp);
    }

    public EnhanceData withBranch(String type, int level) {
        Map<String, Integer> next = new LinkedHashMap<>(branches);
        next.put(type, level);
        return new EnhanceData(mainLevel, next, xpPoints);
    }

}
