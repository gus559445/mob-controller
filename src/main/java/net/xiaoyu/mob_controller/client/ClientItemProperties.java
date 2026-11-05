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
 * <p>物品模型有 32 帧（{@code beast_compass_00} ~ {@code _31}），用一个名为
 * {@code angle} 的模型谓词挑帧。这里注册的就是这个谓词：拿玩家到追踪目标的方位角，
 * 归一化到 0~1 交给模型系统 —— 和原版指南针指向出生点/磁石是同一套机制。</p>
 *
 * <p>没选目标时让指针慢慢自转，一眼能看出「还没锁定」。</p>
 */
@Mod.EventBusSubscriber(modid = MobController.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ClientItemProperties {

    private static final ResourceLocation ANGLE = new ResourceLocation(MobController.MOD_ID, "angle");

    private ClientItemProperties() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ItemProperties.register(
                ModItems.BEAST_COMPASS_ITEM.get(),
                ANGLE,
                ClientItemProperties::angle));
    }

    /**
     * @return 0~1 的角度，对应 32 帧里的某一帧
     */
    private static float angle(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        if (!(entity instanceof Player player)) {
            return 0.0F;
        }
        ClientBeastCompass.Target target = ClientBeastCompass.target();
        if (target == null) {
            // 没选目标：缓慢自转，表示「未锁定」
            return (System.currentTimeMillis() % 4000L) / 4000.0F;
        }
        double dx = target.x() - player.getX();
        double dz = target.z() - player.getZ();
        // 与贴图帧的约定：angle = atan2(dz, dx) / 2π + 0.5
        double value = Math.atan2(dz, dx) / (Math.PI * 2.0D) + 0.5D;
        return (float) (value - Math.floor(value));
    }
}
