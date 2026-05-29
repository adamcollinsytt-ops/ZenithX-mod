package org.adam.zenithx.ui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class MenuButton extends AbstractButton {

    public static final Style BLUE       = new Style(0xFFFFFFFF, 0xFF274673, 0xFF000400);
    public static final Style LIGHT_BLUE = new Style(0xFFFFFFFF, 0xFF3073D4, 0xFFFFFFFF);
    public static final Style GREEN      = new Style(0xFFFFFFFF, 0xFF226132, 0xFF000400);
    public static final Style LIGHT_GREEN= new Style(0xFFFFFFFF, 0xFF2D7941, 0xFFFFFFFF);
    public static final Style RED        = new Style(0xFFFFFFFF, 0xFF9F4444, 0xFF000400);
    public static final Style LIGHT_RED  = new Style(0xFFFFFFFF, 0xFFC02525, 0xFFFFFFFF);
    public static final Style DARK_GRAY  = new Style(0xFFFFFFFF, 0xFF383838, 0xFF000400);
    public static final Style GRAY       = new Style(0xFFFFFFFF, 0xFF595959, 0xFFFFFFFF);
    public static final Style DISABLED   = new Style(0xFFA0A0A0, 0xFF252525, 0xFF000000);

    private Style defaultStyle;
    private Style hoverStyle;
    private Style disabledStyle;

    private final boolean clickSound;
    private boolean drawBackground = true;
    private @Nullable Supplier<Component> tooltipSupplier;
    private final Consumer<MenuButton> onPress;

    private @Nullable Identifier iconIdentifier = null;
    private int iconPosition = 0;
    private int iconSize = 8;
    private final int iconSpacing = 4;

    public MenuButton(int x, int y, int width, int height, Component message, Style defaultStyle, Style hoverStyle, Style disabledStyle, boolean clickSound, Consumer<MenuButton> onPress) {
        super(x, y, width, height, message);
        this.defaultStyle   = defaultStyle;
        this.hoverStyle     = hoverStyle;
        this.disabledStyle  = disabledStyle;
        this.clickSound     = clickSound;
        this.onPress        = onPress;
    }

    public MenuButton(int x, int y, int width, int height, Component message, Consumer<MenuButton> onPress) {
        this(x, y, width, height, message, DARK_GRAY, GRAY, DISABLED, true, onPress);
    }

    public MenuButton setDefaultStyle(Style style) { this.defaultStyle = style; return this; }
    public MenuButton setHoverStyle(Style style) { this.hoverStyle = style; return this; }

    private Style currentStyle() {
        if (!this.active) return disabledStyle;
        return (this.isHoveredOrFocused()) ? hoverStyle : defaultStyle;
    }

    public MenuButton setIcon(Identifier iconIdentifier, int position) {
        return setIcon(iconIdentifier, position, 8);
    }

    public MenuButton setIcon(Identifier iconIdentifier, int position, int size) {
        this.iconIdentifier = iconIdentifier;
        this.iconPosition = position;
        this.iconSize = size;
        return this;
    }

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Style style = currentStyle();

        if (drawBackground) {
            drawStyledBackground(graphics, style);
        }

        var font = Minecraft.getInstance().font;
        int textWidth = font.width(this.getMessage());
        int textX = this.getX() + this.getWidth() / 2;
        int textY = this.getY() + (this.getHeight() - 8) / 2;

        if (iconIdentifier != null) {
            int iconX = 0;
            int iconY = this.getY() + (this.getHeight() - iconSize) / 2;

            switch (iconPosition) {
                case -1 -> iconX = this.getX() - iconSize - iconSpacing;
                case 0 -> {
                    int totalContentWidth = textWidth + iconSpacing + iconSize;
                    int startX = this.getX() + (this.getWidth() - totalContentWidth) / 2;
                    iconX = startX;
                    textX = startX + iconSize + iconSpacing + (textWidth / 2);
                }
                case 1 -> {
                    int totalContentWidth = textWidth + iconSpacing + iconSize;
                    int startX = this.getX() + (this.getWidth() - totalContentWidth) / 2;
                    textX = startX + (textWidth / 2);
                    iconX = startX + textWidth + iconSpacing;
                }
                case 2 -> iconX = this.getX() + this.getWidth() + iconSpacing;
            }

            graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, iconIdentifier, iconX, iconY, iconSize, iconSize);
        }

        graphics.centeredText(font, this.getMessage(), textX, textY, style.textColor);
    }

    private void drawStyledBackground(GuiGraphicsExtractor graphics, Style style) {
        int x = getX(), y = getY(), r = x + getWidth(), b = y + getHeight();
        graphics.fill(x + 1, y + 1, r - 1, b - 1, style.buttonColor);
        graphics.fill(x, y, x + 1, b, style.highlightColor);
        graphics.fill(x + 1, y, r, y + 1, style.highlightColor);
        graphics.fill(r - 1, y, r, b, style.shadowColor);
        graphics.fill(x, b - 1, r - 1, b, style.shadowColor);
        int ol = style.outlineColor;
        graphics.fill(x - 1, y - 1, r + 1, y, ol);
        graphics.fill(x - 1, b, r + 1, b + 1, ol);
        graphics.fill(x - 1, y, x, b, ol);
        graphics.fill(r, y, r + 1, b, ol);
    }

    @Override
    public void onPress(InputWithModifiers input) {
        if (!this.active) return;
        onPress.accept(this);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    public record Style(int textColor, int buttonColor, int outlineColor, int highlightColor, int shadowColor) {
        public Style(int textColor, int buttonColor, int outlineColor) {
            this(textColor, buttonColor, outlineColor, brighten(buttonColor, 0.4f), darken(buttonColor, 0.5f, 0x80));
        }
        private static int brighten(int argb, float factor) {
            int a = ARGB.alpha(argb);
            int r = Math.min(255, (int) (ARGB.red(argb) * (1f + factor)));
            int g = Math.min(255, (int) (ARGB.green(argb) * (1f + factor)));
            int b = Math.min(255, (int) (ARGB.blue(argb) * (1f + factor)));
            return ARGB.color(a, r, g, b);
        }
        private static int darken(int argb, float factor, int alpha) {
            int r = Math.max(0, (int) (ARGB.red(argb) * (1f - factor)));
            int g = Math.max(0, (int) (ARGB.green(argb) * (1f - factor)));
            int b = Math.max(0, (int) (ARGB.blue(argb) * (1f - factor)));
            return ARGB.color(alpha, r, g, b);
        }
    }
}