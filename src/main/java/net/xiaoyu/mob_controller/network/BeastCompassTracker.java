package net.xiaoyu.mob_controller.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.network.PacketDistributor;
import net.xiaoyu.mob_controller.util.MobControlledData;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 服务端侧的「谁在追踪哪只生物」表。
 *
 * <p>每 20 tick 给每个有目标的玩家推一次位置包，客户端拿它画 HUD 指针。
 * 目标死亡、被解除控制、或者换维度了就自动清掉。</p>
 */
public final class BeastCompassTracker {

    private static final Map<UUID, Integer> TARGETS = new ConcurrentHashMap<>();

    private BeastCompassTracker() {
    }

    public static void set(UUID player, int entityId) {
        TARGETS.put(player, entityId);
    }

    public static void clear(UUID player) {
        TARGETS.remove(player);
    }

    public static boolean hasTarget(UUID player) {
        return TARGETS.containsKey(player);
    }

    /** 给某个玩家拼一份当前目标的追踪包（目标无效时 valid = false）。 */
    public static BeastCompassTrackPacket trackOf(ServerPlayer player) {
        Integer id = TARGETS.get(player.getUUID());
        if (id == null) {
            return new BeastCompassTrackPacket(-1, "", 0, 0, 0, false);
        }
        Entity entity = player.serverLevel().getEntity(id);
        if (!(entity instanceof Mob mob)
                || !mob.isAlive()
                || !MobControlledData.isControlledEntity(mob)
                || !Objects.equals(MobControlledData.getControllerUUID(mob), player.getUUID())) {
            TARGETS.remove(player.getUUID());
            return new BeastCompassTrackPacket(-1, "", 0, 0, 0, false);
        }
        return new BeastCompassTrackPacket(id, mob.getDisplayName().getString(),
                mob.getX(), mob.getY(), mob.getZ(), true);
    }

    /** 服务端 tick：每 20 tick 刷一次所有人的指针。 */
    public static void tick(MinecraftServer server) {
        if (TARGETS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, Integer>> iterator = TARGETS.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Integer> entry = iterator.next();
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
