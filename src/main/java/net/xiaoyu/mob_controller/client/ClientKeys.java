package net.xiaoyu.mob_controller.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.client.gui.AiChatConfigScreen;
import net.xiaoyu.mob_controller.network.AiChatTargetPacket;
import net.xiaoyu.mob_controller.network.NetWorkManager;
import org.lwjgl.glfw.GLFW;

/**
 * AI 聊天热键。
 *
 * <p>准星对着自己控制的生物按一下：进入对话（之后在聊天栏打字就是跟它说话）；
 * 对着同一只再按一下（或准星空着按）：退出对话。目标校验在服务端做，客户端只负责把
 * 「我指着谁」报上去。</p>
 */
@Mod.EventBusSubscriber(modid = MobController.MOD_ID, value = Dist.CLIENT)
public final class ClientKeys {

    /** 默认键位 K，可以在「控制 - 生物控制器」里改。 */
    public static final KeyMapping AI_CHAT = new KeyMapping(
            "key.mob_controller.ai_chat",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            "key.categories.mob_controller");

    /** 默认键位 J：打开 AI 聊天设置界面，省得手改 toml。 */
    public static final KeyMapping AI_CONFIG = new KeyMapping(
            "key.mob_controller.ai_config",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_J,
            "key.categories.mob_controller");

    private ClientKeys() {
    }

    @Mod.EventBusSubscriber(modid = MobController.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class ModBus {
        private ModBus() {
        }

        @SubscribeEvent
        public static void registerKeys(RegisterKeyMappingsEvent event) {
            event.register(AI_CHAT);
            event.register(AI_CONFIG);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null) {
            return;
        }
        while (AI_CONFIG.consumeClick()) {
            minecraft.setScreen(new AiChatConfigScreen(null));
        }
        while (AI_CHAT.consumeClick()) {
            Entity aimed = minecraft.crosshairPickEntity;
            // 准星上是生物就选它，空着就当作「关掉对话」
            int entityId = aimed instanceof Mob mob ? mob.getId() : -1;
            NetWorkManager.INSTANCE.sendToServer(new AiChatTargetPacket(entityId));
        }
    }
}
