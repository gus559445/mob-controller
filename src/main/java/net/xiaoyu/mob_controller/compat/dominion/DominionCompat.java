package net.xiaoyu.mob_controller.compat.dominion;

import net.minecraft.world.entity.Mob;
import net.minecraftforge.fml.ModList;

import java.util.UUID;

/**
 * 和「驯服万物（Dominion）」的对接：让本模组控制的生物也能用它的 AI 聊天。
 *
 * <p>Dominion 的聊天入口（右键快捷键 / 界面）在服务端要先过
 * {@code com.mcwork.dominion.chat.PetChatHandler#resolvePet}，它只认两种东西：
 * 实体是 {@code Mob}，且实体持久化数据里带 {@code dpOwner}（{@code PetData.OWNER}）
 * 这个"主人 UUID"标签。所以这里只做一件事——控制成功时把主人写进去，
 * 解除控制时擦掉。</p>
 *
 * <p>只写一个 NBT 字符串，不引 Dominion 的任何类，它没装时这个方法直接空转，
 * 也不会因为版本变动把本模组带崩。</p>
 *
 * <p>行为接管在 {@code mixin/dominion/DominionCompanionRegistryMixin} 里挡掉了：
 * Dominion 的行为系统只驱动 {@code CompanionRegistry} 收录过的生物，
 * 我们让受控生物永远不被收录，于是聊天能用、AI 行为不会和本模组的控制打架。</p>
 */
public final class DominionCompat {

    /** {@code com.mcwork.dominion.PetData.OWNER} 的值，反编译 Dominion 2.5.15 确认过。 */
    private static final String OWNER_TAG = "dpOwner";

    /** 兼容旧版本用的备用键（PetData.hasOwnerTag 也认这个）。 */
    private static final String LEGACY_OWNER_TAG = "Owner";

    private static final String MOD_ID = "dominion";

    private DominionCompat() {
    }

    /** Dominion 是否装了。没装就什么都不做。 */
    public static boolean isLoaded() {
        return ModList.get().isLoaded(MOD_ID);
    }

    /**
     * 把受控生物登记成 Dominion 眼里的「伙伴」，这样玩家对着它按聊天键就能开聊。
     * 只能在服务端调用：{@code getPersistentData} 是 Forge 的服务端扩展数据。
     */
    public static void markAsPet(Mob mob, UUID owner) {
        if (!isLoaded() || owner == null || mob.level().isClientSide) {
            return;
        }
        mob.getPersistentData().putString(OWNER_TAG, owner.toString());
    }

    /** 解除控制时擦掉标签，别让它一直挂在 Dominion 的伙伴名单里。 */
    public static void unmarkPet(Mob mob) {
        if (!isLoaded() || mob.level().isClientSide) {
            return;
        }
        mob.getPersistentData().remove(OWNER_TAG);
        mob.getPersistentData().remove(LEGACY_OWNER_TAG);
    }
}
