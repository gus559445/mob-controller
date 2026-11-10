package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.Entity;
import net.xiaoyu.mob_controller.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 瞬移时带上乘客。
 *
 * <p>原版 {@code Entity.teleportTo(double,double,double)} 只改自身坐标，
 * 骑在身上的实体会被落在原地，导致骑乘被拆开（例如传说怪物潜影拟态者的瞬移技能）。
 * 这里在瞬移完成后，把乘客一并搬到新位置并重新骑上。</p>
 */
@Mixin(Entity.class)
public abstract class MixinEntityTeleportRiders {

    /**
     * 瞬移后同步乘客。
     *
     * @param x  目标 X
     * @param y  目标 Y
     * @param z  目标 Z
     * @param ci 回调
     */
    @Inject(method = "teleportTo(DDD)V", at = @At("RETURN"))
    private void mobController$teleportRiders(double x, double y, double z, CallbackInfo ci) {
        if (!Config.TELEPORT_KEEPS_RIDERS.get()) {
            return;
        }
        Object self = this;
        if (!(self instanceof Entity entity) || entity.getPassengers().isEmpty()) {
            return;
        }
        List<Entity> riders = new ArrayList<>(entity.getPassengers());
        for (Entity rider : riders) {
            rider.stopRiding();
            rider.setPos(entity.getX(), entity.getY(), entity.getZ());
            rider.startRiding(entity, true);
        }
    }
}
