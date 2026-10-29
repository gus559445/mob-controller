package net.xiaoyu.mob_controller.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WitherBoss.class)
public abstract class MixinWitherBossControlled {

    /**
     * 影子字段：副头的闲置更新时间（原版用于控制随机射击）
     */
    @Shadow
    private int[] nextHeadUpdate;

    /**
     * 每 tick 强制副头目标与主头同步，无目标时禁止副头发射
     */
    @Inject(method = "customServerAiStep", at = @At("HEAD"))
    private void onCustomServerAiStep(CallbackInfo ci) {
        WitherBoss wither = (WitherBoss) (Object) this;
        if (!MobControlledData.isControlledEntity(wither)) {
            return;
        }

        LivingEntity mainTarget = wither.getTarget();

        // 强制副头（索引 1 和 2）的目标与主头一致
        int mainTargetId = mainTarget != null ? mainTarget.getId() : 0;
        for (int i = 1; i <= 2; i++) {
            int current = wither.getAlternativeTarget(i);
            if (mainTargetId != 0 && current != mainTargetId) {
                wither.setAlternativeTarget(i, mainTargetId);
            } else if (mainTargetId == 0 && current != 0) {
                wither.setAlternativeTarget(i, 0);
            }
        }

        // 如果主头没有目标，清零闲置计数器，阻止原版随机发射骷髅头
        if (mainTarget == null) {
            for (int i = 0; i < this.nextHeadUpdate.length; i++) {
                this.nextHeadUpdate[i] = 0;
            }
        }
    }

    /**
     * 拦截副头向实体发射骷髅头（当主头无目标时禁止）
     */
    @Inject(method = "performRangedAttack(ILnet/minecraft/world/entity/LivingEntity;)V",
            at = @At("HEAD"), cancellable = true)
    private void onPerformRangedAttackToEntity(int headIndex, LivingEntity target, CallbackInfo ci) {
        WitherBoss wither = (WitherBoss) (Object) this;
        if (MobControlledData.isControlledEntity(wither) && headIndex > 0 && wither.getTarget() == null) {
            ci.cancel();
        }
    }

    /**
     * 拦截副头向坐标发射骷髅头（当主头无目标时禁止）
     */
    @Inject(method = "performRangedAttack(IDDDZ)V",
            at = @At("HEAD"), cancellable = true)
    private void onPerformRangedAttackToPosition(int headIndex, double x, double y, double z, boolean dangerous, CallbackInfo ci) {
        WitherBoss wither = (WitherBoss) (Object) this;
        if (MobControlledData.isControlledEntity(wither) && headIndex > 0 && wither.getTarget() == null) {
            ci.cancel();
        }
    }
}