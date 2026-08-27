package org.cubex.hammr.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.cubex.hammr.HammrEnhance;
import org.cubex.hammr.config.ConfigSettings;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EconomyManager {

    private Economy economy;

    /** 收入账户解析结果缓存：名字 → 玩家的解析每次强化都要做一次，不该每次都重查 */
    private record ResolvedAccount(String name, OfflinePlayer player) {}

    private volatile ResolvedAccount resolvedIncome;

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

    // ------------------------------------------------------------------
    // 强化收入转账
    // ------------------------------------------------------------------

    /**
     * 把玩家已扣除的强化费用转入配置的收入账户。
     * 转账失败不回滚：扣款已成功、强化流程也已继续，这里只能把丢失的金额写进日志供管理员对账。
     *
     * @return true 表示钱确实进了目标账户
     */
    public boolean transferIncome(Player payer, double amount) {
        if (!Double.isFinite(amount)) return false; // NaN <= 0 为 false，必须单独挡掉
        if (amount <= 0) return true;

        ConfigSettings settings = HammrEnhance.getInstance().getSettings();
        String account = settings.getIncomeAccount();
        if (account == null || account.isBlank()) return true; // 留空 = 纯金币回收，不转账

        IncomeAccountType type = settings.getIncomeAccountType();
        boolean ok = switch (type) {
            case COMMAND -> dispatchIncomeCommand(payer, account, amount);
            case BANK -> depositToBank(account, amount);
            case PLAYER -> depositToPlayer(account, amount);
            case AUTO -> depositToAuto(account, amount);
        };

        if (!ok) {
            logger().warning(msg("log.income-transfer-lost",
                    formatAmount(amount), account, payer == null ? "?" : payer.getName()));
        }
        return ok;
    }

    /** AUTO：玩家账户优先，只有玩家账户查不到、同名银行账户确实存在时才转银行 */
    private boolean depositToAuto(String account, double amount) {
        if (resolveAccount(account) != null || hasPlayerAccount(account)) {
            return depositToPlayer(account, amount);
        }
        if (bankExists(account)) return depositToBank(account, amount);

        // 两边都查不到时仍按玩家账户走一次：部分经济插件根本没实现 hasAccount，
        // 与其在这里放弃，不如让经济插件自己去解析这个名字
        return depositToPlayer(account, amount);
    }

    /**
     * 玩家账户转账：在线玩家直接给，离线玩家按服务端已知的真实 UUID 给（等价于 cmi pay 的效果）。
     * 服务端缓存里没有这个名字时不自己造 UUID，而是改用 Vault 的名字接口，
     * 由经济插件按它自己的用户库去解析 —— 外置登录(LittleSkin 等)的 UUID 只有服务端和经济插件
     * 知道，本插件无从推算。
     */
    private boolean depositToPlayer(String account, double amount) {
        if (!isEnabled()) return false;

        OfflinePlayer target = resolveAccount(account);
        EconomyResponse response = target != null
                ? economy.depositPlayer(target, amount)
                : depositByName(account, amount);

        if (response == null || !response.transactionSuccess()) {
            logger().warning(msg("log.income-transfer-failed", formatAmount(amount), account,
                    response == null ? "no response" : String.valueOf(response.errorMessage)));
            return false;
        }
        return true;
    }

    /** Vault 的名字接口虽已废弃，却是唯一能让经济插件自行解析账户的通道 */
    @SuppressWarnings("deprecation")
    private EconomyResponse depositByName(String account, double amount) {
        try {
            return economy.depositPlayer(account, amount);
        } catch (Exception | LinkageError e) {
            logger().log(Level.WARNING, msg("log.income-transfer-failed",
                    formatAmount(amount), account, e.toString()));
            return null;
        }
    }

    private boolean depositToBank(String account, double amount) {
        if (!isEnabled()) return false;
        if (!safeHasBankSupport()) {
            logger().warning(msg("log.income-bank-unsupported", account));
            return false;
        }

        EconomyResponse response = economy.bankDeposit(account, amount);
        if (response == null || !response.transactionSuccess()) {
            logger().warning(msg("log.income-transfer-failed", formatAmount(amount), account,
                    response == null ? "no response" : String.valueOf(response.errorMessage)));
            return false;
        }
        return true;
    }

    /**
     * 命令模式：给 Vault 接口覆盖不到的经济插件留的通道（例如 CMI 的 /cmi money give）。
     * 命令必须在主线程执行，否则大部分经济插件会直接抛异常。
     */
    private boolean dispatchIncomeCommand(Player payer, String account, double amount) {
        String template = HammrEnhance.getInstance().getSettings().getIncomeAccountCommand();
        if (template == null || template.isBlank()) {
            logger().warning(msg("log.income-command-empty"));
            return false;
        }

        String command = template
                .replace("%account%", account)
                .replace("%player%", account)
                .replace("%payer%", payer == null ? "" : payer.getName())
                .replace("%amount%", formatAmount(amount));
        if (command.startsWith("/")) command = command.substring(1);

        final String finalCommand = command;
        HammrEnhance plugin = HammrEnhance.getInstance();
        if (!Bukkit.isPrimaryThread()) {
            // 异步派发拿不到结果，失败由 runConsoleCommand 自己记日志
            plugin.getServer().getScheduler().runTask(plugin, () -> runConsoleCommand(finalCommand));
            return true;
        }
        return runConsoleCommand(finalCommand);
    }

    private boolean runConsoleCommand(String command) {
        try {
            boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            if (!ok) logger().warning(msg("log.income-command-failed", command));
            return ok;
        } catch (Exception e) {
            logger().log(Level.WARNING, msg("log.income-command-failed", command), e);
            return false;
        }
    }

    // ------------------------------------------------------------------
    // 账户解析
    // ------------------------------------------------------------------

    /** 启用与 /hammr reload 时预热收入账户，并把解析结果打进日志方便排查配置错误 */
    public void prepareIncomeAccount(String accountName) {
        resolvedIncome = null;
        if (accountName == null || accountName.isBlank()) return;

        // 解析只读服务端已有的缓存，不会阻塞，可以直接在主线程做
        OfflinePlayer resolved = lookupPlayer(accountName);
        if (resolved != null) resolvedIncome = new ResolvedAccount(accountName, resolved);

        // 经济插件可能比本插件晚一步注册 Vault 服务，诊断日志推迟一 tick 才准
        HammrEnhance plugin = HammrEnhance.getInstance();
        plugin.getServer().getScheduler().runTaskLater(plugin,
                () -> logIncomeAccountStatus(accountName), 1L);
    }

    /** 把「这笔钱最终会进谁的账」在启动日志里说清楚，配置写错不用等到玩家强化才发现 */
    private void logIncomeAccountStatus(String accountName) {
        ConfigSettings settings = HammrEnhance.getInstance().getSettings();
        if (!accountName.equals(settings.getIncomeAccount())) return; // 期间又 reload 过

        IncomeAccountType type = settings.getIncomeAccountType();
        if (type == IncomeAccountType.COMMAND) return;

        ResolvedAccount cached = resolvedIncome;
        if (cached != null) {
            UUID uuid = cached.player().getUniqueId();
            logger().info(msg("log.income-account-resolved", accountName,
                    String.valueOf(uuid), String.valueOf(uuid.version()), type.name()));
            return;
        }
        if (type != IncomeAccountType.BANK && hasPlayerAccount(accountName)) {
            logger().info(msg("log.income-account-vault-name", accountName));
            return;
        }
        if (type != IncomeAccountType.PLAYER && bankExists(accountName)) {
            logger().info(msg("log.income-account-bank", accountName));
            return;
        }
        logger().warning(msg("log.income-account-unresolved", accountName));
    }

    /** @return 目标玩家；null 表示这个名字不对应本服认识的玩家（此时绝不能凭空造账户） */
    private OfflinePlayer resolveAccount(String accountName) {
        ResolvedAccount cached = resolvedIncome;
        if (cached != null && accountName.equals(cached.name())) return cached.player();

        // 预热尚未完成(刚启用/刚改配置)时的兜底
        OfflinePlayer resolved = lookupPlayer(accountName);
        // 解析失败不写缓存：目标玩家首次登录后要能自动恢复，不该逼管理员 reload
        if (resolved != null) resolvedIncome = new ResolvedAccount(accountName, resolved);
        return resolved;
    }

    /**
     * 名字 → 玩家账户，只认服务端已经知道的映射，绝不自己推算 UUID。
     * 绝不能用 Bukkit.getOfflinePlayer(String) 兜底 —— 该方法对没进过本服的名字不会返回 null，
     * 而是用名字派生一个离线模式 UUID(v3)。正版账号和外置登录(LittleSkin 等)的账号都是服务端/
     * 认证服务器下发的 v4 UUID，跟这个派生值毫无关系，钱会打进一个谁都取不出来的幽灵账户。
     *
     * @return 目标玩家；null 表示本服暂时不认识这个名字，应当交给 Vault 的名字接口去解析
     */
    private OfflinePlayer lookupPlayer(String accountName) {
        // 1. 管理员直接写 UUID：按其显式意图使用，不参与后面的排序
        UUID uuid = parseUuid(accountName);
        if (uuid != null) return Bukkit.getOfflinePlayer(uuid);

        // 2. 收集本服已知的同名账户：在线玩家 + usercache(存的就是认证服务器下发的真实 UUID)，
        //    两者都不阻塞、都不会凭空造账户
        List<OfflinePlayer> candidates = new ArrayList<>(2);
        Player online = Bukkit.getPlayerExact(accountName);
        if (online != null) candidates.add(online);

        OfflinePlayer cached = Bukkit.getOfflinePlayerIfCached(accountName);
        if (cached != null && (online == null || !cached.getUniqueId().equals(online.getUniqueId()))) {
            candidates.add(cached);
        }

        // 3. v4 优先：正版与外置登录(LittleSkin 等)的 UUID 都是认证服务器下发的 v4，
        //    v3 只可能是离线模式按名字派生出来的。服务器从离线模式迁到外置登录后，同一个名字
        //    可能残留两份数据，v3 那份是取不出钱的历史空壳。同版本时保持收集顺序(在线玩家优先)。
        return candidates.stream()
                .filter(EconomyManager::isUsableAccount)
                .min(Comparator.comparingInt(candidate -> candidate.getUniqueId().version() == 4 ? 0 : 1))
                .orElse(null);
    }

    /**
     * 候选账户是否真的存在。
     * hasPlayedBefore() 只是一次 playerdata 文件状态检查，且解析结果会被缓存，开销可以忽略；
     * 过滤掉的候选会自动落到 Vault 名字接口，不会让转账直接失败。
     */
    private static boolean isUsableAccount(OfflinePlayer candidate) {
        return candidate.isOnline() || candidate.hasPlayedBefore();
    }

    /** 只接受管理员显式写出来的 UUID(带不带横线都行)，不做任何推算 */
    private static UUID parseUuid(String value) {
        String normalized = value.length() == 32
                ? value.replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5")
                : value;
        if (normalized.length() != 36) return null;
        try {
            return UUID.fromString(normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 经济插件里是否已存在这个玩家账户；部分插件没实现该方法，异常一律当作"查不到" */
    @SuppressWarnings("deprecation")
    private boolean hasPlayerAccount(String account) {
        if (!isEnabled()) return false;
        try {
            return economy.hasAccount(account);
        } catch (Exception | LinkageError e) {
            return false;
        }
    }

    /** 部分经济插件没实现 bank 相关方法，getBanks() 可能直接抛异常 */
    private boolean bankExists(String account) {
        if (!isEnabled() || !safeHasBankSupport()) return false;
        try {
            List<String> banks = economy.getBanks();
            return banks != null && banks.stream().anyMatch(account::equalsIgnoreCase);
        } catch (Exception | LinkageError e) {
            return false;
        }
    }

    private boolean safeHasBankSupport() {
        if (!isEnabled()) return false;
        try {
            return economy.hasBankSupport();
        } catch (Exception | LinkageError e) {
            return false;
        }
    }

    /** 命令模板里的金额：整数不带小数点，避免 1000.0 之类的写法被经济插件拒绝 */
    private static String formatAmount(double amount) {
        if (amount == Math.rint(amount) && !Double.isInfinite(amount) && Math.abs(amount) < 1e15) {
            return Long.toString((long) amount);
        }
        return BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static Logger logger() {
        return HammrEnhance.getInstance().getLogger();
    }

    private static String msg(String key, Object... args) {
        return HammrEnhance.getInstance().getMessages().get(key, args);
    }
}
