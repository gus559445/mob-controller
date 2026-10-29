package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.item.ItemEntity;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Panda.class)
public abstract class MixinPanda {

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        Panda panda = (Panda) (Object) this;
        if (MobControlledData.isControlledEntity(panda) && panda.getFirstPassenger() != null) {
            if (panda.isSitting()) panda.sit(false);
            if (panda.isOnBack()) panda.setOnBack(false);
            if (panda.isRolling()) panda.roll(false);
            if (panda.isEating()) panda.eat(false);
            if (panda.isSneezing()) panda.sneeze(false);
        }
    }

    @Inject(method = "canPerformAction", at = @At("RETURN"), cancellable = true)
    private void onCanPerformAction(CallbackInfoReturnable<Boolean> cir) {
        Panda panda = (Panda) (Object) this;
        if (MobControlledData.isControlledEntity(panda) && panda.getFirstPassenger() != null) {
            cir.setReturnValue(false);
        }
    }
}