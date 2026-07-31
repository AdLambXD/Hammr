<div align="center">

# ⚒️ HammrEnhance

**下界合金装备铁砧强化系统**

![版本](https://img.shields.io/badge/版本-1.2.0-blue)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4+-brightgreen)
![Java](https://img.shields.io/badge/Java-21-orange)
![Paper](https://img.shields.io/badge/API-Paper-blueviolet)
![构建](https://img.shields.io/github/actions/workflow/status/AdLambXD/Hammr/build.yml?label=构建)
![发布](https://img.shields.io/github/v/release/AdLambXD/Hammr?label=Release)

*在铁砧上，将你的下界合金装备一步步强化至极限。*

</div>

---

## 📖 目录

- [插件简介](#-插件简介)
- [功能特性](#-功能特性)
- [环境要求](#-环境要求)
- [安装](#-安装)
- [快速上手](#-快速上手)
- [核心机制详解](#-核心机制详解)
- [命令与权限](#-命令与权限)
- [配置参考](#-配置参考)
- [语言文件](#-语言文件)
- [数据存储](#-数据存储)
- [常见问题](#-常见问题)
- [开发者指南](#-开发者指南)
- [更新日志](#-更新日志)
- [支持与反馈](#-支持与反馈)
- [作者与许可](#-作者与许可)

---

## 🎮 插件简介

HammrEnhance 是一款基于 **Paper** 的 Minecraft 插件，为下界合金装备（武器 / 工具 / 盔甲）引入了一套完整的 **铁砧强化养成系统**。

玩家将装备放入铁砧并消耗材料、金币与经验进行强化，不断提升装备的主附魔等级（如 锋利、效率、保护）。强化成功的装备会在 Lore 中显示成长状态，例如：

```
[+5(3)]
█████████████████████░░░░ | 80%
```

- `[+5(3)]` 表示 **主强化等级 +5**、**分支强化总等级 (3)**；
- 第二行是 **装备经验进度条**，代表当前主等级到下一级所需的经验积累进度。

随着等级提升，成功率逐步下降，失败还可能**掉级**甚至**炸毁铁砧**，充满赌性与刺激感。

---

## ✨ 功能特性

- **🛠 铁砧主强化**：在铁砧中消耗材料（钻石 / 下界合金锭）提升装备主等级，默认最高 `+10`。
- **🧬 分支强化**：主等级达到门槛后，可为武器 / 盔甲随机附加**分支附魔**（如 火焰附加、亡灵杀手、弹射物保护等），分支池按装备类型独立配置。
- **📈 装备经验系统**：用工具挖方块、拾取经验球都会为手持/穿戴的强化装备积累经验，Lore 进度条实时展示。
- **👁 铁砧预览界面**：放入材料后铁砧输出栏自动显示强化类型、材料与金币消耗，点击即执行，操作直观。
- **💥 失败惩罚机制**：失败有概率掉级（`level-down-chance`）与铁砧爆炸（`explosion-chance`），爆炸时装备会被炸落到地面，紧张感拉满。
- **💰 Vault 经济支持**：可选接入 [Vault](https://github.com/MilkBowl/Vault)，按等级扣费；未安装 Vault 时自动跳过金币消耗。
- **📦 数据安全持久化**：强化数据写入物品 **PDC**（Persistent Data Container），装备放入箱子、随身携带、重启服务器都不会丢失。
- **🔧 高度可配置**：成功率、等级上限、材料类型、分支池、主附魔映射、经验倍率、进度条样式等全部可在 `config.yml` 中调整。
- **🌐 全量中文消息**：所有提示、ActionBar、物品名、附魔名均通过 `messages.yml` 管理，可自由修改文案与颜色代码。

---

## 📋 环境要求

| 项目 | 要求 |
| --- | --- |
| 服务端 | [Paper](https://papermc.io) **1.21.4** 及以上 |
| Java | **21** |
| Vault（可选） | 需要金币消耗功能时安装，缺省时自动跳过金币扣费 |

> ⚠️ 插件使用 Paper 独有 API（如 RegistryAccess），**不支持** Spigot / CraftBukkit。

---

## 📥 安装

1. 在 [Releases](https://github.com/AdLambXD/Hammr/releases) 下载最新版 `HammrEnhance-*.jar`，或前往 [Actions](https://github.com/AdLambXD/Hammr/actions) 获取最新构建产物。
2. 将 jar 文件放入服务端 `plugins/` 目录。
3. **重启**服务器（插件不支持热加载）。
4. 启动后插件会自动生成配置文件：
   - `plugins/HammrEnhance/config.yml` — 玩法数值配置
   - `plugins/HammrEnhance/messages.yml` — 语言 / 消息配置
5. 修改配置后，在游戏内执行 `/hammr reload` 即可生效。

---

## 🚀 快速上手

以下是一个完整的玩家体验流程示例（默认配置）：

```
1. 手持【下界合金剑】，将其放入铁砧左侧。
2. 铁砧右侧放入【钻石 x1】（主等级 < 6 时使用钻石）。
3. 铁砧输出栏出现预览：
       ◆ 主强化
       消耗: 钻石 x1 + 1000 金币
       点击取出以执行
4. 点击取出，扣费并执行强化（当前等级 +1 → 有 95% 成功率）。
5. 将剑拿起，Lore 显示 [+1] 与经验进度条。
6. 当主等级达到 +8 后：
   → 再次放入铁砧 + 钻石，将进行【分支强化】，
     随机获得 火焰附加 / 亡灵杀手 / 节肢杀手 之一。
7. 主等级达到 +6 后，需改用【下界合金锭】继续强化。
8. 强化至 +10（满级）时，全服广播庆祝消息！
```

---

## 🧠 核心机制详解

### 1. 主强化（Main Enhancement）

**操作**：铁砧左侧放入下界合金装备，右侧放入对应材料。

| 主等级 | 所需材料 | 说明 |
| --- | --- | --- |
| 0 ～ 5 | 💎 钻石 x1 | 低于 `main-material-threshold`（默认 6） |
| 6 ～ 9 | 🔩 下界合金锭 x1 | 达到或超过阈值后 |

**消耗**（默认值）：

| 项目 | 数值 | 说明 |
| --- | --- | --- |
| 金币 | 1000 / 次 | 可用 `cost-gold-per-level` 按等级区分 |
| 经验 | `主等级 × xp-multiplier` 级 | 例如 +3 → +4 需 300 级经验 |

> 💡 **经验替代**：当装备自身积累的经验达到要求时，会优先消耗装备经验，无需扣除玩家经验值。

**成功率**（默认，主强化 `main-success-rates`）：

| 目标等级 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 成功率 | 95% | 95% | 95% | 95% | 95% | 85% | 75% | 60% | 40% | 30% |

**主附魔映射**（默认）：

| 装备 | 主附魔 |
| --- | --- |
| 剑 / 斧 | 锋利 (sharpness) |
| 镐 / 锹 / 锄 | 效率 (efficiency) |
| 头盔 / 胸甲 / 护腿 / 靴子 | 保护 (protection) |

### 2. 分支强化（Branch Enhancement）

主等级达到 `branch-min-main-level`（默认 **8**）后开放。

- **材料**：钻石 x1（`materials.branch`）。
- **机制**：从该装备类型对应的 `branch-pools` 中**随机**抽取一个尚未满级的分支附魔并提升 1 级。
- **上限**：分支总等级最高 `branch-max-level`（默认 **6**）。

**默认分支池**：

| 装备类型 | 分支池 |
| --- | --- |
| 剑 / 斧 | 火焰附加、节肢杀手、亡灵杀手 |
| 头盔 / 胸甲 / 护腿 / 靴子 | 弹射物保护、火焰保护、爆炸保护 |

**成功率**（默认，分支强化 `branch-success-rates`）：

| 分支等级 | 1 | 2 | 3 | 4 | 5 |
| --- | --- | --- | --- | --- | --- |
| 成功率 | 40% | 35% | 30% | 25% | 20% |

### 3. 装备经验系统

- **经验来源**：
  - 使用工具（镐 / 斧 / 锹 / 锄）挖掘方块：+`block-break-xp`（默认 1）
  - 拾取经验球时，主手 / 副手 / 盔甲栏装备：+`xp-from-exp-orb`（默认 1）
- **经验用途**：`主等级 × xp-multiplier` = 升级所需装备经验，累积满后主强化不再消耗玩家经验。
- **展示**：Lore 中显示进度条，样式（宽度、颜色、字符、百分比格式）可在 `progress-bar` 节点配置。

### 4. 失败与爆炸机制

| 事件 | 概率（默认） | 后果 |
| --- | --- | --- |
| 主强化失败 | 1 - 成功率 | 不掉级 |
| 主强化失败 + 掉级 | `level-down-chance`（20%） | 主等级 -1（掉到 0 时清除全部强化） |
| 铁砧爆炸 | `explosion-chance`（5%） | 关闭铁砧界面，装备被炸落到铁砧位置地面，铁砧被摧毁并产生小范围爆炸（`explosion-radius`） |

> 💥 主强化和分支强化都可能触发爆炸；分支强化失败**不会**掉级，仅损失材料。

---

## 📜 命令与权限

### 命令

| 命令 | 说明 |
| --- | --- |
| `/hammr` | 查看命令帮助 |
| `/hammr set <主等级> [分支类型] [分支等级]` | 将**手中**装备设置为指定强化等级 |
| `/hammr remove` | 移除**手中**装备的所有强化数据 |
| `/hammr give <玩家> [主等级] [分支类型] [分支等级]` | 以**手中**装备为模板，将强化装备给予指定玩家 |
| `/hammr reload` | 热重载 `config.yml` 与 `messages.yml` |

**参数说明**：

| 参数 | 说明 |
| --- | --- |
| `<主等级>` | 0 ～ `main-max-level` 的整数 |
| `[分支类型]` | 分支附魔键（如 `fire_aspect`）或 `random` 随机抽取 |
| `[分支等级]` | 0 ～ `branch-max-level` 的整数，默认 1 |

**示例**：

```text
/hammr set 5                → 将手中剑设为 +5
/hammr set 8 random 3       → 设为 +8，并随机附加 3 级分支
/hammr set 9 smite 2        → 设为 +9，分支为 亡灵杀手 II
/hammr remove               → 清除手中装备强化
/hammr give Steve 10        → 以手中装备为模板，给予 Steve 一把 +10 装备
/hammr give Alex 10 random  → 给予 Alex +10 且带随机分支的装备
```

### 权限

| 权限节点 | 说明 | 默认 |
| --- | --- | --- |
| `hammr.admin` | 允许使用所有管理命令 | OP |

---

## ⚙️ 配置参考

配置文件位于 `plugins/HammrEnhance/config.yml`，首次启动自动生成。修改后执行 `/hammr reload` 生效。

### 节点说明

| 节点 | 默认值 | 说明 |
| --- | --- | --- |
| `main-success-rates` | `[95, 95, 95, 95, 95, 85, 75, 60, 40, 30]` | 主强化各级成功率（%），第 N 个值表示 +N → +N+1 |
| `branch-success-rates` | `[40, 35, 30, 25, 20]` | 分支强化各级成功率（%） |
| `main-max-level` | `10` | 主强化最高等级 |
| `branch-max-level` | `6` | 分支强化最高总等级 |
| `branch-min-main-level` | `8` | 分支强化所需最低主等级 |
| `cost-gold` | `1000` | 单次强化金币消耗（`cost-gold-per-level` 为空时使用） |
| `cost-gold-per-level` | `[]` | 按等级的消耗列表（长度不够时取最后一个值），优先级高于 `cost-gold` |
| `level-down-chance` | `0.2` | 主强化失败掉级概率（0.0 ～ 1.0） |
| `explosion-chance` | `0.05` | 铁砧爆炸概率（0.0 ～ 1.0） |
| `explosion-radius` | `2.0` | 铁砧爆炸半径 |
| `xp-multiplier` | `100` | 每级所需装备经验 = 主等级 × 倍率 |
| `block-break-xp` | `1` | 挖掘方块获得的装备经验 |
| `xp-from-exp-orb` | `1` | 拾取经验球获得的装备经验 |
| `main-material-threshold` | `6` | 主强化材料切换等级（低于此值用低级材料，否则用高级材料） |
| `broadcast-on-max-level` | `true` | 强化至满级是否全服公告 |
| `sound-enabled` | `true` | 是否播放强化音效 |
| `materials.main-low` | `DIAMOND` | 主强化低级材料 |
| `materials.main-high` | `NETHERITE_INGOT` | 主强化高级材料 |
| `materials.branch` | `DIAMOND` | 分支强化材料 |
| `branch-pools` | 见下方 | 各装备类型可随机获得的分支附魔池 |
| `main-enchant-mapping` | 见下方 | 装备类型 → 主附魔 ID 映射 |
| `progress-bar.*` | 见下方 | Lore 进度条样式 |

### 完整示例

```yaml
# ============================================
# HammrEnhance - 下界合金装备强化系统 配置
# ============================================

# 主强化各等级成功率 (%) - 按等级顺序
# 第1个值表示从 +1 强化到 +2 的成功率, 以此类推
main-success-rates: [95, 95, 95, 95, 95, 85, 75, 60, 40, 30]

# 分支强化各等级成功率 (%) - 按等级顺序
branch-success-rates: [40, 35, 30, 25, 20]

# 主强化最高等级
main-max-level: 10

# 分支强化最高总等级
branch-max-level: 6

# 分支强化所需最低主等级
branch-min-main-level: 8

# 每次强化消耗金币 (当 cost-gold-per-level 为空时使用此值)
cost-gold: 1000

# 按等级的金币消耗 (优先级高于 cost-gold)
# 例如: [500, 800, 1000, 1500, 2000, 2500, 3000, 4000, 5000, 8000]
cost-gold-per-level: []

# 失败掉级概率 (0.0 - 1.0)
level-down-chance: 0.2

# 铁砧爆炸概率 (0.0 - 1.0)
explosion-chance: 0.05

# 铁砧爆炸半径
explosion-radius: 2.0

# 经验值倍率 - 每级主强化所需经验 = 主等级 × 倍率
xp-multiplier: 100

# 挖掘方块时获得的物品经验值
block-break-xp: 1

# 获得经验球时转化的物品经验值
xp-from-exp-orb: 1

# 最高级强化达成时是否全服公告
broadcast-on-max-level: true

# 是否播放音效
sound-enabled: true

# 主强化材料切换等级 (低于此值用 main-low, 达到后使用 main-high)
main-material-threshold: 6

materials:
  main-low: DIAMOND          # 主强化低级材料
  main-high: NETHERITE_INGOT # 主强化高级材料
  branch: DIAMOND            # 分支强化材料

# 分支强化池: 每种装备类型可随机获得的分支附魔列表
branch-pools:
  SWORD:
    - fire_aspect
    - bane_of_arthropods
    - smite
  AXE:
    - fire_aspect
    - bane_of_arthropods
    - smite
  HELMET:
    - projectile_protection
    - fire_protection
    - blast_protection
  CHESTPLATE:
    - projectile_protection
    - fire_protection
    - blast_protection
  LEGGINGS:
    - projectile_protection
    - fire_protection
    - blast_protection
  BOOTS:
    - projectile_protection
    - fire_protection
    - blast_protection

# 主附魔映射: 装备类型 -> 对应 Minecraft 附魔 ID
main-enchant-mapping:
  SWORD: sharpness
  AXE: sharpness
  PICKAXE: efficiency
  SHOVEL: efficiency
  HOE: efficiency
  HELMET: protection
  CHESTPLATE: protection
  LEGGINGS: protection
  BOOTS: protection

# 进度条样式
progress-bar:
  width: 25        # 进度条总宽度 (字符数)
  filled-color: "§a"  # 已填充部分的颜色代码
  empty-color: "§8"   # 未填充部分的颜色代码
  suffix-color: "§f"  # 百分比文字的颜色代码
  char: "|"           # 填充和空白使用的字符
  suffix-format: " %s%%"  # 百分比文字格式 (%s 为数值)
```

---

## 🌐 语言文件

所有玩家可见文本（错误提示、ActionBar、物品名称、附魔名称、铁砧预览、命令帮助、控制台日志）均在 `plugins/HammrEnhance/messages.yml` 中。

主要分组：

| 分组 | 内容 |
| --- | --- |
| `error.*` | 各类失败 / 条件不满足的提示 |
| `actionbar.*` | 强化成功 / 失败 / 掉级 / 爆炸的 ActionBar 消息 |
| `item-name.*` | 下界合金装备的中文名称 |
| `material-name.*` | 材料（钻石 / 下界合金锭）名称 |
| `enchant-name.*` | 附魔的中文显示名称 |
| `preview.*` | 铁砧预览界面的标题 / 消耗 / 操作提示 |
| `command.*` | 命令帮助与执行结果 |
| `log.*` | 插件启用 / 停用日志 |

> ✏️ 消息支持 `{0}`、`{1}` 占位符（自动填入数值 / 名称），可直接使用 `§` 颜色代码。若需国际化，将整个文件翻译为其他语言即可。

---

## 📦 数据存储

强化数据通过物品 **PDC**（Persistent Data Container）持久化，命名空间为 `hammr`：

| 键 | 类型 | 说明 |
| --- | --- | --- |
| `enhance_main_level` | int | 主强化等级 |
| `enhance_branches` | string list | 分支列表，格式 `分支类型:等级` |
| `enhance_xp_points` | int | 装备已积累经验值 |

- 数据随物品保存，放入箱子 / 背包 / 末影箱均不会丢失。
- 通过 `/hammr remove` 或插件逻辑清理数据，不影响物品其他附魔。

---

## ❓ 常见问题

**Q：为什么铁砧里没有出现强化预览？**
A：请确认左侧为下界合金装备、右侧为钻石或下界合金锭，且满足强化条件（如分支强化需要主等级 ≥ 8、主强化未满级）。

**Q：不装 Vault 可以吗？**
A：可以。未检测到 Vault 时会跳过金币扣费，其余功能不受影响。

**Q：强化失败装备会消失吗？**
A：主强化失败最多掉 1 级；若掉到 0 级则清除全部强化。只有触发**铁砧爆炸**时装备才会掉落在地面（可捡回）。

**Q：如何让玩家把 +5 剑继续强化到 +6？**
A：+5 → +6 仍使用钻石（低于阈值 6）；+6 → +7 起需要下界合金锭。

**Q：支持 1.20 或更早版本吗？**
A：不支持，插件要求 Paper 1.21.4+ 与 Java 21。

**Q：装备上的普通附魔会被清除吗？**
A：不会。插件只管理自身的强化附魔与数据，其余附魔保持不变。

---

## 👨‍💻 开发者指南

### 项目结构

```text
src/main/java/org/cubex/hammr/
├── HammrEnhance.java          # 插件主类
├── command/
│   └── HammrCommand.java      # /hammr 命令与 Tab 补全
├── config/
│   ├── ConfigSettings.java    # config.yml 解析
│   └── MessageProvider.java   # messages.yml 解析
├── economy/
│   └── EconomyManager.java    # Vault 经济桥接
├── enhancement/
│   ├── BranchPool.java        # 分支池 / 附魔解析
│   ├── EnhanceManager.java    # 强化核心逻辑
│   └── EnhanceResult.java     # 强化结果对象
├── listener/
│   ├── AnvilListener.java     # 铁砧预览与强化交互
│   └── XpListener.java        # 经验积累监听
├── storage/
│   ├── EnhanceData.java       # 强化数据模型
│   └── PDCAdapter.java        # PDC 序列化
└── util/
    ├── ItemChecker.java       # 装备 / 材料判定
    ├── LoreBuilder.java       # Lore 渲染
    └── RomanNumber.java       # 罗马数字工具
```

### 从源码构建

```bash
git clone https://github.com/AdLambXD/Hammr.git
cd Hammr
./gradlew build
```

构建产物位于 `build/libs/HammrEnhance-<version>.jar`。

### 技术栈

- Java 21 · Gradle · Paper API 1.21.4
- [VaultAPI](https://github.com/MilkBowl/VaultAPI)（可选依赖）
- Adventure 组件（彩色消息 / ActionBar）

---

## 📝 更新日志

### v1.2.0
- 调整强化系统参数配置
- 添加工具（镐 / 锹 / 锄）强化支持，优化经验分配逻辑
- 修复 Gradle 构建权限问题
- 优化增强系统实现与编译配置
- 重构分支池配置管理，添加配置文件支持

### v1.1.0
- 修复铁砧强化功能中的预览与材料消耗问题
- 添加装备经验系统

### v1.0.0
- 重构强化系统，支持多分支强化
- 优化铁砧强化系统，添加管理命令

---

## 📮 支持与反馈

- 🐛 遇到 Bug 或有功能建议：请在 [GitHub Issues](https://github.com/AdLambXD/Hammr/issues) 提交
- 🔨 持续集成状态：[Actions](https://github.com/AdLambXD/Hammr/actions)

---

## 👤 作者与许可

**作者**：[CubeX](https://cubexmc.org)、[AdLamb](https://github.com/AdLambXD)、FZAoao

本项目仅用于学习与娱乐用途，作者不对使用过程中产生的任何后果负责。
