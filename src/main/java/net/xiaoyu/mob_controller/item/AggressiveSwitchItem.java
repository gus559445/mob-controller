package net.xiaoyu.mob_controller.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.network.NetWorkManager;
import net.xiaoyu.mob_controller.network.SwitchAggressiveModePacket;
import net.xiaoyu.mob_controller.util.MobControlledData;

import javax.annotation.Nullable;
import java.util.List;

/**
 * 护主切换器物品。
 * <p>用于切换受控生物的索敌模式（索敌/护主）。</p>
 * <p>使用方式：</p>
 * <ul>
 *   <li>右键生物：切换该生物的模式（触屏没有左右键区分，改成「点一次切一次」）</li>
 *   <li>潜行+右键空气/方块：批量把 32 格内受控生物切到索敌模式</li>
 *   <li>不潜行+右键空气/方块：批量切到护主模式</li>
 * </ul>
 * <p>这些入口同时兼容鼠标玩家和触屏玩家：手机上的 TouchController 只会发「右键」，
 * 原来只挂在 {@code InputEvent.MouseButton} 上的逻辑在手机上永远不会触发。</p>
 */
public class AggressiveSwitchItem extends Item {

    /** 长按重复触发的冷却（tick）。原版按住右键每 4 tick 就会再调一次 use()。 */
    private static final int LONG_PRESS_COOLDOWN_TICKS = 10;

    public AggressiveSwitchItem(Properties properties) {
        super(properties);
    }

    /**
     * 右键生物时切换单体模式。
     */
    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Mob mob)) {
            return InteractionResult.PASS;
        }

        // 潜行时不处理单体（交给 use() 的批量逻辑）
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }

        // 仅在服务端执行逻辑
        if (player.level().isClientSide) {
            player.swing(hand);
            return InteractionResult.SUCCESS;
        }

        if (!MobControlledData.isControlledEntity(mob)) {
            player.displayClientMessage(Component.translatable("mob_controller.message.not_controlled").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        if (!player.getUUID().equals(MobControlledData.getControllerUUID(mob))) {
            player.displayClientMessage(Component.translatable("mob_controller.message.not_owner").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        // 触屏只有一个「右键」：改成切换，护主 / 索敌都能靠连续点选到。
        MobControlledData.setSingleAggressiveMode(player, mob, !MobControlledData.isAggressiveMode(mob));

        return InteractionResult.SUCCESS;
    }

    /**
     * 右键空气 / 方块：批量切换。
     *
     * <p>潜行 = 索敌，不潜行 = 护主（对应原来的「潜行+左键 / 潜行+右键」）。</p>
     */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            // 触屏长按时原版每 4 tick 就会再调一次 use()，加个冷却把重复触发压到半秒一次，
            // 免得按住不放把模式刷成来回跳。
            if (player.getCooldowns().isOnCooldown(this)) {
                return InteractionResultHolder.consume(stack);
            }
            player.getCooldowns().addCooldown(this, LONG_PRESS_COOLDOWN_TICKS);
            player.swing(hand);
            // -1 = 批量切换（服务端会遍历 32 格内的受控生物）
            NetWorkManager.INSTANCE.sendToServer(new SwitchAggressiveModePacket(player.isShiftKeyDown(), -1));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    /**
     * 食物组件只是为了骗过 TouchController 的「能不能长按使用」判定，
     * 号角本身不是吃的：咬到头也原样还回去，不消耗、不加饥饿。
     */
    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("mob_controller.tooltip.aggressive_switch").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("mob_controller.tooltip.aggressive_switch.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mob_controller.tooltip.aggressive_switch.desc2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("mob_controller.tooltip.aggressive_switch.desc3").withStyle(ChatFormatting.GRAY));
    }
}
