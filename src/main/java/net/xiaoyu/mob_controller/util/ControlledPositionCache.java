package net.xiaoyu.mob_controller.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 受控生物的「最后已知位置」缓存。
 *
 * <p>Minecraft 只让已加载区块里的实体存在于服务端，区块一卸载生物就从
 * {@code level.getEntities()} 里消失 —— 回响指针原本只能看到加载范围内的那些，
 * 走远一点就找不到了。</p>
 *
 * <p>这里在生物还加载着的时候（{@code onLivingTickHeal} 每 20 tick）把坐标记下来，
 * 卸载之后指针仍然能指向它最后出现的位置。缓存只在解除控制或死亡时清除，
 * 不过期 —— 「丢了很久的伙伴」正是要找的目标。</p>
 */
public final class ControlledPositionCache {


    private static final Map<UUID, PositionEntry> CACHE = new ConcurrentHashMap<>();

    private ControlledPositionCache() {
    }

    /** 生物还加载着的时候调，把位置刷新进缓存。 */
    public static void record(Mob mob) {
        UUID owner = MobControlledData.getControllerUUID(mob);
        if (owner == null) {
            return;
        }
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
        CACHE.put(mob.getUUID(), new PositionEntry(
                mob.getUUID(),
                owner,
                mob.level().dimension(),
                mob.getX(), mob.getY(), mob.getZ(),
                mob.getDisplayName().getString(),
                typeId == null ? "minecraft:unknown" : typeId.toString()
        ));
    }

    /** 解除控制 / 死亡时清掉。 */
    public static void forget(UUID mobId) {
        CACHE.remove(mobId);
    }

    /** 取某个玩家在某个维度里的全部已知位置。 */
    public static List<PositionEntry> forOwner(UUID owner, ResourceKey<Level> dimension) {
        List<PositionEntry> list = new ArrayList<>();
        for (PositionEntry entry : CACHE.values()) {
            if (entry.ownerId().equals(owner) && entry.dimension().equals(dimension)) {
                list.add(entry);
            }
        }
        return list;
    }

    /** 单只生物的最后已知位置。 */
    @Nullable
    public static PositionEntry get(UUID mobId) {
        return CACHE.get(mobId);
    }
}
