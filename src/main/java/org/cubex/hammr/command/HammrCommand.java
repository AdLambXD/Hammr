package org.cubex.hammr.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.cubex.hammr.HammrEnhance;
import org.cubex.hammr.enhancement.BranchPool;
import org.cubex.hammr.storage.EnhanceData;
import org.cubex.hammr.storage.PDCAdapter;
import org.cubex.hammr.util.ItemChecker;
import org.cubex.hammr.util.LoreBuilder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HammrCommand implements TabExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> handleSet(sender, args);
            case "remove" -> handleRemove(sender);
            case "give" -> handleGive(sender, args);
            case "reload" -> handleReload(sender);
            default -> sendUsage(sender);
        }
        return true;
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("只有玩家可以使用此命令。", NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /hammr set <主等级> [分支类型] [分支等级]", NamedTextColor.RED));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sender.sendMessage(Component.text("请手持一件下界合金装备。", NamedTextColor.RED));
            return;
        }
        if (!ItemChecker.isNetheriteEquipment(item)) {
            sender.sendMessage(Component.text("该物品不是可强化的下界合金装备。", NamedTextColor.RED));
            return;
        }

        int mainLevel;
        try {
            mainLevel = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("主等级必须为数字 (0-10)。", NamedTextColor.RED));
            return;
        }
        if (mainLevel < 0 || mainLevel > 10) {
            sender.sendMessage(Component.text("主等级范围为 0-10。", NamedTextColor.RED));
            return;
        }

        int branchLevel = 0;
        String branchType = null;
        if (args.length >= 3) {
            branchType = args[2];
            if (!branchType.equals("random") && BranchPool.toEnchantment(branchType) == null) {
                sender.sendMessage(Component.text("无效的分支类型。可选: fire_aspect, bane_of_arthropods, smite, projectile_protection, fire_protection, blast_protection", NamedTextColor.RED));
                return;
            }
            if (args.length >= 4) {
                try {
                    branchLevel = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("分支等级必须为数字 (0-6)。", NamedTextColor.RED));
                    return;
                }
                if (branchLevel < 0 || branchLevel > 6) {
                    sender.sendMessage(Component.text("分支等级范围为 0-6。", NamedTextColor.RED));
                    return;
                }
            } else {
                branchLevel = 1;
            }
        }

        if ("random".equals(branchType)) {
            String equipType = ItemChecker.getEquipType(item);
            branchType = BranchPool.random(equipType);
            if (branchType == null) {
                sender.sendMessage(Component.text("该装备没有可用的分支类型。", NamedTextColor.RED));
                return;
            }
            if (branchLevel <= 0) branchLevel = 1;
        }

        applyEnchants(item, mainLevel, branchType, branchLevel);

        sender.sendMessage(Component.text("已设置强化: 主=" + mainLevel + (branchType != null ? ", 分支=" + branchType + " " + branchLevel : ""), NamedTextColor.GREEN));
    }

    private void handleRemove(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("只有玩家可以使用此命令。", NamedTextColor.RED));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sender.sendMessage(Component.text("请手持一件物品。", NamedTextColor.RED));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (!PDCAdapter.isEnhanced(meta)) {
            sender.sendMessage(Component.text("该物品没有强化数据。", NamedTextColor.RED));
            return;
        }

        EnhanceData data = PDCAdapter.readData(meta);
        Enchantment mainEnch = LoreBuilder.getMainEnchant(item.getType());
        if (mainEnch != null && data.mainLevel() > 0) {
            meta.removeEnchant(mainEnch);
        }
        for (var entry : data.branches().entrySet()) {
            Enchantment branchEnch = BranchPool.toEnchantment(entry.getKey());
            if (branchEnch != null) {
                meta.removeEnchant(branchEnch);
            }
        }

        PDCAdapter.writeData(meta, EnhanceData.EMPTY);
        meta.lore(LoreBuilder.buildLore(EnhanceData.EMPTY));
        item.setItemMeta(meta);

        sender.sendMessage(Component.text("已移除所有强化。", NamedTextColor.GREEN));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("用法: /hammr give <玩家> <主等级> [分支类型] [分支等级]", NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text("玩家 " + args[1] + " 不在线。", NamedTextColor.RED));
            return;
        }

        int mainLevel = 0;
        if (args.length >= 3) {
            try {
                mainLevel = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("主等级必须为数字 (0-10)。", NamedTextColor.RED));
                return;
            }
            if (mainLevel < 0 || mainLevel > 10) {
                sender.sendMessage(Component.text("主等级范围为 0-10。", NamedTextColor.RED));
                return;
            }
        }

        int branchLevel = 0;
        String branchType = null;
        if (args.length >= 4) {
            branchType = args[3];
            if (!branchType.equals("random") && BranchPool.toEnchantment(branchType) == null) {
                sender.sendMessage(Component.text("无效的分支类型。", NamedTextColor.RED));
                return;
            }
            if (args.length >= 5) {
                try {
                    branchLevel = Integer.parseInt(args[4]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text("分支等级必须为数字 (0-6)。", NamedTextColor.RED));
                    return;
                }
                if (branchLevel < 0 || branchLevel > 6) {
                    sender.sendMessage(Component.text("分支等级范围为 0-6。", NamedTextColor.RED));
                    return;
                }
            } else {
                branchLevel = 1;
            }
        }

        if (sender instanceof Player player) {
            ItemStack held = player.getInventory().getItemInMainHand();
            if (held.getType() != Material.AIR && ItemChecker.isNetheriteEquipment(held)) {
                giveEnhancedItem(target, held, mainLevel, branchType, branchLevel);
                sender.sendMessage(Component.text("已给予 " + target.getName() + " 强化装备。", NamedTextColor.GREEN));
                return;
            }
        }
        sender.sendMessage(Component.text("请手持一个样板装备来给予。", NamedTextColor.RED));
    }

    private void giveEnhancedItem(Player target, ItemStack template, int mainLevel, String branchType, int branchLevel) {
        ItemStack item = template.clone();
        applyEnchants(item, mainLevel, branchType, branchLevel);
        var leftover = target.getInventory().addItem(item);
        if (!leftover.isEmpty()) {
            target.getWorld().dropItemNaturally(target.getLocation(), leftover.get(0));
        }
    }

    private void applyEnchants(ItemStack item, int mainLevel, String branchType, int branchLevel) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        Enchantment mainEnch = LoreBuilder.getMainEnchant(item.getType());
        if (mainEnch != null) {
            if (mainLevel > 0) {
                meta.addEnchant(mainEnch, mainLevel, true);
            } else {
                meta.removeEnchant(mainEnch);
            }
        }

        Map<String, Integer> branches = new LinkedHashMap<>();
        if (branchType != null && branchLevel > 0) {
            branches.put(branchType, branchLevel);
        }

        String equipType = ItemChecker.getEquipType(item);
        List<String> poolKeys = BranchPool.getPoolKeys(equipType);
        if (poolKeys != null) {
            for (String key : poolKeys) {
                Enchantment ench = BranchPool.toEnchantment(key);
                if (ench != null) meta.removeEnchant(ench);
            }
        }
        Enchantment newEnch = branchType != null ? BranchPool.toEnchantment(branchType) : null;
        if (newEnch != null && branchLevel > 0) {
            meta.addEnchant(newEnch, branchLevel, true);
        }

        EnhanceData data = new EnhanceData(mainLevel, branches);
        PDCAdapter.writeData(meta, data);
        meta.lore(LoreBuilder.buildLore(data));
        item.setItemMeta(meta);
    }

    private void handleReload(CommandSender sender) {
        HammrEnhance.getInstance().reloadConfig();
        sender.sendMessage(Component.text("配置已重新加载。", NamedTextColor.GREEN));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text("§6===== HammrEnhance 管理命令 ====="));
        sender.sendMessage(Component.text("§e/hammr set <主等级> [分支类型] [分支等级] §7- 设置手持物品的强化"));
        sender.sendMessage(Component.text("§e/hammr remove §7- 移除手持物品的所有强化"));
        sender.sendMessage(Component.text("§e/hammr give <玩家> <主等级> [分支类型] [分支等级] §7- 给予强化装备"));
        sender.sendMessage(Component.text("§e/hammr reload §7- 重新加载配置"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.addAll(List.of("set", "remove", "give", "reload"));
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return null;
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("give"))) {
            for (int i = 0; i <= 10; i++) {
                completions.add(String.valueOf(i));
            }
        } else if (args.length == 4 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("give"))) {
            completions.addAll(List.of("fire_aspect", "bane_of_arthropods", "smite", "projectile_protection", "fire_protection", "blast_protection", "random"));
        } else if (args.length == 5 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("give"))) {
            for (int i = 0; i <= 6; i++) {
                completions.add(String.valueOf(i));
            }
        }
        return completions;
    }
}
