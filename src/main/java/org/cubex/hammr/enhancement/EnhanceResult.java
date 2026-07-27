package org.cubex.hammr.enhancement;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public record EnhanceResult(
        boolean success,
        boolean levelDown,
        boolean exploded,
        int newMainLevel,
        int newBranchLevel,
        @Nullable String message,
        @Nullable ItemStack enhancedItem
) {

    public static EnhanceResult success(ItemStack item, int mainLevel, int branchLevel) {
        return new EnhanceResult(true, false, false, mainLevel, branchLevel, null, item);
    }

    public static EnhanceResult failure(boolean levelDown, boolean exploded,
                                         int mainLevel, int branchLevel,
                                         @Nullable ItemStack item) {
        return new EnhanceResult(false, levelDown, exploded, mainLevel, branchLevel, null, item);
    }

    public static EnhanceResult error(String message) {
        return new EnhanceResult(false, false, false, 0, 0, message, null);
    }
}
