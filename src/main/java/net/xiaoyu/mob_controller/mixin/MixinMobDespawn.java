package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.Mob;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MixinMobDespawn {

    @Inject(method = "checkDespawn", at = @At("HEAD"), cancellable = true)
    private void onCheckDespawn(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        if (MobControlledData.isControlledEntity(self)) {
            ci.cancel(); // 受控生物永不因任何原因自然消失
        }
    }
}