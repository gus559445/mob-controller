// 文件：net/xiaoyu/mob_controller/item/ControlCommandItem.java
package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.network.ApplyControlCommandPacket;
import net.xiaoyu.mob_controller.network.NetWorkManager;
import net.xiaoyu.mob_controller.util.MobControlledData;

import javax.annotation.Nullable;
import java.util.List;

public class ControlCommandItem extends Item {

    /** 物品 NBT 里记住「上一次用的是哪个模式」，右键一次往后循环一格。 */
    private static final String TAG_MODE = "SelectedControlMode";

    /** 长按重复触发的冷却（tick）。触屏按住不放时原版每 4 tick 会再调一次 use()。 */
    private static final int LONG_PRESS_COOLDOWN_TICKS = 10;

    public ControlCommandItem(Properties properties) {
        super(properties);
    }

    /**
     * 右键使用：在「跟随 / 停留 / 游荡」之间循环，并把选中的模式批量应用出去。
     *
     * <p>原版是靠鼠标左 / 中 / 右键各对应一个模式（{@code InputEvent.MouseButton} 里处理），
     * 触屏根本没有这些事件，所以这里改成右键循环。鼠标玩家也能照常用——
     * 只是从「三个键各管一个」变成「一个键循环」。</p>
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            // 长按时把重复触发压到半秒一次，否则按住不放会疯狂循环模式。
            if (player.getCooldowns().isOnCooldown(this)) {
                return InteractionResultHolder.consume(stack);
            }
            player.getCooldowns().addCooldown(this, LONG_PRESS_COOLDOWN_TICKS);
            player.swing(hand);
            MobControlledData.ControlMode mode = cycleMode(stack);
            NetWorkManager.INSTANCE.sendToServer(new ApplyControlCommandPacket(mode));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * 食物组件只用于让 TouchController 把手上的哨子认成「可长按使用」，
     * 哨子本身不是吃的：咬到头原样还回去，不消耗、不加饥饿。
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        return stack;
    }

    /** 往后循环一格控制模式。 */
    private static MobControlledData.ControlMode cycleMode(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        int next = tag.contains(TAG_MODE) ? (tag.getInt(TAG_MODE) + 1) % MobControlledData.ControlMode.values().length : 0;
        tag.putInt(TAG_MODE, next);
        return MobControlledData.ControlMode.values()[next];
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("mob_controller.tooltip.control_command.range").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("mob_controller.tooltip.control_command.left").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mob_controller.tooltip.control_command.middle").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mob_controller.tooltip.control_command.right").withStyle(ChatFormatting.GRAY));
    }
}
