package net.xiaoyu.mob_controller.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

/**
 * 物品相关配置（最大乘客数、配方开关）。
 */
public class ItemConfig {
    public static final ForgeConfigSpec SPEC;

    /**
     * 为特定实体类型单独配置最大乘客数。
     * 格式：'entity_id;max_count'，例如 'minecraft:camel;2'。
     * 未在此列表中的实体默认最大乘客数为 1。
     *
     * ⚠️ NOTE: 此配置仅对“骑乘之笛”（Ride Command item）有效，
     * 不影响其他骑乘方式（如空手右键直接骑乘、原版马鞍等）。
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> MAX_RIDERS_PER_MOUNT;

    /**
     * 是否启用五谷杂粮的合成表。
     * true = 五谷杂粮可合成（生物控制器不可合成）；
     * false = 生物控制器可合成（五谷杂粮不可合成），默认为 false。
     */
    public static final ForgeConfigSpec.BooleanValue USE_GRAIN_RECIPE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("===================================================");
        builder.comment("  物品与配方");
        builder.comment("===================================================");
        builder.comment("");

        MAX_RIDERS_PER_MOUNT = builder
                .comment(
                        "设置某种实体最多能被多少名骑手骑乘。格式：'实体ID;数量'。",
                        "示例：'minecraft:camel;2'（骆驼支持 2 名骑手）。",
                        "未列出的实体默认只允许 1 名骑手。",
                        "",
                        "注意：本配置只影响「骑兵之笛」物品。",
                        "对原版骑乘（例如给马装鞍）以及直接右键骑乘都不生效。",
                        ""
                )
                .defineList("max_riders_per_mount", List.of("minecraft:camel;2"), obj -> obj instanceof String);
        builder.comment("");

        USE_GRAIN_RECIPE = builder
                .comment("为 true 时「五谷杂粮」有合成配方、普通「不竭金羹」不可合成；为 false 时相反。")
                .define("use_grain_recipe", true);
        SPEC = builder.build();
    }

    public static void register() {
        net.minecraftforge.fml.ModLoadingContext.get()
                .registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, SPEC, "mob_controller/mob_controller-items.toml");
    }
}