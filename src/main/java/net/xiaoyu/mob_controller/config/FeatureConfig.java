package net.xiaoyu.mob_controller.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Arrays;
import java.util.List;

/**
 * 功能开关、限制与数值调整配置。
 * 本配置文件位于：mob_controller/mob_controller-features.toml
 */
public class FeatureConfig {
    public static final ForgeConfigSpec SPEC;

    /**
     * 不可被控制的生物类型黑名单。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACKLISTED_MOBS;

    /**
     * 列表中的生物即使拥有主人或驯服标记，也会被 hasOwnerOrTameTag 判定为 false。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> IGNORE_OWNER_TAG_MOBS;

    /**
     * 在 STAY（停留）模式下需要进行坐标焊死处理的特殊 AI 飞行生物列表。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> STAY_WELDED_SPECIAL_AI_MOBS;

    /**
     * 是否始终使控制尝试成功。
     */
    public static final ForgeConfigSpec.BooleanValue ALWAYS_SUCCESS;

    /**
     * 被控制生物是否会听从主人的指令去攻击其他玩家。
     */
    public static final ForgeConfigSpec.BooleanValue CONTROLLED_MOBS_ATTACK_PLAYERS_ON_COMMAND;

    /**
     * 受控生物在脱战后开始自动回血前需要等待的 tick 数。
     */
    public static final ForgeConfigSpec.IntValue CONTROLLED_MOB_HEAL_OUT_OF_COMBAT_DELAY_TICKS;

    /**
     * 判定为“高生命值生物”的生命值阈值。
     */
    public static final ForgeConfigSpec.IntValue HIGH_HEALTH_THRESHOLD;

    /**
     * 生物死亡后触发重生的延迟刻数。
     */
    public static final ForgeConfigSpec.IntValue RESPAWN_DELAY_TICKS;

    /**
     * 是否启用重生功能。
     */
    public static final ForgeConfigSpec.BooleanValue ENABLE_RESPAWN;

    /**
     * 生物的攻击力上限。
     */
    public static final ForgeConfigSpec.IntValue ATTACK_LIMIT;

    /**
     * 生物的生命上限。
     */
    public static final ForgeConfigSpec.IntValue HEALTH_LIMIT;

    /**
     * 满足驯服条件的生命百分比阈值。
     */
    public static final ForgeConfigSpec.DoubleValue HEALTH_PERCENT_THRESHOLD;

    /**
     * 驯服时需要怪物的固定血量阈值。
     */
    public static final ForgeConfigSpec.IntValue REQUIRED_HEALTH;

    /**
     * 是否允许受控流浪商人拥有无限交易。
     */
    public static final ForgeConfigSpec.BooleanValue INFINITE_TRADES_FOR_CONTROLLED_WANDERING_TRADER;

    /**
     * 雪球击退效果模式（极小伤害+即时回血）
     */
    public static final ForgeConfigSpec.EnumValue<SnowballKnockbackOption> SNOWBALL_KNOCKBACK_MODE;

    /**
     * 是否允许着火雪球使目标着火。
     */
    public static final ForgeConfigSpec.BooleanValue SNOWBALL_FIRE_HIT;

    /**
     * 是否在重生开启时阻止受控生物掉落任何物品（包括装备、战利品表物品、经验值）。
     */
    public static final ForgeConfigSpec.BooleanValue PREVENT_DROPS_ON_RESPAWN;

    /**
     * 自定义生物最大数量限制（覆盖高生命值限制）
     * 格式：'生物注册名,最大数量'，例如 'minecraft:iron_golem,3'
     * 若生物在此列表中，则不受高生命值阈值判断，且每个玩家最多控制指定数量。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_MAX_COUNTS;

    /**
     * 受控生物召唤出来的仆从，是否禁止主动把玩家当攻击目标。
     */
    public static final ForgeConfigSpec.BooleanValue PREVENT_CONTROLLED_MINION_ATTACKING_PLAYERS;

    /**
     * 受控生物的召唤物名单：列在这里的实体一出生就检查附近有没有受控生物，
     * 有就当作它的召唤物处理。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CONTROLLED_MINION_ENTITIES;

    /**
     * 是否阻止已受控的灾变 Boss 执行「回家」传送。
     * 灾变 Boss 离开记录的家乡维度时会自行传送回竞技场，表现为「被带到别的维度后消失」。
     */
    public static final ForgeConfigSpec.BooleanValue PREVENT_CONTROLLED_BOSS_RETURN_HOME;

