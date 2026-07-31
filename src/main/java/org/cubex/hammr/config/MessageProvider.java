package org.cubex.hammr.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.cubex.hammr.HammrEnhance;

import java.io.File;
import java.text.MessageFormat;

public class MessageProvider {

    private FileConfiguration messages;

    public void reload() {
        HammrEnhance plugin = HammrEnhance.getInstance();
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public String get(String key) {
        return messages.getString(key, "<missing:" + key + ">");
    }

    public String get(String key, Object... args) {
        String template = get(key);
        if (args.length == 0) return template;
        return MessageFormat.format(template, args);
    }

    public Component getComponent(String key, Object... args) {
        return Component.text(get(key, args));
    }

    public Component getComponent(String key, NamedTextColor color, Object... args) {
        return Component.text(get(key, args), color);
    }

    public Component getComponent(String key, NamedTextColor color, TextDecoration decoration, Object... args) {
        return Component.text(get(key, args), color, decoration);
    }
}
