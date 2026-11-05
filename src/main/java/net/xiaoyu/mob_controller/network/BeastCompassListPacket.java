package net.xiaoyu.mob_controller.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：当前维度里玩家驯服的生物清单。
 */
public record BeastCompassListPacket(List<Entry> entries) {

    /** 列表里的一项。 */
    public record Entry(int id, String name, String typeId, int x, int y, int z, int distance) {
    }

    public BeastCompassListPacket(FriendlyByteBuf buf) {
        this(readEntries(buf));
    }

    private static List<Entry> readEntries(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<Entry> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(new Entry(
                    buf.readVarInt(),
                    buf.readUtf(64),
                    buf.readUtf(128),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt(),
                    buf.readInt()
            ));
        }
        return list;
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeVarInt(this.entries().size());
        for (Entry entry : this.entries()) {
            buf.writeVarInt(entry.id());
            buf.writeUtf(entry.name(), 64);
            buf.writeUtf(entry.typeId(), 128);
            buf.writeInt(entry.x());
            buf.writeInt(entry.y());
            buf.writeInt(entry.z());
            buf.writeInt(entry.distance());
        }
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> net.xiaoyu.mob_controller.client.ClientBeastCompass.openScreen(this.entries())));
        ctx.get().setPacketHandled(true);
    }
}
