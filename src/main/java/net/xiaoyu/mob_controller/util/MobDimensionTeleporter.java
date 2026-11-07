package net.xiaoyu.mob_controller.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.ITeleporter;

import java.util.function.Function;

/**
 * 把受控生物跨维度迁移到指定落点。
 *
 * <p>需要拿到「迁移后的实体」再覆写坐标，因此必须实现 ITeleporter；
 * 这里用顶层类而非匿名类，避免部分运行环境下内部类加载失败。</p>
 */
public class MobDimensionTeleporter implements ITeleporter {
    private final double x;
    private final double y;
    private final double z;

    /**
     * @param x 落点 X
     * @param y 落点 Y
     * @param z 落点 Z
     */
    public MobDimensionTeleporter(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld,
                              float yaw, Function<Boolean, Entity> repositionEntity) {
        Entity placed = repositionEntity.apply(true);
        if (placed != null) {
            placed.moveTo(this.x, this.y, this.z, yaw, placed.getXRot());
            if (placed instanceof Mob mob) {
                mob.setPersistenceRequired();
                ControlledPositionCache.record(mob);
            }
        }
        return placed;
    }
}
