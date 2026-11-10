package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让受控生物可以被骑乘。
 *
 * <p>部分模组生物会重写 {@code couldAcceptPassenger} 返回 false（例如传说怪物的
 * 潜影拟态者），此时即使 {@code startRiding(vehicle, true)} 也会被拒绝，
 * 导致骑乘令、骑乘链恢复等功能对它无效。</p>
 */
@Mixin(Entity.class)
public abstract class MixinEntityAcceptPassenger {

    /**
     * 受控生物一律接受乘客。
     *
     * @param cir 返回值回调
     */
    @Inject(method = "couldAcceptPassenger", at = @At("RETURN"), cancellable = true)
    private void mobController$acceptControlledPassenger(CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue())) {
            return;
        }
        if (!Config.ALLOW_CONTROLLED_MOB_RIDE.get()) {
            return;
        }
        Object self = this;
        if (self instanceof Mob mob && MobControlledData.isControlledEntity(mob)) {
            cir.setReturnValue(true);
        }
    }
}
