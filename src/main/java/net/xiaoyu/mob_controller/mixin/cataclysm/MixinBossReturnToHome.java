package net.xiaoyu.mob_controller.mixin.cataclysm;

import net.minecraft.world.entity.Mob;
import net.xiaoyu.mob_controller.Config;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 阻止已受控的灾变 Boss「回家」。
 *
 * <p>灾变模组的 Boss 基类 {@code LLibrary_Boss_Monster} 每刻会调用
 * {@code ReturnToHome()}：一旦当前维度与该 Boss 记录的家乡维度不一致，
 * 它会自行 {@code changeDimension} 传送回竞技场，随后被移动到
 * {@code homePos}。这会让被玩家带到其它维度的 Boss 看起来「凭空消失」。</p>
 *
 * <p>这里在方法头拦截：若该 Boss 已被 MobController 控制，则取消回家逻辑，
 * 使其能像普通受控生物一样跟随控制者跨维度活动。</p>
 *
 * <p>使用 {@link Pseudo} 与字符串 {@code targets}，因此本类不依赖灾变模组
 * 参与编译；未安装灾变时该混入会被自动跳过。</p>
 */
@Pseudo
@Mixin(targets = "com.github.L_Ender.cataclysm.entity.AnimationMonster.BossMonsters.LLibrary_Boss_Monster")
public abstract class MixinBossReturnToHome {

    /**
     * 受控 Boss 跳过回家传送。
     *
     * @param ci 注入回调
     */
    @Inject(method = "ReturnToHome", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void mobController$skipReturnHomeWhenControlled(CallbackInfo ci) {
        Object self = this;
        if (Config.PREVENT_CONTROLLED_BOSS_RETURN_HOME.get()
                && self instanceof Mob mob
                && MobControlledData.isControlledEntity(mob)) {
            ci.cancel();
        }
    }
}
