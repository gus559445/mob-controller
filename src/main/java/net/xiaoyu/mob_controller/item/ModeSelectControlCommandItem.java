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
import net.xiaoyu.mob_controller.network.SyncSelectedModePacket;
import net.xiaoyu.mob_controller.util.MobControlledData;

import javax.annotation.Nullable;
import java.util.List;

public class ModeSelectControlCommandItem extends Item {

    private static final String TAG_SELECTED_MODE = "SelectedMode";

    /** 长按重复触发的冷却（tick）。触屏按住不放时原版每 4 tick 会再调一次 use()。 */
    private static final int LONG_PRESS_COOLDOWN_TICKS = 10;

    public ModeSelectControlCommandItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        MobControlledData.ControlMode mode = getSelectedMode(stack);
        String modeKey = "mob_controller.mode." + mode.toString().toLowerCase();
        return Component.translatable("item.mob_controller.mode_select_control_command",
                Component.translatable(modeKey));
    }

    /**
     * 右键使用。
     *
     * <p>触屏 / 手柄没有鼠标左右键之分，原来靠 {@code InputEvent.MouseButton} 的左键循环、
     * 右键批量在这里重新实现一遍，两种设备都能用：</p>
     * <ul>
     *   <li>不潜行 + 右键：循环切换选中的控制模式（原「左键」功能）</li>
     *   <li>潜行 + 右键：把当前选中的模式批量应用到 32 格内的受控生物（原「右键」功能）</li>
     * </ul>
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            // 长按时把重复触发压到半秒一次，否则按住不放会疯狂切模式。
            if (player.getCooldowns().isOnCooldown(this)) {
                return InteractionResultHolder.consume(stack);
            }
            player.getCooldowns().addCooldown(this, LONG_PRESS_COOLDOWN_TICKS);
            player.swing(hand);
            if (player.isShiftKeyDown()) {
                // 潜行：批量应用当前模式
                NetWorkManager.INSTANCE.sendToServer(new ApplyControlCommandPacket(getSelectedMode(stack)));
            } else {
                // 不潜行：循环切换模式
                cycleSelectedMode(stack);
                MobControlledData.ControlMode newMode = getSelectedMode(stack);
                NetWorkManager.INSTANCE.sendToServer(new SyncSelectedModePacket(newMode));
                String modeKey = "mob_controller.mode." + newMode.toString().toLowerCase();
                player.displayClientMessage(Component.translatable("mob_controller.message.mode_switched",
                        Component.translatable(modeKey)).withStyle(ChatFormatting.GOLD), true);
            }
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

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("mob_controller.tooltip.mode_select_control_command").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("mob_controller.tooltip.mode_select_control_command.desc").withStyle(ChatFormatting.GRAY));
    }

    public static void cycleSelectedMode(ItemStack stack) {
        MobControlledData.ControlMode current = getSelectedMode(stack);
        int nextOrd = (current.ordinal() + 1) % MobControlledData.ControlMode.values().length;
        MobControlledData.ControlMode next = MobControlledData.ControlMode.values()[nextOrd];
        setSelectedMode(stack, next);
    }

    public static MobControlledData.ControlMode getSelectedMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_SELECTED_MODE)) {
            try {
                return MobControlledData.ControlMode.valueOf(tag.getString(TAG_SELECTED_MODE));
            } catch (IllegalArgumentException ignored) {}
        }
        return MobControlledData.ControlMode.FOLLOW;
    }

    public static void setSelectedMode(ItemStack stack, MobControlledData.ControlMode mode) {
        stack.getOrCreateTag().putString(TAG_SELECTED_MODE, mode.name());
    }
}
