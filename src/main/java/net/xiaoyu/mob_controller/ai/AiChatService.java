package net.xiaoyu.mob_controller.ai;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.config.AiChatConfig;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * 向 OpenAI 兼容接口要一句回复。
 *
 * <p>全程异步：请求在独立线程池里跑，拿到结果再用 {@code server.execute} 回主线程，
 * 不碰服务器 tick。失败一律走 {@code onError}，让调用方决定怎么提示玩家。</p>
 */
public final class AiChatService {

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static final ExecutorService POOL = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "mob-controller-ai");
        thread.setDaemon(true);
        return thread;
    });

    private AiChatService() {
    }

    public static boolean isConfigured() {
        return AiChatConfig.usable();
    }

    /**
     * 问一句，答一句。
     *
     * @param player    发起对话的玩家
     * @param mob       被对话的生物
     * @param userText  玩家输入的原话
     * @param onReply   成功回调（主线程），参数是 AI 的回复
     * @param onError   失败回调（主线程），参数是给玩家看的短原因
     */
    public static void ask(ServerPlayer player, Mob mob, String userText,
                           Consumer<String> onReply, Consumer<String> onError) {
        if (!isConfigured()) {
            onError.accept("AI 聊天还没配置好（mob_controller-ai-chat.toml）");
            return;
        }

        final MinecraftServer server = player.server;
        final String url = AiChatConfig.API_URL.get();
        final String key = AiChatConfig.API_KEY.get();
        final int timeout = AiChatConfig.TIMEOUT_SECONDS.get();
        final String body = buildRequestBody(player, mob, userText);

        POOL.submit(() -> {
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                        .timeout(Duration.ofSeconds(timeout))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
                if (!key.isBlank()) {
                    builder.header("Authorization", "Bearer " + key);
                }

                HttpResponse<String> response = HTTP.send(builder.build(),
                        HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (response.statusCode() / 100 != 2) {
                    deliver(server, onError, "接口返回 HTTP " + response.statusCode() + "：" + clip(response.body(), 160));
                    return;
                }

                String content = parseContent(response.body());
                if (content == null || content.isBlank()) {
                    deliver(server, onError, "接口没有返回内容");
                    return;
                }
                deliver(server, onReply, content.trim());
            } catch (Exception exception) {
                MobController.LOGGER.warn("AI 聊天请求失败", exception);
                deliver(server, onError, "请求失败：" + exception.getClass().getSimpleName());
            }
        });
    }

    /** 回调统一切回主线程执行。 */
    private static void deliver(MinecraftServer server, Consumer<String> consumer, String value) {
        server.execute(() -> consumer.accept(value));
    }

    /** 组 OpenAI 兼容的请求体：system + 历史 + 这次说的话。 */
    private static String buildRequestBody(ServerPlayer player, Mob mob, String userText) {
        JsonObject root = new JsonObject();
        root.addProperty("model", AiChatConfig.MODEL.get());
        root.addProperty("temperature", AiChatConfig.TEMPERATURE.get());
        root.addProperty("max_tokens", AiChatConfig.MAX_TOKENS.get());
        root.addProperty("stream", false);

        JsonArray messages = new JsonArray();
        messages.add(message("system", systemPrompt(player, mob)));

        List<AiChatSessions.Message> history = AiChatSessions.history(mob.getUUID());
        for (AiChatSessions.Message turn : history) {
            messages.add(message(turn.role(), turn.content()));
        }
        messages.add(message("user", userText));

        root.add("messages", messages);
        return root.toString();
    }

    private static JsonObject message(String role, String content) {
        JsonObject object = new JsonObject();
        object.addProperty("role", role);
        object.addProperty("content", content);
        return object;
    }

    /** 给 AI 的人设：它是谁、现在什么状态、说话要多短。 */
    private static String systemPrompt(ServerPlayer player, Mob mob) {
        String mode = switch (MobControlledData.getControlMode(mob)) {
            case FOLLOW -> "跟随中";
            case STAY -> "停留在原地";
            case WANDER -> "自由游荡";
        };
        return """
                你是 Minecraft 里的一只「%s」（%s），被玩家「%s」驯服并一直跟着他冒险。
                说话要求：
                1. 用中文口语，1~3 句，控制在 80 个字以内；
                2. 有点性格：忠诚、爱表现、偶尔抱怨主人跑太快，但绝不顶撞主人；
                3. 不要用 Markdown、不要列表、不要加引号包住整句；
                4. 不要假装能做成游戏里做不到的事（比如给你物品、传送你）；
                5. 只输出角色说的话本身。
                当前状态：%s，生命 %.0f/%.0f，和主人相距 %.1f 格。
                """.formatted(
                mob.getDisplayName().getString(),
                mob.getType().toShortString(),
                player.getName().getString(),
                mode,
                mob.getHealth(),
                mob.getMaxHealth(),
                player.distanceTo(mob)
        );
    }

    /** 从响应里抠出 {@code choices[0].message.content}。 */
    private static String parseContent(String responseBody) {
        try {
            JsonObject root = JsonParser.parseString(responseBody).getAsJsonObject();
            JsonArray choices = root.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                return null;
            }
            JsonObject message = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (message == null || !message.has("content")) {
                return null;
            }
            return message.get("content").getAsString();
        } catch (Exception exception) {
            MobController.LOGGER.warn("解析 AI 回复失败", exception);
            return null;
        }
    }

    private static String clip(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "…";
    }
}
