package net.xiaoyu.mob_controller.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.xiaoyu.mob_controller.MobController;
import net.xiaoyu.mob_controller.item.BeastCompassItem;

/**
 * 寻兽罗盘的 HUD 指针：手持罗盘且选了目标时，屏幕上一直显示方向和距离。
 *
 * <p>方向按「相对你当前朝向」给八个方位，所以「↑」永远是你正前方，
 * 低头赶路时不用自己换算世界坐标。</p>
 */
@Mod.EventBusSubscriber(modid = MobController.MOD_ID, value = Dist.CLIENT)
public final class BeastCompassHud {

    private static final String[] ARROWS = {"↑", "↗", "→", "↘", "↓", "↙", "←", "↖"};
    private static final String[] NAMES = {"正前", "右前", "右侧", "右后", "正后", "左后", "左侧", "左前"};

    private BeastCompassHud() {
    }

    @SubscribeEvent
    public static void onRender(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        boolean holding = minecraft.player.getMainHandItem().getItem() instanceof BeastCompassItem
                || minecraft.player.getOffhandItem().getItem() instanceof BeastCompassItem;
        if (!holding) {
            return;
        }

        ClientBeastCompass.Target target = ClientBeastCompass.target();
        if (target == null) {
            return;
        }

        double dx = target.x() - minecraft.player.getX();
        double dz = target.z() - minecraft.player.getZ();
        int distance = (int) Math.sqrt(dx * dx + dz * dz);

        // 目标相对玩家朝向的角度：0 = 正前，90 = 右侧
        double worldAngle = Math.toDegrees(Math.atan2(-dx, dz));
        int index = Math.round(Mth.wrapDegrees((float) (worldAngle - minecraft.player.getYRot())) / 45.0F) & 7;

        Component line = Component.literal(ARROWS[index] + " " + target.name() + (target.loaded() ? "" : "（未加载）") + "  ")
                .withStyle(ChatFormatting.AQUA)
                .copy()
                .append(Component.literal(distance + "m " + NAMES[index]).withStyle(ChatFormatting.WHITE));

        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        graphics.drawCenteredString(minecraft.font, line, width / 2, 20, 0xFFFFFF);
    }
}
