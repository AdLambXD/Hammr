package org.cubex.hammr;

import org.bukkit.plugin.java.JavaPlugin;
import org.cubex.hammr.command.HammrCommand;
import org.cubex.hammr.config.ConfigSettings;
import org.cubex.hammr.config.MessageProvider;
import org.cubex.hammr.economy.EconomyManager;
import org.cubex.hammr.listener.AnvilListener;
import org.cubex.hammr.listener.XpListener;

public class HammrEnhance extends JavaPlugin {

    private static HammrEnhance instance;
    private EconomyManager economyManager;
    private ConfigSettings configSettings;
    private MessageProvider messageProvider;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        configSettings = new ConfigSettings();
        messageProvider = new MessageProvider();
        messageProvider.reload();

        economyManager = new EconomyManager();
        if (!economyManager.isEnabled()) {
            getLogger().warning(getMessages().get("log.vault-not-found"));
        }

        getServer().getPluginManager().registerEvents(new AnvilListener(), this);
        getServer().getPluginManager().registerEvents(new XpListener(), this);
        getCommand("hammr").setExecutor(new HammrCommand());
        getLogger().info(getMessages().get("log.enabled"));
    }

    @Override
    public void onDisable() {
        getLogger().info(getMessages().get("log.disabled"));
    }

    public static HammrEnhance getInstance() {
        return instance;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public ConfigSettings getSettings() {
        return configSettings;
    }

    public MessageProvider getMessages() {
        return messageProvider;
    }
}
