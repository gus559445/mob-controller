package net.xiaoyu.mob_controller.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.Arrays;
import java.util.List;

/**
 * 传送与跟随行为配置。
 */
public class TeleportConfig {
    public static final ForgeConfigSpec SPEC;

    /**
     * 水平传送触发距离（单位：格）。
     * 当受控生物与控制者的水平距离超过此值时，强制传送回到控制者身边。
     * 默认值：14.0。
     */
    public static final ForgeConfigSpec.DoubleValue FOLLOW_TELEPORT_HORIZONTAL_DISTANCE;

    /**
     * 垂直传送触发距离（单位：格）。
     * 当受控生物与控制者的垂直距离（绝对值）超过此值时，强制传送回到控制者身边。
     * 默认值：32.0。
     */
    public static final ForgeConfigSpec.DoubleValue FOLLOW_TELEPORT_VERTICAL_DISTANCE;

    /**
     * 跟随移动触发距离（单位：格）。
     * 当受控生物与控制者的距离超过此值时，生物会主动向控制者移动（而非原地待命）。
     * 默认值：8.0。
     */
    public static final ForgeConfigSpec.DoubleValue FOLLOW_MOVE_TO_DISTANCE;

    /**
     * 强制视为水生生物的列表（即使其原版类型不是 WATER）。
     * 列表中每一项为实体注册名，格式 "namespace:path"。
     * 当生物在此列表中时，传送落点一律选择水中。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WATER_BASED_MOBS;

    /**
     * 强制视为陆生生物的列表（即使其原版类型是 WATER 或在两栖列表中）。
     * 列表中每一项为实体注册名，格式 "namespace:path"。
     * 当生物在此列表中时，传送落点一律选择陆地（空气方块）。
     * 注意：此配置的优先级高于 water_based_mobs 和原版类型，但低于 amphibian_mobs 的特殊逻辑。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> LAND_BASED_MOBS;

    /**
     * 两栖动物生物列表（用于跟随传送时的落点决策：在水中优先）。
     * 当控制者完全浸没在水中时，这些生物会被传送到水中安全位置。
     * 默认包含海龟（turtle）和青蛙（frog）。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> AMPHIBIAN_MOBS;

    /**
     * 是否启用「跨维度自动跟随」。
     * 开启后，控制者切换维度时，其名下受控生物会被一并带到新维度。
     */
    public static final ForgeConfigSpec.BooleanValue ENABLE_DIMENSION_FOLLOW;

    /**
     * 跨维度跟随的搜索半径（格）。0 或负数表示不限距离。
     */
    public static final ForgeConfigSpec.DoubleValue DIMENSION_FOLLOW_RANGE;

    /**
     * 是否允许为带走受控生物而强制加载未加载区块。
     */
    public static final ForgeConfigSpec.BooleanValue DIMENSION_FOLLOW_LOAD_UNLOADED_CHUNKS;

    /**
     * 单次跨维度跟随最多携带的生物数量。
     */
    public static final ForgeConfigSpec.IntValue DIMENSION_FOLLOW_MAX_MOBS;

    /**
     * 重生点保护半径（格）。
     * 距离该维度重生点在此范围内的受控生物不会被跨维度带走（通常用于把基地里的伙伴留下）。
     * 重生点取玩家睡觉/设置重生锚的位置；若玩家在该维度没有设置重生点，则退回世界出生点。
     * 0 表示不启用保护。默认 128.0（8 个区块）。
     */
    public static final ForgeConfigSpec.DoubleValue DIMENSION_FOLLOW_EXCLUDE_SPAWN_RADIUS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("===================================================");
        builder.comment("  传送与跟随");
        builder.comment("===================================================");
        builder.comment("");

        FOLLOW_TELEPORT_HORIZONTAL_DISTANCE = builder
                .comment("跟随时，水平距离超过多少格会触发传送。默认 14.0")
                .defineInRange("follow_teleport_horizontal_distance", 14.0, 1.0, 256.0);
        builder.comment("");

        FOLLOW_TELEPORT_VERTICAL_DISTANCE = builder
                .comment("跟随时，垂直距离超过多少格会触发传送。默认 32.0")
                .defineInRange("follow_teleport_vertical_distance", 32.0, 1.0, 256.0);
        builder.comment("");

        FOLLOW_MOVE_TO_DISTANCE = builder
                .comment("跟随时，离控制者超过多少格，受控生物开始朝其移动。默认 8.0")
                .defineInRange("follow_move_to_distance", 8.0, 1.0, 64.0);
        builder.comment("");

        WATER_BASED_MOBS = builder
                .comment("始终按「水生」处理传送的生物列表（覆盖默认的类型判断）。")
                .defineList("water_based_mobs", List.of("alexsmobs:skelewag"), obj -> obj instanceof String);
        builder.comment("");

        LAND_BASED_MOBS = builder
                .comment("始终按「陆生」处理传送的生物列表（覆盖默认类型判断与 water_based 列表）。")
                .defineList("land_based_mobs", List.of(), obj -> obj instanceof String);
        builder.comment("");

        AMPHIBIAN_MOBS = builder
                .comment("两栖生物列表：当控制者在水下时，这些生物会传送到水中")
                .defineList("amphibian_mobs", Arrays.asList("minecraft:turtle", "minecraft:frog", "minecraft:drowned"), obj -> obj instanceof String);
        builder.comment("");

        ENABLE_DIMENSION_FOLLOW = builder
                .comment("启用受控生物跨维度自动跟随：控制者切换维度时，其名下受控生物一并带走。默认 true")
                .define("enable_dimension_follow", true);
        builder.comment("");

        DIMENSION_FOLLOW_RANGE = builder
                .comment("跨维度跟随的搜索半径（格）。设为 0 或负数表示不限距离。默认 256.0")
                .defineInRange("dimension_follow_range", 256.0, -1.0, 30000000.0);
        builder.comment("");

        DIMENSION_FOLLOW_LOAD_UNLOADED_CHUNKS = builder
                .comment("允许为带走受控生物而强制加载未加载区块；开启可找回被困在未加载区块的伙伴，"
                        + "但跨维度瞬间可能出现短暂卡顿。默认 true")
                .define("dimension_follow_load_unloaded_chunks", true);
        builder.comment("");

        DIMENSION_FOLLOW_MAX_MOBS = builder
                .comment("单次跨维度跟随最多携带的生物数量。默认 32")
                .defineInRange("dimension_follow_max_mobs", 32, 1, 1024);
        builder.comment("");

        DIMENSION_FOLLOW_EXCLUDE_SPAWN_RADIUS = builder
                .comment("重生点保护半径（格）：距该维度重生点（玩家床/重生锚；未设置时退回世界出生点）"
                        + "在此范围内的受控生物不会被跨维度带走。0 = 不启用。默认 128.0（8 个区块）")
                .defineInRange("dimension_follow_exclude_spawn_radius", 128.0, 0.0, 100000.0);
        SPEC = builder.build();
    }

    public static void register() {
        net.minecraftforge.fml.ModLoadingContext.get()
                .registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, SPEC, "mob_controller/mob_controller-teleport.toml");
    }
}