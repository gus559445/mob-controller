package net.xiaoyu.mob_controller.event;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.xiaoyu.mob_controller.ai.AiChatService;
import net.xiaoyu.mob_controller.ai.AiChatSessions;
import net.xiaoyu.mob_controller.config.AiChatConfig;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.Objects;

/**
 * 聊天栏对话的总入口。
 *
 * <p>开了对话之后，玩家在聊天栏直接打字就是「跟这只生物说话」：消息不会广播给别人，
 * 而是转给 AI，回复以 {@code [生物名] 内容} 的形式回到聊天栏。
 * 再按一次热键、走远、或者生物被解除控制，对话自动关闭。</p>
 */
public final class AiChatHandler {

    private AiChatHandler() {
    }

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null || !AiChatConfig.ENABLED.get()) {
            return;
        }

        int targetId = AiChatSessions.targetOf(player.getUUID());
        if (targetId < 0) {
            return;
        }

        // 目标还在不在、还是不是你的、还够不够近 —— 任何一条不满足就自动收线
        if (!(player.level().getEntity(targetId) instanceof Mob mob)
                || !MobControlledData.isControlledEntity(mob)
                || !Objects.equals(MobControlledData.getControllerUUID(mob), player.getUUID())
                || player.distanceTo(mob) > AiChatConfig.RANGE.get()) {
            AiChatSessions.clearTarget(player.getUUID());
            player.displayClientMessage(
                    Component.translatable("mob_controller.ai_chat.off").withStyle(ChatFormatting.GRAY), true);
            return;
        }

        // 拦掉这条公开聊天，它只会变成对话内容
        event.setCanceled(true);

        String text = event.getRawText();
        if (text == null || text.isBlank()) {
            return;
        }

        // 先让玩家看见自己说了什么（cancel 之后原版不会帮忙显示）
        player.sendSystemMessage(Component.literal("")
                .append(Component.literal("你 → " + mob.getDisplayName().getString() + "：").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(text).withStyle(ChatFormatting.WHITE)));

        AiChatService.ask(player, mob, text,
                reply -> {
                    String prefix = AiChatConfig.REPLY_PREFIX.get().formatted(mob.getDisplayName().getString());
                    player.sendSystemMessage(Component.literal(prefix + reply));
                    AiChatSessions.addTurn(mob.getUUID(), text, reply, AiChatConfig.MAX_HISTORY_TURNS.get());
                },
                error -> player.sendSystemMessage(
                        Component.literal("§c[AI] " + error)));
    }

    /** 玩家掉线：把「正在跟谁聊」清掉，别占着目标。 */
    @SubscribeEvent
    public static void onLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        AiChatSessions.forgetPlayer(event.getEntity().getUUID());
    }
}