    /**
     * 是否启用「防误伤」：控制者直接挥击或用远程武器攻击自己的受控生物时取消伤害。
     * 潜行攻击不受影响，仍可正常击杀自己的伙伴。
     */
    public static final ForgeConfigSpec.BooleanValue PREVENT_ACCIDENTAL_PET_DAMAGE;

    /**
     * 被骑乘（身上驮着乘客）的受控生物是否保持原版 AI。
     * 开启后不再对它做受控生物的目标过滤，攻击与技能由它自己的 AI 决定。
     */
    public static final ForgeConfigSpec.BooleanValue KEEP_MOUNT_AI;

    /**
     * 是否允许受控生物被骑乘（包括本来拒绝乘客的模组生物）。
     */
    public static final ForgeConfigSpec.BooleanValue ALLOW_CONTROLLED_MOB_RIDE;

    /**
     * 受控生物（及其召唤物）引发的爆炸，是否不破坏方块。
     */
    public static final ForgeConfigSpec.BooleanValue CONTROLLED_MINION_EXPLOSION_NO_GRIEFING;

    /**
     * 回响指针「深度搜索」时一次最多强制加载多少个区块。
     */
    public static final ForgeConfigSpec.IntValue BEAST_COMPASS_DEEP_SEARCH_CHUNK_LIMIT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("===================================================");
        builder.comment("  功能与限制");
        builder.comment("===================================================");
        builder.comment("");

        BLACKLISTED_MOBS = builder
                .comment("无法被控制的生物列表。",
                        "默认黑名单里的模组生物是因为存在严重代码冲突/不兼容才加进去的，移除时请谨慎。")
                .defineList("blacklisted_mobs", Arrays.asList(
                        "minecraft:ender_dragon",
                        "deep_aether:eots_segment",
                        "deep_aether:eots_controller",
                        "aether:sun_spirit",
                        "aether:slider",
                        "lost_aether_content:aerwhale_king",
                        "alexsmobs:centipede_body",
                        "alexsmobs:centipede_tail",
                        "alexsmobs:void_worm",
                        "alexsmobs:void_worm_part",
                        "twilightforest:hydra"
                ), obj -> obj instanceof String);
        builder.comment("");

        IGNORE_OWNER_TAG_MOBS = builder
                .comment("列表中的生物即使本身带主人/驯服标记，也会被当作「没有主人」处理。",
                        "格式：实体注册名，例如 'minecraft:wolf'。",
                        "这会覆盖默认的主人/驯服判定。")
                .defineList("ignore_owner_tag_mobs", Arrays.asList(
                        "alexscaves:grottoceratops",
                        "alexscaves:relicheirus",
                        "alexscaves:luxtructosaurus",
                        "alexscaves:atlatitan",
                        "alexsmobs:orca",
                        "bosses_of_mass_destruction:lich",
                        "bosses_of_mass_destruction:void_blossom",
                        "bosses_of_mass_destruction:obsidilith",
                        "bosses_of_mass_destruction:gauntlet"
                ), obj -> obj instanceof String);
        builder.comment("");

        STAY_WELDED_SPECIAL_AI_MOBS = builder
                .comment("在「停留」模式下需要焊死坐标的特殊 AI 飞行生物")
                .defineList("stay_welded_special_ai_mobs", Arrays.asList(
                        "minecraft:ghast", "minecraft:vex", "minecraft:blaze", "minecraft:phantom", "minecraft:bat"
                ), obj -> obj instanceof String);
        builder.comment("");

        ALWAYS_SUCCESS = builder
                .comment("是否让控制尝试必定成功")
                .define("always_success", false);
        builder.comment("");

        CONTROLLED_MOBS_ATTACK_PLAYERS_ON_COMMAND = builder
                .comment("受控生物是否会听从主人的指令攻击其他玩家")
                .define("controlled_mobs_attack_players_on_command", true);
        builder.comment("");

        CONTROLLED_MOB_HEAL_OUT_OF_COMBAT_DELAY_TICKS = builder
                .comment("受控生物脱离战斗多少 tick 后开始自动回血（100 tick = 5 秒）")
                .defineInRange("controlled_mob_heal_out_of_combat_delay_ticks", 100, 0, Integer.MAX_VALUE);
        builder.comment("");

