package net.xiaoyu.mob_controller.mixin.aether;

import com.aetherteam.aether.entity.monster.dungeon.boss.ValkyrieQueen;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

@Mixin(ValkyrieQueen.class)
public class ValkyrieQueenMixin {

    /**
     * 正式游戏（SRG 名）里的注入点：{@code Mob#mobInteract} 被混淆成 {@code m_6071_}。
     *
     * <p>{@code remap = false} 让 mixin 别去查映射表——这个类名不属于原版，
     * 注解处理器查不到映射会直接 `error: Unable to locate obfuscation mapping`。</p>
     */
    @Inject(method = "m_6071_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void onMobInteractSrg(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        cancelTradeWhenControlled(player, cir);
    }

    /**
     * 开发环境的注入点：方法保持官方名 {@code mobInteract}。
     *
     * <p>两条都写是因为 refmap 只映射原版类身上的方法，目标类 {@code ValkyrieQueen}
     * 继承来的同名方法不在映射表里，生产环境拿着 {@code mobInteract} 找目标必然落空；
     * 而 mixin 配置是 {@code required: true} + {@code defaultRequire: 1}，
     * 一失败整个游戏启动就崩（就是 1.1.5 那次崩的现场）。
     * 现在两边各自 {@code require = 0}，哪边对不上都只记一条日志。</p>
     */
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true, require = 0)
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        cancelTradeWhenControlled(player, cir);
    }

    /** 女王已被当前玩家控制时，拦住原版那段对话，直接返回 PASS。 */
    private void cancelTradeWhenControlled(Player player, CallbackInfoReturnable<InteractionResult> cir) {
        ValkyrieQueen queen = (ValkyrieQueen) (Object) this;

        // 直接返回 PASS，不影响其他物品的交互（如装备编辑蓝图）
        if (MobControlledData.isControlledEntity(queen) &&
                Objects.equals(MobControlledData.getControllerUUID(queen), player.getUUID())) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}