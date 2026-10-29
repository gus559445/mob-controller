package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.monster.EnderMan;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnderMan.class)
public abstract class MixinEnderManStayNoTeleport {

    /**
     * 拦截公开的 teleport() 方法（随机瞬移）
     */
    @Inject(method = "teleport", at = @At("HEAD"), cancellable = true)
    private void onTeleport(CallbackInfoReturnable<Boolean> cir) {
        EnderMan enderman = (EnderMan) (Object) this;
        if (MobControlledData.isControlledEntity(enderman) &&
                MobControlledData.getControlMode(enderman) == MobControlledData.ControlMode.STAY) {
            cir.setReturnValue(false);
        }
    }

    /**
     * 拦截私有的 teleport(double, double, double) 方法（指定坐标瞬移）
     * 注意：方法签名中的参数名和类型必须精确匹配
     */
    @Inject(method = "teleport(DDD)Z", at = @At("HEAD"), cancellable = true)
    private void onTeleportCoords(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        EnderMan enderman = (EnderMan) (Object) this;
        if (MobControlledData.isControlledEntity(enderman) &&
                MobControlledData.getControlMode(enderman) == MobControlledData.ControlMode.STAY) {
            cir.setReturnValue(false);
        }
    }
}