package net.xiaoyu.mob_controller.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.ai.AiChatService;
import net.xiaoyu.mob_controller.ai.AiChatSessions;
import net.xiaoyu.mob_controller.config.AiChatConfig;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.function.Supplier;

/**
 * 客户端按热键后告诉服务端「我要跟这只生物聊天」。
 *
 * <p>{@code entityId < 0} 表示关闭对话。服务端每次都会重新校验：是生物、
 * 是你控制的、在你身边，才把对话目标记下来 —— 客户端说什么不算数。</p>
 */
public record AiChatTargetPacket(int entityId) {

    public AiChatTargetPacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeInt(this.entityId());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            // 对同一只再按一次 = 收线
            if (this.entityId() >= 0 && this.entityId() == AiChatSessions.targetOf(player.getUUID())) {
                AiChatSessions.clearTarget(player.getUUID());
                player.displayClientMessage(
                        Component.translatable("mob_controller.ai_chat.off").withStyle(ChatFormatting.GRAY), true);
                return;
            }

            if (this.entityId() < 0) {
                AiChatSessions.clearTarget(player.getUUID());
                player.displayClientMessage(
                        Component.translatable("mob_controller.ai_chat.off").withStyle(ChatFormatting.GRAY), true);
                return;
            }

            if (!(player.level().getEntity(this.entityId()) instanceof Mob mob)) {
                return;
            }
            if (!MobControlledData.isControlledEntity(mob)
                    || !player.getUUID().equals(MobControlledData.getControllerUUID(mob))) {
                player.displayClientMessage(
                        Component.translatable("mob_controller.ai_chat.not_yours").withStyle(ChatFormatting.RED), true);
                return;
            }
            if (player.distanceTo(mob) > AiChatConfig.RANGE.get()) {
                player.displayClientMessage(
                        Component.translatable("mob_controller.ai_chat.too_far").withStyle(ChatFormatting.RED), true);
                return;
            }
            if (!AiChatService.isConfigured()) {
                player.displayClientMessage(
                        Component.translatable("mob_controller.ai_chat.not_configured").withStyle(ChatFormatting.RED), true);
                return;
            }

            AiChatSessions.setTarget(player.getUUID(), this.entityId());
            player.displayClientMessage(
                    Component.translatable("mob_controller.ai_chat.on", mob.getDisplayName())
                            .withStyle(ChatFormatting.GREEN),
                    true);
        });
        ctx.get().setPacketHandled(true);
    }
}
