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
        int reserved = 40 /*标题*/ + 66 /*底部两排按钮*/ + 10;
        int available = Math.max(rowHeight * 3, this.height - reserved);
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
                    .append(Component.literal("  " + entry.distance() + "m").withStyle(ChatFormatting.DARK_GREEN));
            addRenderableWidget(Button.builder(label, b -> select(entry.id()))
                    .bounds(x, y, width, rowHeight).build());
            y += rowHeight + 2;
        }

        int y2 = Math.max(y + 6, this.height - 60);
        int half = (width - 8) / 2;

        if (page > 0) {
            addRenderableWidget(Button.builder(Component.translatable("mob_controller.beast_compass.prev"),
                    b -> { page--; rebuild(); }).bounds(x, y2, half, 20).build());
        }
        int rightX = page > 0 ? x + half + 8 : x;
        int rightW = page > 0 ? half : width;
        if ((page + 1) * pageSize < entries.size()) {
            addRenderableWidget(Button.builder(Component.translatable("mob_controller.beast_compass.next"),
                    b -> { page++; rebuild(); }).bounds(rightX, y2, rightW, 20).build());
        }

        int y3 = y2 + 24;
        addRenderableWidget(Button.builder(Component.translatable("mob_controller.beast_compass.stop"),
                b -> select(-1)).bounds(x, y3, half, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x + half + 8, y3, half, 20).build());
    }

    private void select(int entityId) {
        NetWorkManager.INSTANCE.sendToServer(new BeastCompassSelectPacket(entityId));
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
