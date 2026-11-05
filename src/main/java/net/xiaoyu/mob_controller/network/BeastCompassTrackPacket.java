package net.xiaoyu.mob_controller.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：追踪目标当前的位置（每 20 tick 一次，或者目标失效时）。
 */
public record BeastCompassTrackPacket(int entityId, String name, double x, double y, double z, boolean valid) {

    public BeastCompassTrackPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readUtf(64), buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entityId());
        buf.writeUtf(this.name(), 64);
        buf.writeDouble(this.x());
        buf.writeDouble(this.y());
        buf.writeDouble(this.z());
        buf.writeBoolean(this.valid());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> net.xiaoyu.mob_controller.client.ClientBeastCompass.onTrack(this)));
        ctx.get().setPacketHandled(true);
    }
}
