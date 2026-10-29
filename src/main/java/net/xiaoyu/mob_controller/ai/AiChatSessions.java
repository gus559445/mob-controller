package net.xiaoyu.mob_controller.ai;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 聊天的会话状态：谁正在跟哪只生物聊天，以及每只生物记得多少上下文。
 *
 * <p>全部放内存（服务端重启就清空），不写实体 NBT —— 聊天记录没必要跟着存档跑，
 * 也免得把存档撑大。</p>
 */
public final class AiChatSessions {

    /** 玩家 → 正在对话的生物实体 id（-1 / 不存在表示没开）。 */
    private static final Map<UUID, Integer> TARGETS = new ConcurrentHashMap<>();

    /** 生物 UUID → 最近若干轮对话。 */
    private static final Map<UUID, Deque<Message>> HISTORY = new ConcurrentHashMap<>();

    private AiChatSessions() {
    }

    /** 一轮对话的一条消息。 */
    public record Message(String role, String content) {
    }

    public static void setTarget(UUID player, int entityId) {
        TARGETS.put(player, entityId);
    }

    public static int targetOf(UUID player) {
        Integer id = TARGETS.get(player);
        return id == null ? -1 : id;
    }

    public static boolean hasTarget(UUID player) {
        return targetOf(player) >= 0;
    }

    public static void clearTarget(UUID player) {
        TARGETS.remove(player);
    }

    /** 取这只生物的历史（按时间顺序），最多保留配置里的轮数。 */
    public static List<Message> history(UUID mob) {
        Deque<Message> deque = HISTORY.get(mob);
        return deque == null ? List.of() : new ArrayList<>(deque);
    }

    /** 记一轮问答，超出上限就丢掉最早的。 */
    public static void addTurn(UUID mob, String userText, String replyText, int maxTurns) {
        if (maxTurns <= 0) {
            return;
        }
        Deque<Message> deque = HISTORY.computeIfAbsent(mob, k -> new ArrayDeque<>());
        synchronized (deque) {
            deque.addLast(new Message("user", userText));
            deque.addLast(new Message("assistant", replyText));
            while (deque.size() > maxTurns * 2) {
                deque.removeFirst();
            }
        }
    }

    /** 生物没了就把它的记录一起丢掉。 */
    public static void forget(UUID mob) {
        HISTORY.remove(mob);
    }

    /** 退出 / 掉线时清理玩家那一侧的状态。 */
    public static void forgetPlayer(UUID player) {
        TARGETS.remove(player);
    }
}
