package net.xiaoyu.mob_controller.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/**
 * AI 聊天配置。
 *
 * <p>走 OpenAI 兼容的 {@code /chat/completions} 接口，填个地址 + Key + 模型名就能用；
 * 换成中转站、本地 ollama（{@code http://127.0.0.1:11434/v1/chat/completions}）也一样。</p>
 *
 * <p>本配置文件位于：{@code mob_controller/mob_controller-ai-chat.toml}</p>
 */
public class AiChatConfig {

    public static final ForgeConfigSpec SPEC;

    /** 总开关。关掉之后热键与聊天拦截全部失效。 */
    public static final ForgeConfigSpec.BooleanValue ENABLED;

    /** 接口地址，OpenAI 兼容格式。 */
    public static final ForgeConfigSpec.ConfigValue<String> API_URL;

    /** API Key（Bearer）。本地 ollama 之类的可以留空。 */
    public static final ForgeConfigSpec.ConfigValue<String> API_KEY;

    /** 模型名。 */
    public static final ForgeConfigSpec.ConfigValue<String> MODEL;

    /** 采样温度。 */
    public static final ForgeConfigSpec.DoubleValue TEMPERATURE;

    /** 单次回复的最大 token。 */
    public static final ForgeConfigSpec.IntValue MAX_TOKENS;

    /** 请求超时（秒）。 */
    public static final ForgeConfigSpec.IntValue TIMEOUT_SECONDS;

    /** 记住最近多少轮对话。 */
    public static final ForgeConfigSpec.IntValue MAX_HISTORY_TURNS;

    /** 对话最大距离（格），超出会自动断开。 */
    public static final ForgeConfigSpec.DoubleValue RANGE;

    /** 回复前缀，{@code %s} 会被替换成生物名字。 */
    public static final ForgeConfigSpec.ConfigValue<String> REPLY_PREFIX;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("AI 聊天：驯服后的生物可以像伙伴一样和你对话").push("ai_chat");

        ENABLED = builder
                .comment("总开关")
                .define("enabled", true);

        API_URL = builder
                .comment("OpenAI 兼容的 chat/completions 地址")
                .define("api_url", "https://api.openai.com/v1/chat/completions");

        API_KEY = builder
                .comment("API Key；本地服务不需要可以留空")
                .define("api_key", "");

        MODEL = builder
                .comment("模型名")
                .define("model", "gpt-4o-mini");

        TEMPERATURE = builder
                .comment("采样温度，0~2")
                .defineInRange("temperature", 0.8D, 0.0D, 2.0D);

        MAX_TOKENS = builder
                .comment("单次回复最大 token")
                .defineInRange("max_tokens", 200, 16, 4096);

        TIMEOUT_SECONDS = builder
                .comment("请求超时秒数")
                .defineInRange("timeout_seconds", 20, 3, 120);

        MAX_HISTORY_TURNS = builder
                .comment("记住最近多少轮对话（一问一答算一轮）")
                .defineInRange("max_history_turns", 6, 0, 50);

        RANGE = builder
                .comment("对话最大距离（格）")
                .defineInRange("range", 32.0D, 4.0D, 128.0D);

        REPLY_PREFIX = builder
                .comment("回复前缀，%s 替换成生物名字")
                .define("reply_prefix", "§b[%s] §f");

        builder.pop();
        SPEC = builder.build();
    }

    private AiChatConfig() {
    }

    /**
     * 注册本配置文件到 Forge 配置系统。
     */
    public static void register() {
        ModLoadingContext.get()
                .registerConfig(ModConfig.Type.SERVER, SPEC, "mob_controller/mob_controller-ai-chat.toml");
    }

    /** 配置是否已经能真正发出请求（开关打开 + 有地址 + 有模型）。 */
    public static boolean usable() {
        return ENABLED.get()
                && !API_URL.get().isBlank()
                && !MODEL.get().isBlank();
    }
}
