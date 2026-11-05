package net.xiaoyu.mob_controller.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.registry.ModItems;

import javax.annotation.Nullable;

/**
 * 寻兽罗盘的「指针会转」是怎么实现的。
 *
 * <p>两套贴图：{@code beast_compass.png} 是静态的单帧（指针朝上），
 * {@code beast_compass_animated.png} 是 32 帧竖排 + {@code .mcmeta} 的循环动画。
 * 模型用谓词 {@code mob_controller:in_use} 在两者之间切换：
 * <b>拿在手上播动画，躺在背包里显示静态</b>。</p>
 */
@Mod.EventBusSubscriber(modid = MobController.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientItemProperties {

    /** 谓词：这件罗盘此刻是不是拿在手上（= 要播动态贴图）。 */
    private static final ResourceLocation IN_USE = new ResourceLocation(MobController.MOD_ID, "in_use");

    private ClientItemProperties() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.BEAST_COMPASS_ITEM.get(),
                IN_USE,
                ClientItemProperties::inUse));
    }

    /**
     * 拿在手上返回 1.0 → 模型切到带动画的贴图（32 帧旋转指针）；
     * 躺在背包/箱子里返回 0.0 → 用静态那一帧。
     */
    private static float inUse(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        if (!(entity instanceof Player player)) {
            return 0.0F;
        }
        boolean holding = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        return holding ? 1.0F : 0.0F;
    }
}
