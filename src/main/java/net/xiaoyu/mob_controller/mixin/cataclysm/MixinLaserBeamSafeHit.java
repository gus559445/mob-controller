package net.xiaoyu.mob_controller.mixin.cataclysm;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 灾变「激光束」命中处理的健壮性修复。
 *
 * <p>灾变的 {@code Laser_Beam_Entity.onHitEntity} 会把 {@code getOwner()}
 * 直接强转为 {@link LivingEntity}。当发射者不是生物（例如两发激光互相持有、
 * 或 owner 链被其它模组改写）时会抛 {@code ClassCastException}，
 * 导致服务端在实体 tick 时直接崩溃。</p>
 *
 * <p>这里在方法头拦截：owner 不是生物时跳过本次命中处理。</p>
 */
@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.projectile.Laser_Beam_Entity")
public abstract class MixinLaserBeamSafeHit {

    /**
     * 避开 owner 强转崩溃。
     *
     * @param result 命中结果
     * @param ci     注入回调
     */
    @Inject(method = "m_5790_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mobController$guardLaserOwner(EntityHitResult result, CallbackInfo ci) {
        Object self = this;
        if (self instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            if (!(owner instanceof LivingEntity)) {
                ci.cancel();
            }
        }
    }
}
