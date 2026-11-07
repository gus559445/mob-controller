package net.xiaoyu.mob_controller.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.xiaoyu.mob_controller.util.ControlledPositionCache;

import java.util.UUID;
import java.util.function.Supplier;
import net.xiaoyu.mob_controller.util.PositionEntry;

/**
 * 客户端在列表里点了某只生物：服务端记下追踪目标。
 *
 * <p>用生物 UUID 而不是实体 id —— 远处区块没加载的生物在服务端根本没有实体对象，
 * 只有缓存里的 UUID 和最后已知位置。</p>
 */
public record BeastCompassSelectPacket(UUID mobId, String name) {

    public BeastCompassSelectPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readUtf(64));
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.mobId());
        buf.writeUtf(this.name(), 64);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            // 取消追踪
            if (this.mobId() == null || this.mobId().equals(BeastCompassTracker.NONE)) {
                BeastCompassTracker.clear(player.getUUID());
                player.displayClientMessage(
                        Component.translatable("mob_controller.beast_compass.cleared").withStyle(ChatFormatting.GRAY),
                        true);
                return;
            }

            PositionEntry cached = ControlledPositionCache.get(this.mobId());
            if (cached == null) {
                player.displayClientMessage(
                        Component.translatable("mob_controller.beast_compass.not_yours").withStyle(ChatFormatting.RED),
                        true);
                return;
            }

            BeastCompassTracker.set(player.getUUID(), this.mobId());
            NetWorkManager.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    BeastCompassTracker.trackOf(player)
            );
            player.displayClientMessage(
                    Component.translatable("mob_controller.beast_compass.selected", cached.name())
                            .withStyle(ChatFormatting.GREEN),
                    true);
        });
        ctx.get().setPacketHandled(true);
    }
}
