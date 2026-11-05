package net.xiaoyu.mob_controller.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.xiaoyu.mob_controller.network.BeastCompassRequestPacket;
import net.xiaoyu.mob_controller.network.NetWorkManager;

/**
 * 寻兽罗盘：右键让服务端把「当前维度里你驯服的所有生物」列出来，
 * 选一只之后 HUD 会一直指向它。
 *
 * <p>列表由服务端收集（客户端拿不到远处实体的位置），选择结果存在服务端，
 * 位置每 20 tick 推一次给客户端画指针。</p>
 */
public class BeastCompassItem extends Item {

    public BeastCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide) {
            NetWorkManager.INSTANCE.sendToServer(new BeastCompassRequestPacket());
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }
}
