package net.xiaoyu.mob_controller.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：追踪目标当前的位置。
 *
 * <p>{@code loaded = false} 表示目标所在区块没加载，坐标是它最后出现的位置。</p>
 */
public record BeastCompassTrackPacket(UUID mobId, String name, double x, double y, double z,
                                     boolean valid, boolean loaded) {

    public BeastCompassTrackPacket(FriendlyByteBuf buf) {
        this(buf.readUUID(), buf.readUtf(64), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                buf.readBoolean(), buf.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeUUID(this.mobId());
        buf.writeUtf(this.name(), 64);
        buf.writeDouble(this.x());
        buf.writeDouble(this.y());
        buf.writeDouble(this.z());
        buf.writeBoolean(this.valid());
        buf.writeBoolean(this.loaded());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> net.xiaoyu.mob_controller.client.ClientBeastCompass.onTrack(this)));
        ctx.get().setPacketHandled(true);
    }
}
