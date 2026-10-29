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
     * 注入 {@code Mob#mobInteract}。
     *
     * <p>这里必须把两个名字都写上：开发环境里 Minecraft 方法叫 {@code mobInteract}，
     * 而正式游戏里是 SRG 名 {@code m_6071_}。refmap 只会映射原版类身上的方法，
     * 目标类是 Aether 的 {@code ValkyrieQueen}（继承来的 mobInteract 不在映射表里），
     * 于是运行时拿着 {@code mobInteract} 这个名字找不到任何目标 ——
     * mixin 配置是 {@code required: true} + {@code defaultRequire: 1}，一失败就是整包启动崩溃。</p>
     *
     * <p>{@code require = 0} 是最后一道保险：Aether 换版本、方法被挪走时只记一条日志，
     * 不让游戏跟着陪葬。</p>
     */
    @Inject(method = {"mobInteract", "m_6071_"}, at = @At("HEAD"), cancellable = true, require = 0)
    private void onMobInteract(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        ValkyrieQueen queen = (ValkyrieQueen) (Object) this;

        // 如果女王已被当前玩家控制，则阻止弹出对话
        if (MobControlledData.isControlledEntity(queen) &&
                Objects.equals(MobControlledData.getControllerUUID(queen), player.getUUID())) {
            // 直接返回 PASS，不影响其他物品的交互（如装备编辑蓝图）
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}