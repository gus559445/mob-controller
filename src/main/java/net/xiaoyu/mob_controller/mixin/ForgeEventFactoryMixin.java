package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Panda;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.ForgeEventFactory;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * Forge 事件工厂注入。
 *
 * <p>受控生物（及其弹射物）触发的 mobGriefing 查询统一返回禁止，
 * 但猪灵（AbstractPiglin 及其子类）和熊猫除外。</p>
 */
@Mixin(ForgeEventFactory.class)
public class ForgeEventFactoryMixin {

    /**
     * 注入 {@code getMobGriefingEvent} 头部：受控生物相关来源返回 false，
     * 但猪灵和熊猫不受限制。
     */
    @Inject(method = "getMobGriefingEvent", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onGetMobGriefingEvent(Level level, @Nullable Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (entity instanceof Mob mob) {
            if (MobControlledData.isControlledEntity(mob)) {
                // 猪灵（包括 Piglin 和 PiglinBrute）以及熊猫除外
                if (!(mob instanceof AbstractPiglin || mob instanceof Panda)) {
                    cir.setReturnValue(false);
                }
            }
        } else if (entity instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (owner instanceof Mob mob && MobControlledData.isControlledEntity(mob)) {
                if (!(mob instanceof AbstractPiglin || mob instanceof Panda)) {
                    cir.setReturnValue(false);
                }
            }
        }
    }
}