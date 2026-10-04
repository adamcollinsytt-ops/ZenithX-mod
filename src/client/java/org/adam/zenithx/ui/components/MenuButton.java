package org.adam.zenithx.ui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
//? } else {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;*/
//? }
//? if >=1.21.9 && <26 {
/*import net.minecraft.client.input.InputWithModifiers;*/
//? }
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
//? if >=26 {
import org.jspecify.annotations.Nullable;
//? }

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
    //? if >=26 {
    private @Nullable Supplier<Component> tooltipSupplier;
    //? } else {
    /*private Supplier<Component> tooltipSupplier;*/
    //? }
    private final Consumer<MenuButton> onPress;

    //? if >=26 {
    private @Nullable Identifier iconIdentifier = null;
    //? } else {
    /*private ResourceLocation iconIdentifier = null;*/
    //? }
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

    //? if >=26 {
    public MenuButton setIcon(Identifier iconIdentifier, int position) {
    //? } else {
    /*public MenuButton setIcon(ResourceLocation iconIdentifier, int position) {*/
    //? }
        return setIcon(iconIdentifier, position, 8);
    }

    //? if >=26 {
    public MenuButton setIcon(Identifier iconIdentifier, int position, int size) {
    //? } else {
    /*public MenuButton setIcon(ResourceLocation iconIdentifier, int position, int size) {*/
    //? }
        this.iconIdentifier = iconIdentifier;
        this.iconPosition = position;
        this.iconSize = size;
        return this;
    }

    //? if >=26 {
    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
    //? } else {
    /*@Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float a) {*/
    //? }
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
                    if (this.getMessage().getString().isEmpty()) {
                        iconX = this.getX() + (this.getWidth() - iconSize) / 2;
                    } else {
                        int totalContentWidth = textWidth + iconSpacing + iconSize;
                        int startX = this.getX() + (this.getWidth() - totalContentWidth) / 2;
                        iconX = startX;
                        textX = startX + iconSize + iconSpacing + (textWidth / 2);
                    }
                }
                case 1 -> {
                    int totalContentWidth = textWidth + iconSpacing + iconSize;
                    int startX = this.getX() + (this.getWidth() - totalContentWidth) / 2;
                    textX = startX + (textWidth / 2);
                    iconX = startX + textWidth + iconSpacing;
                }
                case 2 -> iconX = this.getX() + this.getWidth() + iconSpacing;
            }

            //? if >=1.21.9 {
            graphics.blit(
                    net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                    iconIdentifier,
                    iconX, iconY,
                    0, 0,
                    iconSize, iconSize,
                    iconSize, iconSize
            );
            //? } else {
            /*graphics.blit(
                    iconIdentifier,
                    iconX, iconY,
                    0, 0,
                    iconSize, iconSize,
                    iconSize, iconSize
            );*/
            //? }
        }

        //? if >=26 {
        graphics.centeredText(font, this.getMessage(), textX, textY, style.textColor);
        //? } else {
        /*graphics.drawCenteredString(font, this.getMessage(), textX, textY, style.textColor);*/
        //? }
    }

    //? if >=26 {
    private void drawStyledBackground(GuiGraphicsExtractor graphics, Style style) {
    //? } else {
    /*private void drawStyledBackground(GuiGraphics graphics, Style style) {*/
    //? }
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

    //? if >=1.21.9 {
    @Override
    public void onPress(InputWithModifiers input) {
        if (!this.active) return;
        onPress.accept(this);
    }
    //? } else {
    /*@Override
    public void onPress() {
        if (!this.active) return;
        onPress.accept(this);
    }*/
    //? }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
    }

    public record Style(int textColor, int buttonColor, int outlineColor, int highlightColor, int shadowColor) {
        public Style(int textColor, int buttonColor, int outlineColor) {
            this(textColor, buttonColor, outlineColor, brighten(buttonColor, 0.4f), darken(buttonColor, 0.5f, 0x80));
        }
        //? if >=26 {
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
        //? } else {
        /*private static int argbAlpha(int argb) { return (argb >> 24) & 0xFF; }
        private static int argbRed(int argb)   { return (argb >> 16) & 0xFF; }
        private static int argbGreen(int argb) { return (argb >> 8) & 0xFF; }
        private static int argbBlue(int argb)  { return argb & 0xFF; }
        private static int argbPack(int a, int r, int g, int b) {
            return (a << 24) | (r << 16) | (g << 8) | b;
        }
        private static int brighten(int argb, float factor) {
            int a = argbAlpha(argb);
            int r = Math.min(255, (int) (argbRed(argb) * (1f + factor)));
            int g = Math.min(255, (int) (argbGreen(argb) * (1f + factor)));
            int b = Math.min(255, (int) (argbBlue(argb) * (1f + factor)));
            return argbPack(a, r, g, b);
        }
        private static int darken(int argb, float factor, int alpha) {
            int r = Math.max(0, (int) (argbRed(argb) * (1f - factor)));
            int g = Math.max(0, (int) (argbGreen(argb) * (1f - factor)));
            int b = Math.max(0, (int) (argbBlue(argb) * (1f - factor)));
            return argbPack(alpha, r, g, b);
        }*/
        //? }
    }
}
