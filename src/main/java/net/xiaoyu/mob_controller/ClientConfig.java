package net.xiaoyu.mob_controller;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * 模组客户端配置类，仅影响客户端表现（如音效、渲染等）。
 */
public class ClientConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue PLAY_SOUND_ON_MODE_SWITCH;

    static {
        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
        BUILDER.push("Mob Controller Client Config");
        PLAY_SOUND_ON_MODE_SWITCH = BUILDER
                .comment("直接右键切换受控生物模式（跟随/停留/游荡）时，是否播放经验球拾取音效。只有控制者本人能听到。")
                .define("play_sound_on_mode_switch", true);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, SPEC, "mob_controller/mob_controller-client.toml");
    }
}