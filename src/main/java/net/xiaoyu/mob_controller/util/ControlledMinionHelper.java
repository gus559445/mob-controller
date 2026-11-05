package net.xiaoyu.mob_controller.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * 受控生物的「召唤物」识别与拦阻。
 *
 * <p>模组生态里召唤物记录主人的方式五花八门，这里只认最常见的那几个 NBT 字段：
 * {@code Owner / OwnerUUID / Summoner / SummonerUUID}（大小写都试）。
 * 判定成立后给实体打一个标记，之后它再想把玩家设成攻击目标就会被拦下来。</p>
 *
 * <p>标记打在实体自己的持久化数据里，跟着实体走，不需要额外维护映射表。</p>
 */
public final class ControlledMinionHelper {

    /** 标记：这只生物是「受控生物召唤出来的仆从」。 */
    private static final String TAG_MINION = "mob_controller_controlled_minion";

    /** 召唤者/主人可能出现的 NBT 键名，大小写都覆盖一遍。 */
    private static final String[] OWNER_KEYS = {
            "Owner", "owner", "OwnerUUID", "ownerUUID",
            "Summoner", "summoner", "SummonerUUID", "summonerUUID"
    };

    private ControlledMinionHelper() {
    }

    /**
     * 实体刚进世界时调用：如果它的召唤者是某只受控生物，就给它打上标记。
     *
     * <p>只在刚生成的这一刻判断一次，避免每 tick 扫 NBT。</p>
     */
    public static void markIfSummonedByControlledMob(Entity entity) {
        if (!(entity instanceof Mob mob) || mob.level().isClientSide) {
            return;
        }
        if (!(mob.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // 方式一：生物自己带 Owner/Summoner 这类 NBT，且指向受控生物
        UUID ownerId = findOwnerUuid(mob);
        if (ownerId != null) {
            Entity owner = serverLevel.getEntity(ownerId);
            // 召唤者可能在别的维度或已卸载，那就认不出来，落到方式二去试
            if (owner instanceof Mob ownerMob && MobControlledData.isControlledEntity(ownerMob)) {
                mob.getPersistentData().putBoolean(TAG_MINION, true);
                return;
            }
        }

        // 方式二：玩家在 controlled_minion_entities 名单里点名了这种实体，
        // 那就在它出生时看附近有没有受控生物，有就当作它的召唤物
        if (isListedAsMinion(mob) && hasControlledMobNearby(serverLevel, mob)) {
            mob.getPersistentData().putBoolean(TAG_MINION, true);
        }
    }

    /** 实体类型是否在 controlled_minion_entities 名单里（支持 "*"）。 */
    private static boolean isListedAsMinion(Mob mob) {
        List<? extends String> list = net.xiaoyu.mob_controller.Config.CONTROLLED_MINION_ENTITIES.get();
        if (list.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        if (id == null) {
            return false;
        }
        String key = id.toString();
        for (String entry : list) {
            if (entry == null) {
                continue;
            }
            if ("*".equals(entry.trim()) || entry.trim().equalsIgnoreCase(key)) {
                return true;
            }
        }
        return false;
    }

    /** 16 格内有没有受控生物。 */
    private static boolean hasControlledMobNearby(ServerLevel level, Mob minion) {
        return !level.getEntitiesOfClass(
                Mob.class,
                minion.getBoundingBox().inflate(16.0D),
                other -> other != minion && MobControlledData.isControlledEntity(other)
        ).isEmpty();
    }

    /** 这只生物是不是「受控生物的召唤物」。 */
    public static boolean isControlledMinion(Entity entity) {
        return entity.getPersistentData().getBoolean(TAG_MINION);
    }

    /**
     * 要不要拦下这次目标设置：受控召唤物锁定玩家，且配置开关打开。
     */
    public static boolean shouldBlockPlayerTarget(Mob mob, @Nullable Entity target) {
        if (!(target instanceof Player)) {
            return false;
        }
        if (!net.xiaoyu.mob_controller.Config.PREVENT_CONTROLLED_MINION_ATTACKING_PLAYERS.get()) {
            return false;
        }
        return isControlledMinion(mob);
    }

    /**
     * 从实体自身的 NBT 里翻出召唤者 UUID。
     *
     * <p>用 {@code saveWithoutId} 而不是 {@code getPersistentData}，因为有相当多模组
     * （以及原版驯服动物）把 Owner 写在实体正式 NBT 里，ForgeData 里是空的。</p>
     */
    @Nullable
    private static UUID findOwnerUuid(Mob mob) {
        CompoundTag data = mob.saveWithoutId(new CompoundTag());
        for (String key : OWNER_KEYS) {
            if (!data.contains(key, 8)) {
                continue;
            }
            try {
                return UUID.fromString(data.getString(key));
            } catch (IllegalArgumentException ignored) {
                // 不是合法 UUID 就继续找下一个键
            }
        }
        return null;
    }
}
