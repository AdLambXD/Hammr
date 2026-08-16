package org.cubex.hammr.economy;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.cubex.hammr.HammrEnhance;

import java.util.logging.Level;

public class EconomyManager {

    private Economy economy;

    // 收入账户解析结果缓存：Bukkit.getOfflinePlayer(String) 对未缓存的名字会发起
    // 阻塞式 UUID 查询，绝不能每次强化都在主线程上跑一次
    private volatile String incomeAccountName;
    private volatile OfflinePlayer incomeAccount;

    public EconomyManager() {
        resolveProvider();
    }

    /**
     * Vault 属于软依赖，缺失时不能让插件启用失败；
     * 经济插件晚于本插件注册服务时也需要能补上，否则会永久跳过扣费。
     */
    private void resolveProvider() {
        HammrEnhance plugin = HammrEnhance.getInstance();
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) return;
        try {
            RegisteredServiceProvider<Economy> rsp = plugin.getServer()
                    .getServicesManager().getRegistration(Economy.class);
            if (rsp != null) economy = rsp.getProvider();
        } catch (LinkageError e) {
            plugin.getLogger().log(Level.WARNING, "Vault economy API is unavailable", e);
        }
    }

    public boolean isEnabled() {
        if (economy == null) resolveProvider();
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

    public boolean depositToAccount(String accountName, double amount) {
        if (!isEnabled() || accountName == null || accountName.isBlank()) return false;
        OfflinePlayer account = resolveAccount(accountName);
        if (account == null) return false;
        return economy.depositPlayer(account, amount).transactionSuccess();
    }

    /** 启用与 /hammr reload 时异步预热收入账户，避免主线程等待 UUID 查询 */
    public void prepareIncomeAccount(String accountName) {
        if (accountName == null || accountName.isBlank()) {
            incomeAccountName = null;
            incomeAccount = null;
            return;
        }
        if (accountName.equals(incomeAccountName) && incomeAccount != null) return;

        HammrEnhance plugin = HammrEnhance.getInstance();
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer resolved = Bukkit.getOfflinePlayer(accountName);
            incomeAccount = resolved;
            incomeAccountName = accountName;
        });
    }

    @SuppressWarnings("deprecation")
    private OfflinePlayer resolveAccount(String accountName) {
        OfflinePlayer cached = incomeAccount;
        if (cached != null && accountName.equals(incomeAccountName)) return cached;

        // 预热尚未完成(刚启用/刚改配置)时的兜底，解析结果同样写入缓存
        OfflinePlayer resolved = Bukkit.getOfflinePlayer(accountName);
        incomeAccount = resolved;
        incomeAccountName = accountName;
        return resolved;
    }
}
