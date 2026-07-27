package org.cubex.hammr.storage;

public record EnhanceData(int mainLevel, int branchLevel, String branchType) {

    public static final EnhanceData EMPTY = new EnhanceData(0, 0, null);

    public boolean hasMain() {
        return mainLevel > 0;
    }

    public boolean hasBranch() {
        return branchLevel > 0 && branchType != null;
    }

    public boolean isMainMaxed() {
        return mainLevel >= 10;
    }

    public boolean isBranchMaxed() {
        return branchLevel >= 6;
    }

    public boolean canBranch() {
        return mainLevel >= 8 && branchLevel < 6;
    }
}
