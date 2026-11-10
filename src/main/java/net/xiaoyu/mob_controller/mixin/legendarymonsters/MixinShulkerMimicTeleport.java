package net.xiaoyu.mob_controller.mixin.legendarymonsters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 传说怪物「潜影拟态者」瞬移时带上乘客。
 *
 * <p>它的传送只改自身坐标，骑在它身上的实体会被落在原地，导致骑乘被拆开。
 * 这里在传送成功后把乘客同步到新位置。</p>
 */
@Pseudo
@Mixin(targets = "net.miauczel.legendary_monsters.entity.AnimatedMonster.Mobs.ShulkerTower.Shulker_MimicEntity")
public abstract class MixinShulkerMimicTeleport {

    /**
     * 传送后同步乘客位置（保留骑乘关系）。
     */
    @Inject(method = "teleport(DDD)Z", at = @At("RETURN"), remap = false, require = 0)
    private void mobController$syncRiders(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        mobController$moveRiders();
    }

    /**
     * 底层传送方法的兜底同步。
     */
    @Inject(method = "teleportBoolean(DDDZ)Z", at = @At("RETURN"), remap = false, require = 0)
    private void mobController$syncRidersFlag(double x, double y, double z, boolean flag, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        mobController$moveRiders();
    }

    private void mobController$moveRiders() {
        Object self = this;
        if (!(self instanceof Mob mob)) {
            return;
        }
        for (Entity rider : mob.getPassengers()) {
            rider.stopRiding();
            rider.setPos(mob.getX(), mob.getY(), mob.getZ());
            rider.startRiding(mob, true);
        }
    }
}
