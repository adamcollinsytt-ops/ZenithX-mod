package org.adam.zenithx.ui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A styled button for use in various menus — ported to Minecraft 26.1.2 (Fabric).
 *
 * Rendering pipeline:
 *   - extractContents()  →  called each frame to build the render state
 *   - All drawing goes through GuiGraphicsExtractor (replaces old GuiGraphics / UGraphics)
 *
 * Color format: 0xAARRGGBB  (ARGB int, same as vanilla widgets)
 */
public class MenuButton extends AbstractButton {

    // -----------------------------------------------------------------------
    // Pre-made color styles  (ARGB ints)
    // -----------------------------------------------------------------------
    public static final Style DARK_GRAY  = new Style(0xFFFFFFFF, 0xFF383838, 0xFF000400);
    public static final Style GRAY       = new Style(0xFFFFFFFF, 0xFF595959, 0xFFFFFFFF);
    public static final Style GREEN      = new Style(0xFFFFFFFF, 0xFF226132, 0xFF000400);
    public static final Style LIGHT_GREEN= new Style(0xFFFFFFFF, 0xFF2D7941, 0xFFFFFFFF);
    public static final Style BLUE       = new Style(0xFFFFFFFF, 0xFF274673, 0xFF000400);
    public static final Style LIGHT_BLUE = new Style(0xFFFFFFFF, 0xFF3073D4, 0xFFFFFFFF);
    public static final Style RED        = new Style(0xFFFFFFFF, 0xFF9F4444, 0xFF000400);
    public static final Style LIGHT_RED  = new Style(0xFFFFFFFF, 0xFFC02525, 0xFFFFFFFF);
    public static final Style DISABLED   = new Style(0xFFA0A0A0, 0xFF252525, 0xFF000000);

    // -----------------------------------------------------------------------
    // Vanilla-style button sprites (same identifiers vanilla uses in 1.21+)
    // -----------------------------------------------------------------------
    private static final Identifier SPRITE_NORMAL      = Identifier.withDefaultNamespace("widget/button");
    private static final Identifier SPRITE_DISABLED    = Identifier.withDefaultNamespace("widget/button_disabled");
    private static final Identifier SPRITE_HIGHLIGHTED  = Identifier.withDefaultNamespace("widget/button_highlighted");

    // -----------------------------------------------------------------------
    // Fields
    // -----------------------------------------------------------------------

    private Style defaultStyle;

    private Style hoverStyle;

    private Style disabledStyle;

    private final boolean clickSound;

    private boolean drawBackground = true;

    private @Nullable Supplier<Component> tooltipSupplier;

    private final Consumer<MenuButton> onPress;

    // -----------------------------------------------------------------------
    // Constructors
    // -----------------------------------------------------------------------

    public MenuButton(
            int x, int y, int width, int height,
            Component message,
            Style defaultStyle,
            Style hoverStyle,
            Style disabledStyle,
            boolean clickSound,
            Consumer<MenuButton> onPress
    ) {
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

    public MenuButton(int x, int y, int width, int height, Component message, Runnable onPress) {
        this(x, y, width, height, message, DARK_GRAY, GRAY, DISABLED, true, btn -> onPress.run());
    }

    // -----------------------------------------------------------------------
    // Style helpers
    // -----------------------------------------------------------------------

    private Style currentStyle() {
        if (!this.active) return disabledStyle;
        return (this.isHoveredOrFocused()) ? hoverStyle : defaultStyle;
    }

    // -----------------------------------------------------------------------
    // AbstractButton implementation
    // -----------------------------------------------------------------------

    @Override
    protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        Style style = currentStyle();

        if (drawBackground) {
            drawStyledBackground(graphics, style);
        }

        // Draw label (centred, scrolling if too long)
        ActiveTextCollector text = graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.TOOLTIP_ONLY);
        graphics.centeredText(
                Minecraft.getInstance().font,
                this.getMessage(),
                this.getX() + this.getWidth() / 2,
                this.getY() + (this.getHeight() - 8) / 2,
                style.textColor
        );

