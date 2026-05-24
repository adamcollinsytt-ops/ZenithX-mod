package org.adam.zenithx.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.adam.zenithx.HostClient;
import org.adam.zenithx.ui.components.MenuButton;
import org.adam.zenithx.handlers.ZenithXClient;

public class NoConnectionWarningScreen extends HostBaseScreen {

    private static final int PW = 250;
    private static final int PH = 120;

    private static final int C_BG = 0xFF1A1A1A;
    private static final int C_BORDER = 0xFF3A3A3A;
    private static final int C_TOPBAR = 0xFF212121;
    private static final int C_TEXT = 0xFFD4D0C8;
    private static final int C_DIM = 0xFF888888;

    private final Runnable onContinue;
    private final Runnable onRetry;

    private boolean handled = false;

    public NoConnectionWarningScreen(Runnable onContinue, Runnable onRetry) {
        super(Component.literal("Connection Lost"));
        this.onContinue = onContinue;
        this.onRetry = onRetry;

        new Exception("NoConnectionWarningScreen opened from:").printStackTrace();
    }

    private int px() { return (width - PW) / 2; }
    private int py() { return (height - PH) / 2; }

    @Override
    protected void init() {

        int px = px();
        int py = py();

        addRenderableWidget(new MenuButton(
                px + 18, py + PH - 30, 96, 18,
                Component.literal("Continue"),
                MenuButton.DARK_GRAY,
                MenuButton.GRAY,
                MenuButton.DISABLED,
                true,
                btn -> {
                    if (handled) return;
                    handled = true;
                    HostClient.USER_DISMISSED = true;
                    ZenithXClient.connectionChecked = true;
                    if (onContinue != null) onContinue.run();
                    minecraft.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
                }
        ));

        addRenderableWidget(new MenuButton(
                px + PW - 114, py + PH - 30, 96, 18,
                Component.literal("Try Again"),
                MenuButton.BLUE,
                MenuButton.LIGHT_BLUE,
                MenuButton.DISABLED,
                true,
                btn -> {
                    if (handled) return;
                    handled = true;
                    HostClient.USER_DISMISSED = true;
                    ZenithXClient.connectionChecked = true;
                    if (onRetry != null) onRetry.run();
                    HostClient.tryReconnectNow();
                    minecraft.setScreen(new TitleScreen());
                }
        ));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mx, int my, float a) {

        gfx.fill(0, 0, width, height, 0x88000000);

        int px = px();
        int py = py();

        gfx.fill(px + 4, py + 4, px + PW + 4, py + PH + 4, 0x55000000);
        gfx.fill(px, py, px + PW, py + PH, C_BG);
        gfx.outline(px, py, PW, PH, C_BORDER);
        gfx.fill(px, py, px + PW, py + 22, C_TOPBAR);

        gfx.centeredText(font, Component.literal("Connection Lost"),
                px + PW / 2, py + 7, C_TEXT);

        gfx.centeredText(font, Component.literal("Connection to external server was lost."),
                px + PW / 2, py + 44, C_TEXT);

        gfx.centeredText(font, Component.literal("Please try again."),
                px + PW / 2, py + 58, C_DIM);

        super.extractRenderState(gfx, mx, my, a);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}