package net.xiaoyu.mob_controller.util;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 骑乘关系快照与恢复。
 *
 * <p>按「乘客 UUID -> 载具 UUID」记录一条或多条骑乘链（支持 A 骑 B、B 骑 C 的
 * 多层嵌套），之后多轮尝试恢复：每轮把能骑上的骑回，重复直到不再变化。</p>
 */
public final class RidingHelper {
    /** passenger UUID -> vehicle UUID。 */
    private final Map<UUID, UUID> pairs = new LinkedHashMap<>();

    /** 快照某个维度内全部已加载实体的骑乘关系。 */
    public void snapshot(ServerLevel level) {
        pairs.clear();
        if (level == null) {
            return;
        }
        for (Entity entity : level.getEntities().getAll()) {
            for (Entity passenger : entity.getPassengers()) {
                pairs.put(passenger.getUUID(), entity.getUUID());
            }
        }
    }

    /** 是否没有可恢复的骑乘关系。 */
    public boolean isEmpty() {
        return pairs.isEmpty();
    }

    /**
     * 多轮恢复骑乘关系。
     *
     * @param server 服务器实例
     */
    public void restore(MinecraftServer server) {
        if (pairs.isEmpty() || server == null) {
            pairs.clear();
            return;
        }
        for (int round = 0; round < 8; round++) {
            boolean changed = false;
            for (Map.Entry<UUID, UUID> entry : pairs.entrySet()) {
                Entity passenger = find(server, entry.getKey());
                Entity vehicle = find(server, entry.getValue());
                if (passenger == null || vehicle == null || passenger.isRemoved() || vehicle.isRemoved()) {
                    continue;
                }
                if (passenger.getVehicle() == vehicle || passenger.isPassenger()) {
                    continue;
                }
                if (passenger.level() != vehicle.level()) {
                    if (!(vehicle.level() instanceof ServerLevel vehicleLevel)) {
                        continue;
                    }
                    Entity moved = passenger.changeDimension(vehicleLevel,
                            new MobDimensionTeleporter(vehicle.getX(), vehicle.getY(), vehicle.getZ()));
                    if (moved == null) {
                        continue;
                    }
                    passenger = moved;
                }
                passenger.stopRiding();
                passenger.startRiding(vehicle, true);
                changed = true;
            }
            if (!changed) {
                break;
            }
        }
        pairs.clear();
    }

    /** 在所有已加载维度中按 UUID 查找实体。 */
    private static Entity find(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntities().get(uuid);
            if (entity != null) {
                return entity;
            }
        }
        return null;
    }
}