        // Tooltip
        if (tooltipSupplier != null && this.isHovered()) {
            Component tip = tooltipSupplier.get();
            if (tip != null) {
                graphics.setTooltipForNextFrame(Minecraft.getInstance().font, tip, mouseX, mouseY);
            }
        }
    }

    private void drawStyledBackground(GuiGraphicsExtractor graphics, Style style) {
        int x = getX();
        int y = getY();
        int r = x + getWidth();
        int b = y + getHeight();

        // Base fill
        graphics.fill(x + 1, y + 1, r - 1, b - 1, style.buttonColor);

        // Highlight — left edge
        graphics.fill(x, y, x + 1, b, style.highlightColor);
        // Highlight — top edge (skip top-left pixel already drawn)
        graphics.fill(x + 1, y, r, y + 1, style.highlightColor);

        // Shadow — right edge
        graphics.fill(r - 1, y, r, b, style.shadowColor);
        // Shadow — bottom edge (skip bottom-right pixel already drawn)
        graphics.fill(x, b - 1, r - 1, b, style.shadowColor);

        // Outline (1 px outside the widget on all four sides)
        int ol = style.outlineColor;
        graphics.fill(x - 1, y - 1, r + 1, y,     ol); // top
        graphics.fill(x - 1, b,     r + 1, b + 1,  ol); // bottom
        graphics.fill(x - 1, y,     x,     b,       ol); // left
        graphics.fill(r,     y,     r + 1, b,       ol); // right
    }

    // -----------------------------------------------------------------------
    // Press / sound
    // -----------------------------------------------------------------------

    @Override
    public void onPress(InputWithModifiers input) {
        if (!this.active) return;
        if (clickSound) {
            playButtonClickSound(Minecraft.getInstance().getSoundManager());
        }
        onPress.accept(this);
    }

    // -----------------------------------------------------------------------
    // Narration
    // -----------------------------------------------------------------------

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        defaultButtonNarrationText(output);
        if (tooltipSupplier != null) {
            Component tip = tooltipSupplier.get();
            if (tip != null) {
                output.add(NarratedElementType.HINT, tip);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Builder-style setters
    // -----------------------------------------------------------------------

    public MenuButton setDefaultStyle(Style style) {
        this.defaultStyle = style;
        return this;
    }

    public MenuButton setHoverStyle(Style style) {
        this.hoverStyle = style;
        return this;
    }

    public MenuButton setDisabledStyle(Style style) {
        this.disabledStyle = style;
        return this;
    }

    public MenuButton setStyles(Style defaultStyle, Style hoverStyle) {
        this.defaultStyle = defaultStyle;
        this.hoverStyle   = hoverStyle;
        return this;
    }

    public MenuButton setDrawBackground(boolean drawBackground) {
        this.drawBackground = drawBackground;
        return this;
    }

    public MenuButton setTooltip(Supplier<Component> supplier) {
        this.tooltipSupplier = supplier;
        return this;
    }

    public MenuButton setTooltip(Component tooltip) {
        return setTooltip(() -> tooltip);
    }

    // -----------------------------------------------------------------------
    // Style record
    // -----------------------------------------------------------------------

    public record Style(
            int textColor,
            int buttonColor,
            int outlineColor,
            int highlightColor,
            int shadowColor
    ) {
        public Style(int textColor, int buttonColor, int outlineColor) {
            this(
                    textColor,
                    buttonColor,
                    outlineColor,
                    brighten(buttonColor, 0.4f),
                    darken(buttonColor,  0.5f, 0x80)
            );
        }

        // ----------------------------------------------------------------
        // Color math helpers (operate on ARGB ints)
        // ----------------------------------------------------------------

        private static int brighten(int argb, float factor) {
            int a = ARGB.alpha(argb);
            int r = Math.min(255, (int) (ARGB.red(argb)   * (1f + factor)));
            int g = Math.min(255, (int) (ARGB.green(argb) * (1f + factor)));
            int b = Math.min(255, (int) (ARGB.blue(argb)  * (1f + factor)));
            return ARGB.color(a, r, g, b);
        }

        private static int darken(int argb, float factor, int alpha) {
            int r = Math.max(0, (int) (ARGB.red(argb)   * (1f - factor)));
            int g = Math.max(0, (int) (ARGB.green(argb) * (1f - factor)));
            int b = Math.max(0, (int) (ARGB.blue(argb)  * (1f - factor)));
            return ARGB.color(alpha, r, g, b);
        }
    }
}