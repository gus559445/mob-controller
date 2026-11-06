package net.xiaoyu.mob_controller.util;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraftforge.common.util.ITeleporter;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.MobController;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 受控生物的跨维度自动跟随。
 *
 * <p>控制者切换维度时，将其名下受控生物一并带到新维度。候选来源依次为：</p>
 * <ol>
 *   <li>原维度中已加载的实体（直接传送）；</li>
 *   <li>{@link ControlledPositionCache} 记录的位置（生物曾加载过但当前在未加载区块）；</li>
 *   <li>{@link HighHealthDatabase} 持久化的位置（高生命值受控生物，跨重启有效）。</li>
 * </ol>
 *
 * <p>后两者若开启了 {@code dimension_follow_load_unloaded_chunks}，会在传送前
 * 强制加载对应区块以取回实体，因此可能带来短暂卡顿。</p>
 */
public final class DimensionFollow {
    private static final Logger LOGGER = LogManager.getLogger(MobController.MOD_ID);

    /** 传送前记录的上下文。 */
    private record PendingTravel(ResourceKey<Level> fromDimension, double x, double y, double z) {
    }

    /** playerUUID -> 传送前所在维度与坐标。 */
    private static final Map<UUID, PendingTravel> PENDING = new ConcurrentHashMap<>();

    /** 落点相对玩家的散布半径（格）。 */
    private static final double SPREAD_RADIUS = 2.0;

    private DimensionFollow() {
    }

    /**
     * 在玩家即将跨维度前记录其当前维度与坐标。
     *
     * @param player 即将传送的玩家
     */
    public static void recordBeforeTravel(ServerPlayer player) {
        if (!isEnabled()) {
            return;
        }
        PENDING.put(player.getUUID(), new PendingTravel(
                player.level().dimension(), player.getX(), player.getY(), player.getZ()));
    }

    /**
     * 玩家完成维度切换后，把留在原维度的受控生物带过来。
     *
     * @param player     已完成切换的玩家
     * @param toKey      目标维度
     */
    public static void onChangedDimension(ServerPlayer player, ResourceKey<Level> toKey) {
        PendingTravel pending = PENDING.remove(player.getUUID());
        if (pending == null || !isEnabled()) {
            return;
        }
        if (pending.fromDimension().equals(toKey)) {
            return;
        }
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        ServerLevel fromLevel = server.getLevel(pending.fromDimension());
        ServerLevel toLevel = server.getLevel(toKey);
        if (fromLevel == null || toLevel == null) {
            return;
        }

        List<Mob> candidates = collectCandidates(player, fromLevel, pending);
        if (candidates.isEmpty()) {
            return;
        }

        int limit = Config.DIMENSION_FOLLOW_MAX_MOBS.get();
        if (candidates.size() > limit) {
            candidates = new ArrayList<>(candidates.subList(0, limit));
        }

        int moved = 0;
        for (Mob mob : candidates) {
            if (teleportMob(mob, toLevel, player)) {
                moved++;
            }
        }
        if (moved > 0) {
            LOGGER.debug("Dimension follow: brought {} controlled mob(s) to {}", moved, toKey.location());
        }
    }

