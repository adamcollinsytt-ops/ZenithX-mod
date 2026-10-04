package org.adam.zenithx.ui;

import net.minecraft.client.Minecraft;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? } else {
/*import net.minecraft.client.gui.GuiGraphics;*/
//? }
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.adam.zenithx.HostClient;
import org.adam.zenithx.ui.components.MenuButton;
import org.adam.zenithx.handlers.ZenithXClient;

public class NoConnectionWarningScreen extends HostBaseScreen {

    private static final int PW = 260;
    private static final int PH = 135;

    private static final int C_BG = 0xFF1A1A1A;
    private static final int C_BORDER = 0xFF3A3A3A;
    private static final int C_TOPBAR = 0xFF212121;
    private static final int C_TEXT = 0xFFD4D0C8;
    private static final int C_DIM = 0xFF888888;

    private final Runnable onContinue;
    private final Runnable onRetry;
    private final String failReason;

    private boolean handled = false;

    public NoConnectionWarningScreen(Runnable onContinue, Runnable onRetry, String failReason) {
        super(Component.literal(isOutdated(failReason) ? "Outdated Version" : "Connection Lost"));
        this.onContinue = onContinue;
        this.onRetry = onRetry;
        this.failReason = failReason != null ? failReason : "";

        System.out.println("[ZenithX] NoConnectionWarningScreen opened with failReason: '" + this.failReason + "'");
    }

    private static boolean isOutdated(String reason) {
        if (reason == null) return false;
        String lower = reason.toLowerCase().trim();
        return lower.equals("outdated_version") || 
               lower.contains("outdated") || 
               lower.contains("version") || 
               lower.contains("update");
    }

    private int px() { return (width - PW) / 2; }
    private int py() { return (height - PH) / 2; }

    //? if <26 {
    /*private void goToTitle() {
        minecraft.setScreen(new TitleScreen());
    }*/
    //? }

    @Override
    protected void init() {
        int px = px();
        int py = py();
        boolean outdated = isOutdated(failReason);

        if (outdated) {
            addRenderableWidget(new MenuButton(
                    px + (PW - 120) / 2, py + PH - 32, 120, 20,
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

                        Minecraft.getInstance().execute(() -> {
                            try {
                                if (onContinue != null) onContinue.run();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //? if >=26 {
                            minecraft.setScreen(new TitleScreen());
                            //? } else {
                            /*goToTitle();*/
                            //? }
                        });
                    }
            ));
        } else {
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
                        
                        Minecraft.getInstance().execute(() -> {
                            try {
                                if (onContinue != null) onContinue.run();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            //? if >=26 {
                            minecraft.setScreen(new TitleScreen());
                            //? } else {
                            /*goToTitle();*/
                            //? }
                        });
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
                        
                        java.util.concurrent.CompletableFuture.runAsync(() -> {
                            try {
                                if (onRetry != null) onRetry.run();
                                HostClient.tryReconnectNow();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }).thenRun(() -> {
                            Minecraft.getInstance().execute(() -> {
                                //? if >=26 {
                                minecraft.setScreen(new TitleScreen());
                                //? } else {
                                /*goToTitle();*/
                                //? }
                            });
                        });
                    }
            ));
        }
    }

    //? if >=26 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mx, int my, float a) {
    //? } else {
    /*@Override
    public void render(GuiGraphics gfx, int mx, int my, float a) {*/
    //? }
        gfx.fill(0, 0, width, height, 0x88000000);

        int px = px();
        int py = py();
        boolean outdated = isOutdated(failReason);

        gfx.fill(px + 4, py + 4, px + PW + 4, py + PH + 4, 0x55000000);
        gfx.fill(px, py, px + PW, py + PH, C_BG);
        //? if >=26 {
        gfx.outline(px, py, PW, PH, C_BORDER);
        //? } else {
        /*gfx.fill(px,         py,         px + PW, py + 1,      C_BORDER);
        gfx.fill(px,         py + PH - 1, px + PW, py + PH,     C_BORDER);
        gfx.fill(px,         py,         px + 1,  py + PH,      C_BORDER);
        gfx.fill(px + PW - 1, py,        px + PW, py + PH,      C_BORDER);*/
        //? }
        gfx.fill(px, py, px + PW, py + 22, C_TOPBAR);

        String titleText = outdated ? "Outdated Version" : "Connection Lost";
        //? if >=26 {
        gfx.centeredText(font, Component.literal(titleText), px + PW / 2, py + 7, C_TEXT);

        if (outdated) {
            gfx.centeredText(font, Component.literal("Your mod version is outdated!"), px + PW / 2, py + 36, 0xFFFF5555);
            gfx.centeredText(font, Component.literal("Clicking Continue will let you play normally,"), px + PW / 2, py + 56, C_TEXT);
            gfx.centeredText(font, Component.literal("but you won't connect to ZenithX Network."), px + PW / 2, py + 68, C_DIM);
        } else {
            gfx.centeredText(font, Component.literal("Connection to ZenithX Network was lost."), px + PW / 2, py + 44, C_TEXT);
            gfx.centeredText(font, Component.literal("Please try again."), px + PW / 2, py + 58, C_DIM);
        }
        //? } else {
        /*gfx.drawCenteredString(font, Component.literal(titleText), px + PW / 2, py + 7, C_TEXT);

        if (outdated) {
            gfx.drawCenteredString(font, Component.literal("Your mod version is outdated!"), px + PW / 2, py + 36, 0xFFFF5555);
            gfx.drawCenteredString(font, Component.literal("Clicking Continue will let you play normally,"), px + PW / 2, py + 56, C_TEXT);
            gfx.drawCenteredString(font, Component.literal("but you won't connect to ZenithX Network."), px + PW / 2, py + 68, C_DIM);
        } else {
            gfx.drawCenteredString(font, Component.literal("Connection to ZenithX Network was lost."), px + PW / 2, py + 44, C_TEXT);
            gfx.drawCenteredString(font, Component.literal("Please try again."), px + PW / 2, py + 58, C_DIM);
        }*/
        //? }

        //? if >=26 {
        super.extractRenderState(gfx, mx, my, a);
        //? } else {
        /*super.render(gfx, mx, my, a);*/
        //? }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
