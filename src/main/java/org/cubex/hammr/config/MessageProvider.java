package org.cubex.hammr.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.cubex.hammr.HammrEnhance;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.logging.Level;

public class MessageProvider {

    private FileConfiguration messages;

    public void reload() {
        HammrEnhance plugin = HammrEnhance.getInstance();
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(file);

        // 以 jar 内的 messages.yml 作为兜底，老版本配置文件缺少新增键时不会显示 <missing:...>
        try (InputStream defaults = plugin.getResource("messages.yml")) {
            if (defaults != null) {
                messages.setDefaults(YamlConfiguration.loadConfiguration(
                        new InputStreamReader(defaults, StandardCharsets.UTF_8)));
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load bundled messages.yml defaults", e);
        }
    }

    public String get(String key) {
        String value = messages.getString(key);
        return value != null ? value : "<missing:" + key + ">";
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