    /**
     * 收集需要带走的受控生物。
     */
    private static List<Mob> collectCandidates(ServerPlayer player, ServerLevel fromLevel, PendingTravel pending) {
        UUID ownerUUID = player.getUUID();
        double range = Config.DIMENSION_FOLLOW_RANGE.get();
        boolean limited = range > 0;
        List<Mob> result = new ArrayList<>();
        Set<UUID> seen = new HashSet<>();

        // 1) 原维度中已加载的实体
        for (Entity entity : fromLevel.getEntities().getAll()) {
            if (!(entity instanceof Mob mob)) {
                continue;
            }
            if (!isOwnedBy(mob, ownerUUID)) {
                continue;
            }
            if (limited && mob.distanceToSqr(pending.x(), pending.y(), pending.z()) > range * range) {
                continue;
            }
            if (seen.add(mob.getUUID())) {
                result.add(mob);
            }
        }

        // 2) 未加载区块中的生物：先用内存索引，再用持久化数据库
        if (!Config.DIMENSION_FOLLOW_LOAD_UNLOADED_CHUNKS.get()) {
            return result;
        }
        Set<UUID> missing = new LinkedHashSet<>();
        List<double[]> missingPos = new ArrayList<>();
        for (ControlledPositionCache.Entry cached
                : ControlledPositionCache.forOwner(ownerUUID, pending.fromDimension())) {
            if (seen.contains(cached.mobId())) {
                continue;
            }
            if (limited && distSqr(cached.x(), cached.y(), cached.z(),
                    pending.x(), pending.y(), pending.z()) > range * range) {
                continue;
            }
            missing.add(cached.mobId());
            missingPos.add(new double[]{cached.x(), cached.y(), cached.z()});
        }
        String fromDimId = pending.fromDimension().location().toString();
        for (HighHealthDatabase.PersistedMobPos pos : HighHealthDatabase.listPositions(ownerUUID)) {
            if (!fromDimId.equals(pos.dimension()) || seen.contains(pos.mobUUID())) {
                continue;
            }
            if (limited && distSqr(pos.x(), pos.y(), pos.z(), pending.x(), pending.y(), pending.z()) > range * range) {
                continue;
            }
            if (missing.add(pos.mobUUID())) {
                missingPos.add(new double[]{pos.x(), pos.y(), pos.z()});
            }
        }

        Iterator<double[]> posIt = missingPos.iterator();
        for (UUID mobUUID : missing) {
            double[] pos = posIt.hasNext() ? posIt.next() : null;
            Mob mob = loadAndFindMob(fromLevel, mobUUID, pos);
            if (mob != null && isOwnedBy(mob, ownerUUID) && seen.add(mob.getUUID())) {
                result.add(mob);
            }
        }
        return result;
    }

    /**
     * 强制加载区块后从原维度取回实体。
     */
    private static Mob loadAndFindMob(ServerLevel level, UUID mobUUID, double[] pos) {
        Mob existing = level.getEntities().get(mobUUID) instanceof Mob m ? m : null;
        if (existing != null) {
            return existing;
        }
        if (pos == null) {
            return null;
        }
        try {
            int chunkX = ((int) Math.floor(pos[0])) >> 4;
            int chunkZ = ((int) Math.floor(pos[2])) >> 4;
            level.getChunk(chunkX, chunkZ, ChunkStatus.FULL, true);
            return level.getEntities().get(mobUUID) instanceof Mob m ? m : null;
        } catch (Exception e) {
            LOGGER.warn("Failed to load chunk for controlled mob {}: {}", mobUUID, e.toString());
            return null;
        }
    }

    /**
     * 把生物传送到目标维度的玩家身边。
     *
     * @return 是否成功传送
     */
    private static boolean teleportMob(Mob mob, ServerLevel toLevel, ServerPlayer player) {
        if (mob.level() == toLevel || mob.isRemoved()) {
            return false;
        }
        double x = player.getX() + (mob.getRandom().nextDouble() - 0.5) * SPREAD_RADIUS * 2.0;
        double z = player.getZ() + (mob.getRandom().nextDouble() - 0.5) * SPREAD_RADIUS * 2.0;
        double y = player.getY();
        try {
            Entity placed = mob.changeDimension(toLevel, new ITeleporter() {
                @Override
                public Entity placeEntity(Level level, Entity entity, float yaw, Function<Boolean, Entity> repositionEntity) {
                    Entity result = repositionEntity.apply(true);
                    if (result != null) {
                        result.moveTo(x, y, z, yaw, result.getXRot());
                        if (result instanceof Mob placedMob) {
                            placedMob.setPersistenceRequired();
                            ControlledPositionCache.record(placedMob);
                        }
                    }
                    return result;
                }
            });
            return placed != null;
        } catch (Exception e) {
            LOGGER.error("Failed to move controlled mob {} across dimension: {}", mob.getUUID(), e.toString());
            return false;
        }
    }

    /**
     * 判断生物是否归属于指定控制者且不是召唤物。
     */
    private static boolean isOwnedBy(Mob mob, UUID ownerUUID) {
        if (MobControlledData.isSummoned(mob)) {
            return false;
        }
        return MobControlledData.isControlledEntity(mob)
                && Objects.equals(MobControlledData.getControllerUUID(mob), ownerUUID);
    }

    private static double distSqr(double x1, double y1, double z1, double x2, double y2, double z2) {
        double dx = x1 - x2;
        double dy = y1 - y2;
        double dz = z1 - z2;
        return dx * dx + dy * dy + dz * dz;
    }

    private static boolean isEnabled() {
        return Config.ENABLE_DIMENSION_FOLLOW.get();
    }

    /**
     * 清空待处理上下文（服务器停止时调用）。
     */
    public static void clear() {
        PENDING.clear();
    }
}
