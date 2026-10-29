package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Monster.class)
public abstract class MixinMonsterDespawn {

    /**
     * 拦截 shouldDespawnInPeaceful，受控生物永不因和平模式消失。
     */
    @Inject(method = "shouldDespawnInPeaceful", at = @At("HEAD"), cancellable = true)
    private void onShouldDespawnInPeaceful(CallbackInfoReturnable<Boolean> cir) {
        Monster self = (Monster) (Object) this;
        if (MobControlledData.isControlledEntity(self)) {
            cir.setReturnValue(false);
        }
    }
}