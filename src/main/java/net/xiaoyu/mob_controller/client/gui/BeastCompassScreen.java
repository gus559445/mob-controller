package net.xiaoyu.mob_controller.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.xiaoyu.mob_controller.network.BeastCompassListPacket;
import net.xiaoyu.mob_controller.network.BeastCompassSelectPacket;
import net.xiaoyu.mob_controller.network.NetWorkManager;

import java.util.List;

/**
 * 寻兽罗盘的列表界面：列出当前维度里你驯服的生物，点一个就开始指向它。
 *
 * <p>一页 7 条，多了用上下页翻；点「取消追踪」会清掉当前目标。</p>
 */
@OnlyIn(Dist.CLIENT)
public class BeastCompassScreen extends Screen {

    private static final int ROW_H = 20;
    private static final int ROW_GAP = 24;

    private final List<BeastCompassListPacket.Entry> entries;
    private int page;

    // 下面这几个都按当前屏幕尺寸算出来，见 ensureLayout()
    private int pageSize = 7;
    private int rowHeight = 22;
    private int listWidth = 300;

    public BeastCompassScreen(List<BeastCompassListPacket.Entry> entries) {
        super(Component.translatable("mob_controller.beast_compass.title"));
        this.entries = entries;
    }

    @Override
    protected void init() {
        ensureLayout();
        rebuild();
    }

    /**
     * 按当前屏幕尺寸重新算一遍布局：界面整体占屏宽 85%（限制在 180~420），
     * 行高跟着屏幕高度走，一页显示多少条由剩余高度决定 —— 手机竖屏少显示几行、
     * 按钮矮一点，平板/大屏多显示几行，不会出现挤成一团或者飘在中间一大片空白。
     */
    private void ensureLayout() {
        listWidth = Mth.clamp((int) (this.width * 0.85F), 180, 420);
        rowHeight = Mth.clamp((int) (this.height * 0.045F), 14, 28);
        // 底部现在有三排按钮（翻页 / 取消-完成 / 深度搜索），从屏幕底边往上数
        int pageY = this.height - 6 - ROW_GAP * 2 - ROW_H;
        int available = Math.max(rowHeight * 3, pageY - 34 - 6);
        pageSize = Math.max(3, available / (rowHeight + 2));
        if (page * pageSize >= entries.size() && page > 0) {
            page = Math.max(0, (entries.size() - 1) / pageSize);
        }
    }

    private void rebuild() {
        clearWidgets();

        int width = listWidth;
        int x = this.width / 2 - width / 2;
        int y = 34;

        int from = page * pageSize;
        int to = Math.min(entries.size(), from + pageSize);
        for (int i = from; i < to; i++) {
            BeastCompassListPacket.Entry entry = entries.get(i);
            Component label = Component.literal(entry.name())
                    .append(Component.literal("  " + entry.typeId()).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("  " + entry.distance() + "m").withStyle(ChatFormatting.DARK_GREEN))
                    .append(entry.loaded()
                            ? Component.empty()
                            : Component.literal("  未加载").withStyle(ChatFormatting.DARK_RED));
            addRenderableWidget(Button.builder(label, b -> select(entry.mobId(), entry.name()))
                    .bounds(x, y, width, rowHeight).build());
            y += rowHeight + 2;
        }

        int half = (width - 8) / 2;

        // 从屏幕底边往上排，保证最下面那排（深度搜索）不会跑出屏幕
        int deepY = this.height - 6 - ROW_H;
        int y3 = deepY - ROW_GAP;
        int y2 = y3 - ROW_GAP;

        if (page > 0) {
            addRenderableWidget(Button.builder(Component.translatable("mob_controller.beast_compass.prev"),
                    b -> { page--; rebuild(); }).bounds(x, y2, half, ROW_H).build());
        }
        int rightX = page > 0 ? x + half + 8 : x;
        int rightW = page > 0 ? half : width;
        if ((page + 1) * pageSize < entries.size()) {
            addRenderableWidget(Button.builder(Component.translatable("mob_controller.beast_compass.next"),
                    b -> { page++; rebuild(); }).bounds(rightX, y2, rightW, ROW_H).build());
        }

        addRenderableWidget(Button.builder(Component.translatable("mob_controller.beast_compass.stop"),
                b -> select(net.xiaoyu.mob_controller.network.BeastCompassTracker.NONE, "")).bounds(x, y3, half, ROW_H).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x + half + 8, y3, half, ROW_H).build());

        // 深度搜索：让服务端把远处/未加载区块里的受控生物也拉出来
        addRenderableWidget(Button.builder(Component.translatable("mob_controller.beast_compass.deep"),
                b -> deepSearch()).bounds(x, deepY, width, ROW_H).build());
    }

    /** 深度搜索：服务端会强制加载远处区块，可能要等一下才有结果。 */
    private void deepSearch() {
        NetWorkManager.INSTANCE.sendToServer(new net.xiaoyu.mob_controller.network.BeastCompassRequestPacket(true));
        onClose();
    }

    private void select(java.util.UUID mobId, String name) {
        NetWorkManager.INSTANCE.sendToServer(new BeastCompassSelectPacket(mobId, name));
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        if (entries.isEmpty()) {
            graphics.drawCenteredString(this.font,
                    Component.translatable("mob_controller.beast_compass.empty"),
                    this.width / 2, this.height / 2, 0xFF6666);
        } else {
            graphics.drawCenteredString(this.font,
                    Component.translatable("mob_controller.beast_compass.count", entries.size()),
                    this.width / 2, 22, 0xA0A0A0);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
