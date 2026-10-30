package net.xiaoyu.mob_controller.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.xiaoyu.mob_controller.config.AiChatConfig;

/**
 * AI 聊天设置界面，省得手改 toml。
 *
 * <p>七个输入框对应 {@link AiChatConfig} 里的七项；点「保存」会把值写进配置对象、
 * 立刻对运行中的服务端生效，同时落盘回
 * {@code config/mob_controller/mob_controller-ai-chat.toml}。</p>
 *
 * <p>数字框留空或填错就用默认值，不会因为手滑把配置写坏。</p>
 */
@OnlyIn(Dist.CLIENT)
public class AiChatConfigScreen extends Screen {

    private static final int ROW = 26;

    private final Screen parent;

    private EditBox urlBox;
    private EditBox keyBox;
    private EditBox modelBox;
    private EditBox tokensBox;
    private EditBox temperatureBox;
    private EditBox rangeBox;
    private EditBox prefixBox;

    private Component status = Component.empty();
    private ChatFormatting statusColor = ChatFormatting.GRAY;

    public AiChatConfigScreen(Screen parent) {
        super(Component.translatable("mob_controller.ai_config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int width = Math.min(300, this.width - 30);
        int x = this.width / 2 - width / 2;
        int y = 30;

        urlBox = addField(x, y, width, AiChatConfig.API_URL.get(), "https://api.deepseek.com/v1/chat/completions");
        y += ROW;
        keyBox = addField(x, y, width, AiChatConfig.API_KEY.get(), "sk-...");
        y += ROW;
        modelBox = addField(x, y, width, AiChatConfig.MODEL.get(), "deepseek-chat");
        y += ROW;
        tokensBox = addField(x, y, width, String.valueOf(AiChatConfig.MAX_TOKENS.get()), "500");
        y += ROW;
        temperatureBox = addField(x, y, width, String.valueOf(AiChatConfig.TEMPERATURE.get()), "0.8");
        y += ROW;
        rangeBox = addField(x, y, width, String.valueOf(AiChatConfig.RANGE.get()), "32.0");
        y += ROW;
        prefixBox = addField(x, y, width, AiChatConfig.REPLY_PREFIX.get(), "§b[%s] §f");
        y += ROW + 6;

        int buttonWidth = (width - 8) / 2;
        addRenderableWidget(Button.builder(Component.translatable("mob_controller.ai_config.save"), b -> save())
                .bounds(x, y, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .bounds(x + buttonWidth + 8, y, buttonWidth, 20).build());
    }

    private EditBox addField(int x, int y, int width, String value, String hint) {
        EditBox box = new EditBox(this.font, x, y, width, 18, Component.empty());
        box.setMaxLength(512);
        box.setValue(value == null ? "" : value);
        box.setHint(Component.literal(hint).withStyle(ChatFormatting.DARK_GRAY));
        addRenderableWidget(box);
        return box;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        int width = Math.min(300, this.width - 30);
        int x = this.width / 2 - width / 2;
        int y = 30;
        String[] labels = {
                "mob_controller.ai_config.url",
                "mob_controller.ai_config.key",
                "mob_controller.ai_config.model",
                "mob_controller.ai_config.tokens",
                "mob_controller.ai_config.temperature",
                "mob_controller.ai_config.range",
                "mob_controller.ai_config.prefix"
        };
        for (String label : labels) {
            graphics.drawString(this.font, Component.translatable(label), x, y - 10, 0xA0A0A0, false);
            y += ROW;
        }

        if (!status.getString().isEmpty()) {
            graphics.drawCenteredString(this.font, status, this.width / 2, this.height - 16, 0xFFFFFF);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** 写进配置对象并落盘；数值填错一律回退到默认值。 */
    private void save() {
        AiChatConfig.API_URL.set(urlBox.getValue().trim());
        AiChatConfig.API_KEY.set(keyBox.getValue().trim());
        AiChatConfig.MODEL.set(modelBox.getValue().trim());
        AiChatConfig.MAX_TOKENS.set(parseInt(tokensBox.getValue(), AiChatConfig.MAX_TOKENS.getDefault(), 16, 4096));
        AiChatConfig.TEMPERATURE.set(parseDouble(temperatureBox.getValue(), AiChatConfig.TEMPERATURE.getDefault(), 0.0D, 2.0D));
        AiChatConfig.RANGE.set(parseDouble(rangeBox.getValue(), AiChatConfig.RANGE.getDefault(), 4.0D, 128.0D));
        AiChatConfig.REPLY_PREFIX.set(prefixBox.getValue());

        try {
            AiChatConfig.SPEC.save();
            status = Component.translatable("mob_controller.ai_config.saved");
            statusColor = ChatFormatting.GREEN;
        } catch (Exception exception) {
            status = Component.translatable("mob_controller.ai_config.save_failed", exception.getMessage());
            statusColor = ChatFormatting.RED;
        }
        status = status.copy().withStyle(statusColor);

        // 数字框回填规范化后的值，让玩家看到实际生效的是什么
        tokensBox.setValue(String.valueOf(AiChatConfig.MAX_TOKENS.get()));
        temperatureBox.setValue(String.valueOf(AiChatConfig.TEMPERATURE.get()));
        rangeBox.setValue(String.valueOf(AiChatConfig.RANGE.get()));
    }

    private int parseInt(String text, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(text.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private double parseDouble(String text, double fallback, double min, double max) {
        try {
            double value = Double.parseDouble(text.trim());
            return Math.max(min, Math.min(max, value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
