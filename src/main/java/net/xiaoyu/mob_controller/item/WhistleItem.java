package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.network.ApplyControlCommandPacket;
import net.xiaoyu.mob_controller.network.NetWorkManager;
import net.xiaoyu.mob_controller.network.SyncSelectedModePacket;
import net.xiaoyu.mob_controller.registry.ModSounds;
import net.xiaoyu.mob_controller.util.MobControlledData;

/**
 * 驭兽哨的共用实现：**轻点一下切模式，按住不放吹响**。
 *
 * <p>为什么这么做：触屏只有一个「右键」，原版又分不出「点一下」和「按住」，
 * 所以把一次点击拆成两段——按下先进入「使用物品」状态，然后按用户按了多久分流：</p>
 * <ul>
 *   <li>松手时还没按满 {@link #LONG_PRESS_TICKS}（= 点了一下）→ 切到下一个控制模式；</li>
 *   <li>一直按到 {@link #LONG_PRESS_TICKS} 还没松手 → 吹响，把当前模式下发给 32 格内所有受控生物。</li>
 * </ul>
 *
 * <p>阈值取 8 tick：TouchController 判「长按」的门槛是 5 tick（0.25 秒），
 * 留 3 tick 余量，短按不会误触发吹响。吹过之后再按住、松手都不会再切模式。</p>
 */
public abstract class WhistleItem extends Item {

    /** 按住达到这个 tick 数就算长按（吹响）；不到就是轻点（切模式）。 */
    public static final int LONG_PRESS_TICKS = 8;

    /** 按下后一直处于「使用中」的时长，给足时间让玩家按住不放。 */
    private static final int USE_DURATION = 72000;

    public WhistleItem(Properties properties) {
        super(properties);
    }

    /** 这件哨子当前选中的控制模式。 */
    public abstract MobControlledData.ControlMode getMode(ItemStack stack);

    /** 写入选中的控制模式。 */
    public abstract void setMode(ItemStack stack, MobControlledData.ControlMode mode);

    /** 切到下一个模式（跟随 → 停留 → 游荡 → 跟随）。 */
    public MobControlledData.ControlMode nextMode(ItemStack stack) {
        MobControlledData.ControlMode current = getMode(stack);
        MobControlledData.ControlMode[] values = MobControlledData.ControlMode.values();
        return values[(current.ordinal() + 1) % values.length];
    }

    /** 模式名（用于物品名与提示）。 */
    public static Component modeName(MobControlledData.ControlMode mode) {
        return Component.translatable("mob_controller.mode." + mode.toString().toLowerCase());
    }

    /**
     * 按下即进入「使用中」，之后的判定全交给 {@link #onUseTick} 与 {@link #releaseUsing}。
     * 不在这里直接做事，否则一次点击分不清是点还是长按。
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    /** 不播任何挥臂/进食动画，只看按住时长。 */
    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION;
    }

    /**
     * 按住计时。只在「刚好按满 {@link #LONG_PRESS_TICKS}」那一 tick 吹一次，
     * 继续按住不会重复吹（held 是单调递增的，只命中一次）。
     */
    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        if (!level.isClientSide || !(livingEntity instanceof Player player)) {
            return;
        }
        int heldTicks = getUseDuration(stack) - remainingUseDuration;
        if (heldTicks != LONG_PRESS_TICKS) {
            return;
        }
        MobControlledData.ControlMode mode = getMode(stack);
        NetWorkManager.INSTANCE.sendToServer(new ApplyControlCommandPacket(mode));
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
                ModSounds.CONTROL_COMMAND_USE.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /**
     * 兜底：按住放到 {@link #USE_DURATION} 也不会把哨子吃掉/消耗掉。
     * （哨子挂着空食物组件只是为了被 TouchController 认成「可长按使用」，不是真能吃。）
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        return stack;
    }

    /**
     * 松手。按满过阈值说明这次是「吹响」，什么都不做；
     * 没按满说明是「点了一下」，切到下一个模式。
     */
    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (!level.isClientSide || !(livingEntity instanceof Player player)) {
            return;
        }
        int heldTicks = getUseDuration(stack) - timeLeft;
        if (heldTicks >= LONG_PRESS_TICKS) {
            return;
        }

        MobControlledData.ControlMode next = nextMode(stack);
        setMode(stack, next);
        NetWorkManager.INSTANCE.sendToServer(new SyncSelectedModePacket(next));

        player.displayClientMessage(
                Component.translatable("mob_controller.message.mode_switched", modeName(next))
                        .withStyle(ChatFormatting.GOLD),
                true);
        level.playSound(player, player.getX(), player.getY(), player.getZ(),
                SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.2F);
    }
}
