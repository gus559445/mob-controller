// 文件：net/xiaoyu/mob_controller/item/ControlCommandItem.java
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
 * 驭兽哨。
 *
 * <p>操作只有两个，吹和切都靠同一件物品：</p>
 * <ul>
 *   <li><b>轻点一下</b>（对着方块点也行）→ 切到下一个控制模式：跟随 → 停留 → 游荡 → 跟随</li>
 *   <li><b>按住不放</b> → 吹响，把当前模式下发给 32 格内所有受控生物</li>
 * </ul>
 *
 * <p>当前选中的模式直接显示在物品名上，不用记。</p>
 */
public class ControlCommandItem extends WhistleItem {

    /** 物品 NBT 里记住「上一次用的是哪个模式」，点一次往后循环一格。 */
    private static final String TAG_MODE = "SelectedControlMode";

    public ControlCommandItem(Properties properties) {
        super(properties);
    }

    @Override
    public MobControlledData.ControlMode getMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_MODE)) {
            MobControlledData.ControlMode[] values = MobControlledData.ControlMode.values();
            int ordinal = tag.getInt(TAG_MODE);
            if (ordinal >= 0 && ordinal < values.length) {
                return values[ordinal];
            }
        }
        return MobControlledData.ControlMode.FOLLOW;
    }

    @Override
    public void setMode(ItemStack stack, MobControlledData.ControlMode mode) {
        stack.getOrCreateTag().putInt(TAG_MODE, mode.ordinal());
    }

    /** 物品名直接带当前模式，省得玩家猜「现在点一下会切到哪」。 */
    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.mob_controller.control_command.mode", modeName(getMode(stack)));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("mob_controller.tooltip.whistle.click").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mob_controller.tooltip.whistle.hold").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mob_controller.tooltip.whistle.range").withStyle(ChatFormatting.AQUA));
    }
}
