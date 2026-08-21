package org.cubex.hammr.economy;

/** income-account 的账户类型，决定强化收入用哪种方式转账 */
public enum IncomeAccountType {

    /** 优先按玩家账户转账，解析不到玩家且存在同名银行账户时回退到银行 */
    AUTO,
    /** 强制玩家账户（在线/离线玩家均可，等价于 cmi pay 的效果） */
    PLAYER,
    /** Vault 银行账户，需要经济插件实现 bank 支持 */
    BANK,
    /** 由控制台执行 income-account-command 指定的命令转账 */
    COMMAND;

    public static IncomeAccountType parse(String raw, IncomeAccountType fallback) {
        if (raw == null || raw.isBlank()) return fallback;
        try {
            return valueOf(raw.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }
}
