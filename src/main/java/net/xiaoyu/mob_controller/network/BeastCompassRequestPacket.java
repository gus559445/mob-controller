package net.xiaoyu.mob_controller.network;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.xiaoyu.mob_controller.util.ControlledPositionCache;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 回响指针：客户端右键后请求一份「当前维度里属于我的受控生物」清单。
 *
 * <p>先收已加载的实体（实时坐标），再用 {@link ControlledPositionCache} 里
 * 「最后已知位置」补上那些区块已经卸载的，这样走多远都能在列表里看到。</p>
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
            Set<UUID> seen = new HashSet<>();

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
                seen.add(mob.getUUID());
                entries.add(new BeastCompassListPacket.Entry(
                        mob.getUUID(),
                        mob.getDisplayName().getString(),
                        BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(),
                        mob.blockPosition().getX(),
                        mob.blockPosition().getY(),
                        mob.blockPosition().getZ(),
                        (int) Math.sqrt(mob.distanceToSqr(player)),
                        true
                ));
            }

            // 区块卸载了的：用最后已知位置补上
            for (ControlledPositionCache.Entry cached : ControlledPositionCache.forOwner(player.getUUID(), level.dimension())) {
                if (seen.contains(cached.mobId())) {
                    continue;
                }
                int distance = (int) Math.sqrt(
                        Math.pow(cached.x() - player.getX(), 2) + Math.pow(cached.z() - player.getZ(), 2));
                entries.add(new BeastCompassListPacket.Entry(
                        cached.mobId(),
                        cached.name(),
                        cached.typeId(),
                        (int) Math.floor(cached.x()),
                        (int) Math.floor(cached.y()),
                        (int) Math.floor(cached.z()),
                        distance,
                        false
                ));
            }

            entries.sort(Comparator.comparingInt(BeastCompassListPacket.Entry::distance));

            NetWorkManager.INSTANCE.send(
                    PacketDistributor.PLAYER.with(() -> player),
                    new BeastCompassListPacket(entries)
            );
        });
        ctx.get().setPacketHandled(true);
    }
}
