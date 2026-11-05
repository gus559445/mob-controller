package net.xiaoyu.mob_controller.mixin.mutantmore;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 突变烈焰人（Mutant More）被驯服后不再散发灼烧值。
 *
 * <p>它的灼烧是「热源」机制：{@code MutantBlaze implements IHeatSource}，
 * 每 tick 由 {@code HandleEntityHeatEvent} 调 {@code HeatUtils.performHeating}，
 * 遍历范围内的生物时用 {@code IHeatSource#canHeat}
 * 逐个判断「这个受害者能不能被加热」。所以只要在源头把它判死，
 * 周围的玩家和生物就都不会再被烤到，也不用去改受害者的 heat 数据。</p>
 *
 * <p>{@code remap = false} 是第三方模组的方法名不参与混淆；
 * {@code require = 0} 是保险，Mutant More 换版本时只记日志、不影响启动。</p>
 */
@Mixin(targets = "com.alexander.mutantmore.entities.MutantBlaze")
public class MutantBlazeHeatMixin {

    @Inject(method = "canHeat", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mobController$noHeatWhenControlled(LivingEntity target, CallbackInfoReturnable<Boolean> cir) {
        if (MobControlledData.isControlledEntity((Mob) (Object) this)) {
            cir.setReturnValue(false);
        }
    }
}
