package org.cubex.hammr;

import org.bukkit.plugin.java.JavaPlugin;
import org.cubex.hammr.command.HammrCommand;
import org.cubex.hammr.economy.EconomyManager;
import org.cubex.hammr.listener.AnvilListener;

public class HammrEnhance extends JavaPlugin {

    private static HammrEnhance instance;
    private EconomyManager economyManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        economyManager = new EconomyManager();
        if (!economyManager.isEnabled()) {
            getLogger().warning("Vault not found — gold costs will be skipped.");
        }

        getServer().getPluginManager().registerEvents(new AnvilListener(), this);
        getCommand("hammr").setExecutor(new HammrCommand());
        getLogger().info("HammrEnhance enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("HammrEnhance disabled.");
    }

    public static HammrEnhance getInstance() {
        return instance;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }
}
