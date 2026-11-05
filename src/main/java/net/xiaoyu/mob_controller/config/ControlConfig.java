package net.xiaoyu.mob_controller.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

/**
 * 通用控制规则配置（实验性功能）。
 */
public class ControlConfig {
    public static final ForgeConfigSpec SPEC;
    /**
     * 自定义控制规则列表。
     * 格式：生物ID;物品ID;成功率(0~1);所需绝对血量(≤该值);是否消耗物品(true/false)
     * 示例：minecraft:zombie;minecraft:rotten_flesh;0.5;10;true
     */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> CUSTOM_CONTROL_RULES;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("===================================================");
        builder.comment("  自定义控制规则（实验性）");
        builder.comment("===================================================");
        builder.comment("");

        CUSTOM_CONTROL_RULES = builder
                .comment(
                        "!!! 警告：这是实验性测试功能，不保证兼容，可能导致游戏崩溃或存档损坏 !!!",
                        "重要提醒 1：如果指定的物品本来就和该生物有交互（例如小麦喂牛、骨头驯狼），",
                        "本规则会优先执行控制逻辑，覆盖原本的交互。拿着该物品右键这只生物，",
                        "会尝试控制它，而不是喂食/繁殖/驯服。请谨慎选择物品。",
                        "重要提醒 2：禁止使用本模组自己的物品（命名空间 'mob_controller'，例如 'mob_controller:mob_controller'）。",
                        "使用这类物品时，该规则会被静默忽略，完全不会生效。",
                        "",
                        "格式：'生物ID;物品ID;成功率;所需血量;是否消耗'",
                        "- 生物ID：实体注册名，例如 'minecraft:cow'",
                        "- 物品ID：物品注册名，例如 'minecraft:wheat'（不能是 'mob_controller:xxx'）",
                        "- 成功率：0.0~1.0 的小数，1.0 表示必定成功",
                        "- 所需血量：整数，生物的当前血量必须小于等于这个值才能被控制",
                        "- 是否消耗：true/false，每次尝试是否消耗一个物品（无论成功与否）",
                        "",
                        "示例：'minecraft:cow;minecraft:wheat;1.0;10;true'"
                )
                .defineList("custom_control_rules", List.of(), obj -> obj instanceof String);
        SPEC = builder.build();
    }

    public static void register() {
        net.minecraftforge.fml.ModLoadingContext.get()
                .registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, SPEC, "mob_controller/mob_controller-control.toml");
    }
}