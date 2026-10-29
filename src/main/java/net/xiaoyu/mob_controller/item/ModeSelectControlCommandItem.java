package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.util.MobControlledData;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 驭兽哨（精准版）。
 *
 * <p>操作和普通驭兽哨完全一致，只是物品名会带上当前模式、并且带附魔光效：</p>
 * <ul>
 *   <li><b>轻点一下</b> → 切到下一个控制模式</li>
 *   <li><b>按住不放</b> → 吹响，把当前模式下发给 32 格内所有受控生物</li>
 * </ul>
 */
public class ModeSelectControlCommandItem extends WhistleItem {

    private static final String TAG_SELECTED_MODE = "SelectedMode";

    public ModeSelectControlCommandItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public MobControlledData.ControlMode getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_SELECTED_MODE)) {
            try {
                return MobControlledData.ControlMode.valueOf(tag.getString(TAG_SELECTED_MODE));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return MobControlledData.ControlMode.FOLLOW;
    }

    @Override
    public void setMode(ItemStack stack, MobControlledData.ControlMode mode) {
        stack.getOrCreateTag().putString(TAG_SELECTED_MODE, mode.name());
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.mob_controller.mode_select_control_command", modeName(getMode(stack)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("mob_controller.tooltip.whistle.click").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mob_controller.tooltip.whistle.hold").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mob_controller.tooltip.whistle.range").withStyle(ChatFormatting.AQUA));
    }
}
