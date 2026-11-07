package net.xiaoyu.mob_controller.client;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.xiaoyu.mob_controller.client.gui.BeastCompassScreen;
import net.xiaoyu.mob_controller.network.BeastCompassListPacket;
import net.xiaoyu.mob_controller.network.BeastCompassTrackPacket;

import java.util.List;
import net.xiaoyu.mob_controller.network.BeastCompassEntry;

/**
 * 寻兽罗盘的客户端状态：当前追踪目标 + 打开列表界面。
 */
@OnlyIn(Dist.CLIENT)
public final class ClientBeastCompass {

    /** 当前追踪的目标（null 表示没选）。 */
    private static volatile BeastCompassTarget target;

    private ClientBeastCompass() {
    }

    /** 追踪目标快照。 */

    /** 收到服务端列表后弹界面。 */
    public static void openScreen(List<BeastCompassEntry> entries) {
        Minecraft.getInstance().setScreen(new BeastCompassScreen(entries));
    }

    /** 收到位置推送。 */
    public static void onTrack(BeastCompassTrackPacket packet) {
        if (!packet.valid()) {
            target = null;
            return;
        }
        target = new BeastCompassTarget(packet.mobId(), packet.name(), packet.x(), packet.y(), packet.z(), packet.loaded());
    }

    public static BeastCompassTarget target() {
        return target;
    }

    public static void clearTarget() {
        target = null;
    }
}
