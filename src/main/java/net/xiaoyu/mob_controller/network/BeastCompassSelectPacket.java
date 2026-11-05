package net.xiaoyu.mob_controller.network;

import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * 客户端在列表里点了某只生物：服务端记下追踪目标。
 */
public record BeastCompassSelectPacket(int entityId) {

    public BeastCompassSelectPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }

            if (this.entityId() < 0) {
                BeastCompassTracker.clear(player.getUUID());
                player.displayClientMessage(
                        Component.translatable("mob_controller.beast_compass.cleared").withStyle(ChatFormatting.GRAY),
                        true);
                return;
            }

            Entity entity = player.serverLevel().getEntity(this.entityId());
            if (!(entity instanceof Mob mob)
                    || !MobControlledData.isControlledEntity(mob)
                    || !Objects.equals(MobControlledData.getControllerUUID(mob), player.getUUID())) {
                player.displayClientMessage(
                        Component.translatable("mob_controller.beast_compass.not_yours").withStyle(ChatFormatting.RED),
                        true);
                return;
            }

            BeastCompassTracker.set(player.getUUID(), this.entityId());
            NetWorkManager.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    BeastCompassTracker.trackOf(player)
            );
            player.displayClientMessage(
                    Component.translatable("mob_controller.beast_compass.selected", mob.getDisplayName())
                            .withStyle(ChatFormatting.GREEN),
                    true);
        });
        ctx.get().setPacketHandled(true);
    }
}
