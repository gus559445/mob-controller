package net.xiaoyu.mob_controller.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：当前维度里玩家驯服的生物清单。
 *
 * <p>已经加载的实体给的是实时坐标；区块卸载了的用「最后已知位置」补上，
 * 并用 {@code loaded = false} 标出来。</p>
 */
public record BeastCompassListPacket(List<BeastCompassEntry> entries) {

    /** 列表里的一项。 */

    public BeastCompassListPacket(FriendlyByteBuf buf) {
        this(readEntries(buf));
    }

    private static List<BeastCompassEntry> readEntries(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<BeastCompassEntry> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new BeastCompassEntry(
                    new UUID(buf.readLong(), buf.readLong()),
                    buf.readUtf(64),
                    buf.readUtf(128),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readBoolean()
            ));
        }
        return list;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entries().size());
        for (BeastCompassEntry entry : this.entries()) {
            buf.writeLong(entry.mobId().getMostSignificantBits());
            buf.writeLong(entry.mobId().getLeastSignificantBits());
            buf.writeUtf(entry.name(), 64);
            buf.writeUtf(entry.typeId(), 128);
            buf.writeInt(entry.x());
            buf.writeInt(entry.y());
            buf.writeInt(entry.z());
            buf.writeInt(entry.distance());
            buf.writeBoolean(entry.loaded());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> net.xiaoyu.mob_controller.client.ClientBeastCompass.openScreen(this.entries())));
        ctx.get().setPacketHandled(true);
    }
}
