package net.xiaoyu.mob_controller.mixin.dominion;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.xiaoyu.mob_controller.util.MobControlledData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 挡住 Dominion 对「本模组控制的生物」的行为接管。
 *
 * <p>Dominion 的跟随 / 停留 / 游荡、自动索敌这些 AI 行为，全都跑在
 * {@code CompanionRegistry.companions(level)} 返回的集合上；而这个集合唯一的入口就是
 * {@code CompanionRegistry.register(entity)}，它只检查实体身上有没有主人标签。</p>
 *
 * <p>我们为了让受控生物能进它的 AI 聊天，确实写了主人标签（见
 * {@code compat/dominion/DominionCompat}），但那会让它的行为系统也来抢同一只生物，
 * 两套跟随逻辑一起推会互相打架。所以在入口这里把受控生物挑出去：
 * <b>聊天照用，AI 行为不接管。</b></p>
 *
 * <p>{@code remap = false} 是因为这是第三方模组的类，方法名不会被混淆；
 * {@code require = 0} 是保险：Dominion 改版本、方法被挪走时只记一条日志，不影响启动。</p>
 */
@Mixin(targets = "com.mcwork.dominion.CompanionRegistry")
public class DominionCompanionRegistryMixin {

    @Inject(method = "register", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void mobController$skipControlledMob(Entity entity, CallbackInfo ci) {
        if (entity instanceof Mob mob && MobControlledData.isControlledEntity(mob)) {
            ci.cancel();
        }
    }
}
