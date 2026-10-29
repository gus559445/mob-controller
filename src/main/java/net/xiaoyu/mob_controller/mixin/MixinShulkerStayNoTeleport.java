package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.monster.Shulker;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Shulker.class)
public abstract class MixinShulkerStayNoTeleport {

    /**
     * 拦截潜影贝的 teleportSomewhere 方法，停留状态下阻止瞬移。
     */
    @Inject(method = "teleportSomewhere", at = @At("HEAD"), cancellable = true)
    private void onTeleportSomewhere(CallbackInfoReturnable<Boolean> cir) {
        Shulker shulker = (Shulker) (Object) this;
        if (MobControlledData.isControlledEntity(shulker) &&
                MobControlledData.getControlMode(shulker) == MobControlledData.ControlMode.STAY) {
            cir.setReturnValue(false);
        }
    }
}