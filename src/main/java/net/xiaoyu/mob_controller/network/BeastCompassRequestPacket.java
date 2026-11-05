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
public record BeastCompassRequestPacket(boolean deep) {

    public BeastCompassRequestPacket(FriendlyByteBuf buf) {
        this(buf.readBoolean());
    }

    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(this.deep());
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) {
                return;
            }
            ServerLevel level = player.serverLevel();

            // 深度搜索：先把缓存里那些生物所在的区块主动加载出来，
            // 这样远处/被卸载的实体就重新出现在 level.getEntities() 里了
            if (this.deep()) {
                forceLoadCachedChunks(player, level);
            }

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

    /**
     * 把缓存里每只生物所在的区块强制加载一遍。
     *
     * <p>加载上限由 {@code beast_compass_deep_search_chunk_limit} 控制 —— 一次性拉太多区块
     * 会把服务器卡住，所以宁可少搜几只也别把主线程堵死。</p>
     */
    private void forceLoadCachedChunks(ServerPlayer player, ServerLevel level) {
        int limit = net.xiaoyu.mob_controller.Config.BEAST_COMPASS_DEEP_SEARCH_CHUNK_LIMIT.get();
        Set<Long> loaded = new HashSet<>();
        for (ControlledPositionCache.Entry cached : ControlledPositionCache.forOwner(player.getUUID(), level.dimension())) {
            int chunkX = net.minecraft.core.BlockPos.containing(cached.x(), cached.z()).getX() >> 4;
            int chunkZ = net.minecraft.core.BlockPos.containing(cached.x(), cached.z()).getZ() >> 4;
            if (!loaded.add(((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL))) {
                continue;
            }
            if (loaded.size() > limit) {
                break;
            }
            try {
                level.getChunk(chunkX, chunkZ);
            } catch (Exception ignored) {
                // 区块坏了也不能让整个请求挂掉
            }
        }
    }
}
