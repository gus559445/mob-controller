package net.xiaoyu.mob_controller.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 寻兽罗盘：客户端右键后请求一份「当前维度里属于我的受控生物」清单。
 *
 * <p>服务端遍历本维度实体，按距离从近到远排序，回一个
 * {@link BeastCompassListPacket} 让客户端弹列表。</p>
 */
public record BeastCompassRequestPacket() {

    public BeastCompassRequestPacket(FriendlyByteBuf buf) {
        this();
    }

    public void toBytes(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();

            List<BeastCompassListPacket.Entry> entries = new ArrayList<>();
            for (Entity entity : level.getEntities().getAll()) {
                if (!(entity instanceof Mob mob) || !mob.isAlive()) {
                    continue;
                }
                if (!MobControlledData.isControlledEntity(mob)) {
                    continue;
                }
                if (!Objects.equals(MobControlledData.getControllerUUID(mob), player.getUUID())) {
                    continue;
                }
                entries.add(new BeastCompassListPacket.Entry(
                        mob.getId(),
                        mob.getDisplayName().getString(),
                        net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(),
                        mob.blockPosition().getX(),
                        mob.blockPosition().getY(),
                        mob.blockPosition().getZ(),
                        (int) Math.sqrt(mob.distanceToSqr(player))
                ));
            }
            entries.sort(Comparator.comparingInt(BeastCompassListPacket.Entry::distance));

            NetWorkManager.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new BeastCompassListPacket(entries)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
