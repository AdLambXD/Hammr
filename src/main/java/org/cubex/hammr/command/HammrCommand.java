package org.cubex.hammr.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.cubex.hammr.HammrEnhance;
import org.cubex.hammr.config.MessageProvider;
import org.cubex.hammr.config.ConfigSettings;
import org.cubex.hammr.enhancement.BranchPool;
import org.cubex.hammr.enhancement.EnhanceManager;
import org.cubex.hammr.storage.EnhanceData;
import org.cubex.hammr.storage.PDCAdapter;
import org.cubex.hammr.util.ItemChecker;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HammrCommand implements TabExecutor {

    private static final String PERM_SET = "hammr.command.set";
    private static final String PERM_REMOVE = "hammr.command.remove";
    private static final String PERM_GIVE = "hammr.command.give";
    private static final String PERM_RELOAD = "hammr.command.reload";
    private static final List<String> ALL_PERMS = List.of(PERM_SET, PERM_REMOVE, PERM_GIVE, PERM_RELOAD);

    private MessageProvider msg() {
        return HammrEnhance.getInstance().getMessages();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendUsageOrDeny(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> { if (checkPermission(sender, PERM_SET)) handleSet(sender, args); }
            case "remove" -> { if (checkPermission(sender, PERM_REMOVE)) handleRemove(sender); }
            case "give" -> { if (checkPermission(sender, PERM_GIVE)) handleGive(sender, args); }
            case "reload" -> { if (checkPermission(sender, PERM_RELOAD)) handleReload(sender); }
            default -> sendUsageOrDeny(sender);
        }
        return true;
    }

    private void sendUsageOrDeny(CommandSender sender) {
        if (ALL_PERMS.stream().noneMatch(sender::hasPermission)) {
            denyPermission(sender);
            return;
        }
        sendUsage(sender);
    }

    private boolean checkPermission(CommandSender sender, String permission) {
        if (sender.hasPermission(permission)) return true;
        denyPermission(sender);
        return false;
    }

    private void denyPermission(CommandSender sender) {
        sender.sendMessage(Component.text(msg().get("command.no-permission"), NamedTextColor.RED));
    }

    private void handleSet(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(msg().get("command.player-only"), NamedTextColor.RED));
            return;
        }
        if (args.length < 2) {
            sender.sendMessage(Component.text(msg().get("command.usage-set"), NamedTextColor.RED));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sender.sendMessage(Component.text(msg().get("command.need-item"), NamedTextColor.RED));
            return;
        }
        if (!ItemChecker.isNetheriteEquipment(item)) {
            sender.sendMessage(Component.text(msg().get("command.not-enhanceable"), NamedTextColor.RED));
            return;
        }

        int mainMax = HammrEnhance.getInstance().getSettings().getMainMaxLevel();
        int branchMax = HammrEnhance.getInstance().getSettings().getBranchMaxLevel();
        int mainLevel;
        try {
            mainLevel = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text(msg().get("command.invalid-main-level", mainMax), NamedTextColor.RED));
            return;
        }
        if (mainLevel < 0 || mainLevel > mainMax) {
            sender.sendMessage(Component.text(msg().get("command.main-level-range", mainMax), NamedTextColor.RED));
            return;
        }

        int branchLevel = 0;
        String branchType = null;
        if (args.length >= 3) {
            branchType = args[2];
            if (!branchType.equals("random") && BranchPool.toEnchantment(branchType) == null) {
                sender.sendMessage(Component.text(msg().get("command.invalid-branch-type"), NamedTextColor.RED));
                return;
            }
            if (args.length >= 4) {
                try {
                    branchLevel = Integer.parseInt(args[3]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text(msg().get("command.invalid-branch-level", branchMax), NamedTextColor.RED));
                    return;
                }
                if (branchLevel < 0 || branchLevel > branchMax) {
                    sender.sendMessage(Component.text(msg().get("command.branch-level-range", branchMax), NamedTextColor.RED));
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
                sender.sendMessage(Component.text(msg().get("command.no-branch-available"), NamedTextColor.RED));
                return;
            }
            if (branchLevel <= 0) branchLevel = 1;
        }

        applyEnchants(item, mainLevel, branchType, branchLevel);

        String branchInfo = branchType != null ? ", 分支=" + branchType + " " + branchLevel : "";
        sender.sendMessage(Component.text(msg().get("command.set-success", mainLevel, branchInfo), NamedTextColor.GREEN));
    }

    private void handleRemove(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text(msg().get("command.player-only"), NamedTextColor.RED));
            return;
        }

        ItemStack item = player.getInventory().getItemInMainHand();
        if (item.getType() == Material.AIR) {
            sender.sendMessage(Component.text(msg().get("command.remove-no-item"), NamedTextColor.RED));
            return;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        if (!PDCAdapter.isEnhanced(meta)) {
            sender.sendMessage(Component.text(msg().get("command.remove-no-data"), NamedTextColor.RED));
            return;
        }

        EnhanceManager.writeItem(meta, item.getType(), EnhanceData.EMPTY);
        item.setItemMeta(meta);

        sender.sendMessage(Component.text(msg().get("command.remove-success"), NamedTextColor.GREEN));
    }

    private void handleGive(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text(msg().get("command.usage-give"), NamedTextColor.RED));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(Component.text(msg().get("command.give-player-offline", args[1]), NamedTextColor.RED));
            return;
        }

        int mainMax = HammrEnhance.getInstance().getSettings().getMainMaxLevel();
        int branchMax = HammrEnhance.getInstance().getSettings().getBranchMaxLevel();
        int mainLevel = 0;
        if (args.length >= 3) {
            try {
                mainLevel = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text(msg().get("command.invalid-main-level", mainMax), NamedTextColor.RED));
                return;
            }
            if (mainLevel < 0 || mainLevel > mainMax) {
                sender.sendMessage(Component.text(msg().get("command.main-level-range", mainMax), NamedTextColor.RED));
                return;
            }
        }

        int branchLevel = 0;
        String branchType = null;
        if (args.length >= 4) {
            branchType = args[3];
            if (!branchType.equals("random") && BranchPool.toEnchantment(branchType) == null) {
                sender.sendMessage(Component.text(msg().get("command.invalid-branch-type"), NamedTextColor.RED));
                return;
            }
            if (args.length >= 5) {
                try {
                    branchLevel = Integer.parseInt(args[4]);
                } catch (NumberFormatException e) {
                    sender.sendMessage(Component.text(msg().get("command.invalid-branch-level", branchMax), NamedTextColor.RED));
                    return;
                }
                if (branchLevel < 0 || branchLevel > branchMax) {
                    sender.sendMessage(Component.text(msg().get("command.branch-level-range", branchMax), NamedTextColor.RED));
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
                sender.sendMessage(Component.text(msg().get("command.give-success", target.getName()), NamedTextColor.GREEN));
                return;
            }
        }
        sender.sendMessage(Component.text(msg().get("command.give-need-template"), NamedTextColor.RED));
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

        Map<String, Integer> branches = new LinkedHashMap<>();
        if (branchType != null && branchLevel > 0) {
            branches.put(branchType, branchLevel);
        }

        EnhanceManager.writeItem(meta, item.getType(), new EnhanceData(mainLevel, branches));
        item.setItemMeta(meta);
    }

    private void handleReload(CommandSender sender) {
        HammrEnhance plugin = HammrEnhance.getInstance();
        plugin.reloadConfig();
        plugin.getSettings().reload();
        plugin.getMessages().reload();
        plugin.getEconomyManager().prepareIncomeAccount(plugin.getSettings().getIncomeAccount());
        sender.sendMessage(Component.text(msg().get("command.reload-success"), NamedTextColor.GREEN));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(Component.text(msg().get("command.help-header")));
        sender.sendMessage(Component.text(msg().get("command.help-set")));
        sender.sendMessage(Component.text(msg().get("command.help-remove")));
        sender.sendMessage(Component.text(msg().get("command.help-give")));
        sender.sendMessage(Component.text(msg().get("command.help-reload")));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        var settings = HammrEnhance.getInstance().getSettings();
        if (args.length == 1) {
            if (sender.hasPermission(PERM_SET)) completions.add("set");
            if (sender.hasPermission(PERM_REMOVE)) completions.add("remove");
            if (sender.hasPermission(PERM_GIVE)) completions.add("give");
            if (sender.hasPermission(PERM_RELOAD)) completions.add("reload");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("give")) {
            return null;
        } else if (args.length == 3 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("give"))) {
            for (int i = 0; i <= settings.getMainMaxLevel(); i++) {
                completions.add(String.valueOf(i));
            }
        } else if (args.length == 4 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("give"))) {
            addPoolCompletions(completions, settings, "SWORD");
            addPoolCompletions(completions, settings, "AXE");
            addPoolCompletions(completions, settings, "HELMET");
            completions.add("random");
        } else if (args.length == 5 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("give"))) {
            for (int i = 0; i <= settings.getBranchMaxLevel(); i++) {
                completions.add(String.valueOf(i));
            }
        }
        return completions;
    }

    /** 配置中该装备类型的分支池可能被清空(跳过空池)，此时 getBranchPool 返回 null，需判空 */
    private void addPoolCompletions(List<String> completions, ConfigSettings settings, String type) {
        List<String> pool = settings.getBranchPool(type);
        if (pool != null) {
            completions.addAll(pool);
        }
    }
}