        HIGH_HEALTH_THRESHOLD = builder
                .comment("被判定为「高生命值生物」的生命值门槛")
                .defineInRange("high_health_threshold", 150, 1, Integer.MAX_VALUE);
        builder.comment("");

        RESPAWN_DELAY_TICKS = builder
                .comment("生物死亡后经过多少 tick 触发重生（600 tick = 30 秒）")
                .defineInRange("respawn_delay_ticks", 600, 100, Integer.MAX_VALUE);
        builder.comment("");

        ENABLE_RESPAWN = builder
                .comment("是否启用受控生物的重生功能。设为 false 时受控生物会正常死亡、不再重生。")
                .define("enable_respawn", true);
        builder.comment("");

        ATTACK_LIMIT = builder
                .comment("可被控制的生物允许的最大攻击伤害（属性 attack_damage）。攻击伤害大于等于该值的生物无法被控制。")
                .defineInRange("attack_limit", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        builder.comment("");

        HEALTH_LIMIT = builder
                .comment("可被控制的生物允许的最大生命值。最大生命值大于等于该值的生物无法被控制。")
                .defineInRange("health_limit", Integer.MAX_VALUE, 1, Integer.MAX_VALUE);
        builder.comment("");

        HEALTH_PERCENT_THRESHOLD = builder
                .comment("血量百分比门槛（0.0 ~ 100.0）。当（当前血量 / 最大血量）* 100 小于等于该值时，生物可被控制（与 required_health 二选一）。示例：10.0 = 10%，0.5 = 0.5%")
                .defineInRange("health_percent_threshold", 0.01, 0.0, 100.0);
        builder.comment("");

        REQUIRED_HEALTH = builder
                .comment("绝对血量门槛。当前血量小于等于该值时生物可被控制（与 health_percent_threshold 二选一）。")
                .defineInRange("required_health", 10, 1, Integer.MAX_VALUE);
        builder.comment("");

        INFINITE_TRADES_FOR_CONTROLLED_WANDERING_TRADER = builder
                .comment("让受控的流浪商人可以无限交易（每次交易后使用次数重置为 0）。")
                .define("infinite_trades_for_controlled_wandering_trader", true);
        builder.comment("");

        // 雪球击退效果配置（极小伤害+即时回血）
        SNOWBALL_KNOCKBACK_MODE = builder
                .comment("控制雪球击退效果（0.000001 伤害 + 立刻回血）。",
                        "可选值：",
                        "  DISABLED                     - 不启用，保持原版行为。",
                        "  CONTROLLED_SNOW_GOLEM_ONLY   - 只有受控雪傀儡扔出的雪球才有该效果（默认）。",
                        "  SNOW_GOLEM_ONLY              - 所有雪傀儡（受控与否）扔出的雪球都有该效果。",
                        "  ALL_SNOWBALLS                - 所有雪球（任何来源）都有该效果。")
                .defineEnum("snowball_knockback_mode", SnowballKnockbackOption.CONTROLLED_SNOW_GOLEM_ONLY);
        builder.comment("");

        // 着火雪球使目标着火开关
        SNOWBALL_FIRE_HIT = builder
                .comment("为 true 时，着火的雪球命中后会让目标燃烧 5 秒。",
                        "默认：true")
                .define("snowball_fire_hit", true);
        builder.comment("");

        PREVENT_DROPS_ON_RESPAWN = builder
                .comment("为 true 时，在开启重生（enable_respawn）的情况下，受控生物死亡不掉落任何战利品、装备和经验（可避免重生刷物品）。",
                        "这样可以避免重生带来的刷物品问题。",
                        "默认：true")
                .define("prevent_drops_on_respawn", true);
        builder.comment("");

        CUSTOM_MAX_COUNTS = builder
                .comment("为特定生物自定义数量上限（覆盖高血量限制）。",
                        "格式：'实体ID,数量'（例如 'minecraft:iron_golem,3'）。",
                        "列在这里的生物不算「高血量」，每名玩家最多可控制到「数量」个。",
                        "把数量设为 0 表示完全禁止控制该生物。")
                .defineList("custom_max_counts", List.of(), obj -> obj instanceof String);

        builder.comment("");

        PREVENT_CONTROLLED_MINION_ATTACKING_PLAYERS = builder
                .comment("为 true 时，受控生物召唤出来的仆从不会主动把玩家当成攻击目标。",
                        "识别方式：新生成的生物如果其召唤者/主人（Owner、Summoner 等 NBT 字段）",
                        "对应的是某只受控生物，就会被打上「受控仆从」标记，之后它锁定玩家时会被拦下。",
                        "注意：没有任何主人信息的野生召唤物（例如凋灵自己造的小怪）不在此列。")
                .define("prevent_controlled_minion_attacking_players", true);

        builder.comment("");

        CONTROLLED_MINION_ENTITIES = builder
                .comment("受控生物的召唤物名单（指定实体）。",
                        "两种识别方式会一起用：",
                        "  1) 自动：生物自带 Owner/Summoner 这类 NBT，且指向的是受控生物；",
                        "  2) 名单：实体出生时附近 16 格内有受控生物，就当作它的召唤物。",
                        "名单只影响第 2 种，用来兜住那些不写 owner NBT 的模组召唤物。",
                        "格式：实体注册名，例如 'mutantmore:rodling'；写 \"*\" 表示所有实体的召唤物都接管。",
                        "留空 = 只走自动识别。")
                .defineList("controlled_minion_entities", List.of(), obj -> obj instanceof String);

        builder.comment("");

        ALLOW_CONTROLLED_MOB_RIDE = builder
                .comment("允许受控生物被骑乘。部分模组生物（如传说怪物的潜影拟态者）默认拒绝乘客，",
                        "开启后可用骑乘令等让它们充当坐骑。默认 true")
                .define("allow_controlled_mob_ride", true);
        builder.comment("");

        KEEP_MOUNT_AI = builder
                .comment("被骑乘的受控生物保持原版 AI：为 true 时不再参与受控生物的目标过滤，",
                        "它自己的攻击与技能逻辑照常运行。默认 true")
                .define("keep_mount_ai", true);
        builder.comment("");

        PREVENT_ACCIDENTAL_PET_DAMAGE = builder
                .comment("启用防误伤：控制者直接挥击或用远程武器攻击自己的受控生物时不会造成伤害。",
                        "潜行（Shift）攻击不受此限制，仍可正常击杀自己的伙伴。默认 true")
                .define("prevent_accidental_pet_damage", true);
        builder.comment("");

        PREVENT_CONTROLLED_BOSS_RETURN_HOME = builder
                .comment("阻止已受控的灾变 Boss 执行「回家」传送。",
                        "灾变 Boss 一旦离开自身记录的家乡维度会自行传送回竞技场中心，",
                        "表现为「被放到别的维度后直接消失」。开启后受控 Boss 可正常跟随控制者跨维度。",
                        "默认 true")
                .define("prevent_controlled_boss_return_home", true);
        builder.comment("");

        CONTROLLED_MINION_EXPLOSION_NO_GRIEFING = builder
                .comment("为 true 时，受控生物以及它的召唤物引发的爆炸不会破坏方块。",
                        "只清掉方块破坏，伤害和击退照常 —— 苦力怕类召唤物自爆还是会炸伤生物，只是不再炸出坑。")
                .define("controlled_minion_explosion_no_griefing", true);

        builder.comment("");

        BEAST_COMPASS_DEEP_SEARCH_CHUNK_LIMIT = builder
                .comment("回响指针的「深度搜索」一次最多强制加载多少个区块。",
                        "深度搜索会把缓存里那些生物所在的区块拉起来，好让远处/被卸载的实体重新出现；",
                        "一次拉太多会让服务器明显卡顿，默认 32 是个折中值。")
                .defineInRange("beast_compass_deep_search_chunk_limit", 32, 1, 512);

        SPEC = builder.build();
    }

    /**
     * 注册本配置文件到 Forge 配置系统。
     */
    public static void register() {
        ModLoadingContext.get()
                .registerConfig(ModConfig.Type.COMMON, SPEC, "mob_controller/mob_controller-features.toml");
    }

    /**
     * 雪球击退效果的可选模式枚举。
     */
    public enum SnowballKnockbackOption {
        DISABLED,
        CONTROLLED_SNOW_GOLEM_ONLY,
        SNOW_GOLEM_ONLY,
        ALL_SNOWBALLS
    }
}