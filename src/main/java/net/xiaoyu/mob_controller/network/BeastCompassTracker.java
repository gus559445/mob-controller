package net.xiaoyu.mob_controller.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.PacketDistributor;
import net.xiaoyu.mob_controller.util.ControlledPositionCache;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端侧的「谁在追踪哪只生物」表。
 *
 * <p>目标用 UUID 记：区块没加载时服务端没有实体对象，只能拿缓存里的最后已知位置。</p>
 *
 * <p>每 20 tick 给每个有目标的玩家推一次位置包，客户端拿它画指针。</p>
 */
public final class BeastCompassTracker {

    /** 表示「没有目标」的哨兵 UUID。 */
    public static final UUID NONE = new UUID(0L, 0L);

    private static final Map<UUID, UUID> TARGETS = new ConcurrentHashMap<>();

    private BeastCompassTracker() {
    }

    public static void set(UUID player, UUID mobId) {
        TARGETS.put(player, mobId);
    }

    public static void clear(UUID player) {
        TARGETS.remove(player);
    }

    public static boolean hasTarget(UUID player) {
        return TARGETS.containsKey(player);
    }

    /** 给某个玩家拼一份当前目标的追踪包（目标没了时 valid = false）。 */
    public static BeastCompassTrackPacket trackOf(ServerPlayer player) {
        UUID mobId = TARGETS.get(player.getUUID());
        if (mobId == null) {
            return new BeastCompassTrackPacket(NONE, "", 0, 0, 0, false, false);
        }

        // 1) 实体还在加载范围里：用实时坐标
        Entity entity = player.serverLevel().getEntities().get(mobId);
        if (entity instanceof Mob mob && mob.isAlive()
                && MobControlledData.isControlledEntity(mob)
                && Objects.equals(MobControlledData.getControllerUUID(mob), player.getUUID())) {
            return new BeastCompassTrackPacket(mobId, mob.getDisplayName().getString(),
                    mob.getX(), mob.getY(), mob.getZ(), true, true);
        }

        // 2) 没加载：退回最后已知位置
        ControlledPositionCache.Entry cached = ControlledPositionCache.get(mobId);
        if (cached != null && cached.ownerId().equals(player.getUUID())
                && cached.dimension().equals(player.level().dimension())) {
            return new BeastCompassTrackPacket(mobId, cached.name(),
                    cached.x(), cached.y(), cached.z(), true, false);
        }

        TARGETS.remove(player.getUUID());
        return new BeastCompassTrackPacket(NONE, "", 0, 0, 0, false, false);
    }

    /** 服务端 tick：每 20 tick 刷一次所有人的指针。 */
    public static void tick(MinecraftServer server) {
        if (TARGETS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, UUID>> iterator = TARGETS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, UUID> entry = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player == null) {
                iterator.remove();
                continue;
            }
            BeastCompassTrackPacket packet = trackOf(player);
            if (!packet.valid()) {
                iterator.remove();
            }
            NetWorkManager.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }
}
