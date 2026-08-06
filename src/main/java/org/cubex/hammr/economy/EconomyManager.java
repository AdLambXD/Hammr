package org.cubex.hammr.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.cubex.hammr.HammrEnhance;

public class EconomyManager {

    private Economy economy;

    public EconomyManager() {
        var rsp = HammrEnhance.getInstance()
                .getServer().getServicesManager()
                .getRegistration(Economy.class);
        if (rsp != null) {
            economy = rsp.getProvider();
        }
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public boolean hasBalance(Player player, double amount) {
        return isEnabled() && economy.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        return isEnabled() && economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    @SuppressWarnings("unused")
    public void deposit(Player player, double amount) {
        if (isEnabled()) {
            economy.depositPlayer(player, amount);
        }
    }

    @SuppressWarnings("deprecation")
    public boolean depositToAccount(String accountName, double amount) {
        if (!isEnabled() || accountName == null || accountName.isBlank()) return false;
        return economy.depositPlayer(Bukkit.getOfflinePlayer(accountName), amount).transactionSuccess();
    }
}
