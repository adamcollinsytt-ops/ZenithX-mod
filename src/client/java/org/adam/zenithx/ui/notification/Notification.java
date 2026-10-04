package org.adam.zenithx.ui.notification;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? } else {
/*import net.minecraft.client.gui.GuiGraphics;*/
//? }
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.util.Mth;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Environment(EnvType.CLIENT)
public class Notification implements GuiEventListener {

    public static final int WIDTH       = 175;
    public static final int HEIGHT      = 50;
    public static final int HEIGHT_BTN  = 64;
    private static final int TIMER_H    = 3;
    private static final int CLOSE_SIZE = 11;

    // --- ثوابت الألوان الجديدة (Dark / Black Theme) ---
    private static final int C_BG           = 0xFF121212; // خلفية سوداء داكنة
    private static final int C_BORDER       = 0xFF2A2A2A; // حد أسود رمادي
    private static final int C_BORDER_HOVER = 0xFF444444; // حد أسود أفتح عند مرور الماوس
    private static final int C_TIMER        = 0xFF333333; // شريط الوقت باللون الأسود الداكن
    private static final int C_TIMER_HOVER  = 0xFF555555; // شريط الوقت عند التمرير
    private static final int C_TITLE        = 0xFFFFFFFF; // عنوان أبيض
    private static final int C_TEXT         = 0xFFAAAAAA; // نص رمادي فاتح
    private static final int C_CLOSE        = 0xFFFF5555; // زر الإغلاق
    private static final int C_CLOSE_X      = 0xFFFFFFFF;
    private static final int C_BTN          = 0xFF252525; // زر التفاعل أسود
    private static final int C_BTN_HOVER    = 0xFF3A3A3A; // زر التفاعل عند التمرير
    private static final int C_BTN_TEXT     = 0xFFFFFFFF; // نص الزر أبيض

    public final String   title;
    public final String   text;
    public final float    duration;
    public final boolean  persistent;
    public final Object   uniqueId;

    private final String   actionLabel;
    private final Runnable actionCallback;

    private final Runnable          onClick;
    private final Runnable          onClosed;
    private final Supplier<Boolean> timerEnabled;

    private float   currentX;
    private float   currentHeight;
    private final int targetHeight;

    private boolean animatingIn    = true;
    private boolean animatingOut   = false;
    private boolean animatingClose = false;

    private float   timerProgress  = 0f;
    private boolean timerStarted   = false;

    private boolean dragging         = false;
    private float   dragStartX       = 0f;
    private float   dragOffsetPx     = 0f;
    private boolean couldBeAClick    = true;
    private boolean didTriggerAction = false;
    private boolean clicked          = false;

    private boolean hovered    = false;
    private boolean btnHovered = false;
    private boolean focused    = false;
    private boolean dismissed  = false;

    public int screenY = 0;

    private int closeBtnX, closeBtnY;
    private int actionBtnX, actionBtnY, actionBtnW, actionBtnH;

    public Notification(
            String title,
            String text,
            float duration,
            boolean persistent,
            Object uniqueId,
            Supplier<Boolean> timerEnabled,
            Runnable onClick,
            Runnable onClosed,
            CompletableFuture<Void> dismissFuture,
            CompletableFuture<Void> dismissInstantlyFuture
    ) {
        this(title, text, duration, persistent, uniqueId, timerEnabled,
                onClick, onClosed, dismissFuture, dismissInstantlyFuture,
                null, null);
    }

    public Notification(
            String title,
            String text,
            float duration,
            boolean persistent,
            Object uniqueId,
            Supplier<Boolean> timerEnabled,
            Runnable onClick,
            Runnable onClosed,
            CompletableFuture<Void> dismissFuture,
            CompletableFuture<Void> dismissInstantlyFuture,
            String actionLabel,
            Runnable actionCallback
    ) {
        this.title          = title;
        this.text           = text;
        this.duration       = duration;
        this.persistent     = persistent;
        this.uniqueId       = uniqueId;
        this.timerEnabled   = timerEnabled != null ? timerEnabled : () -> true;
        this.onClick        = onClick        != null ? onClick        : () -> {};
        this.onClosed       = onClosed       != null ? onClosed       : () -> {};
        this.actionLabel    = actionLabel;
        this.actionCallback = actionCallback != null ? actionCallback : () -> {};

        this.targetHeight  = (actionLabel != null) ? HEIGHT_BTN : HEIGHT;
        this.currentX      = 99999f;
        this.currentHeight = targetHeight;

        if (dismissFuture != null)
            dismissFuture.thenRun(this::animateCompleteTimerThenOut);
        if (dismissInstantlyFuture != null)
            dismissInstantlyFuture.thenRun(this::dismissInstantly);
    }

    public void tick(float deltaSeconds, int screenWidth) {
        if (dismissed) return;

        float baseX = screenWidth - WIDTH - 2f;

        if (animatingIn) {
            if (currentX > screenWidth) currentX = screenWidth + 5f;
            float target = baseX + dragOffsetPx;
            currentX = expDecay(currentX, target, 12f, deltaSeconds);
            if (Math.abs(currentX - target) < 0.5f) {
                currentX    = target;
                animatingIn = false;
                timerStarted = true;
            }
            return;
        }

        if (!animatingOut && !animatingClose) {
            currentX = baseX + dragOffsetPx;
        }

        if (timerStarted && !persistent && !animatingOut && !animatingClose) {
            if (timerEnabled.get() && !hovered) {
                timerProgress += deltaSeconds / duration;
                if (timerProgress >= 1f) {
                    timerProgress = 1f;
                    animateOut();
                }
            }
        }

        if (animatingOut) {
            float offscreen = screenWidth + 10f;
            currentX = expDecay(currentX, offscreen, 10f, deltaSeconds);
            if (Math.abs(currentX - offscreen) < 1f) {
                animatingOut   = false;
                animatingClose = true;
            }
        }

        if (animatingClose) {
            currentHeight = expDecay(currentHeight, 0f, 15f, deltaSeconds);
            if (currentHeight < 0.5f) {
                dismissed = true;
                onClosed.run();
            }
        }
    }

    //? if >=26 {
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    //? } else {
    /*public void render(GuiGraphics graphics, int mouseX, int mouseY) {*/
    //? }
        if (dismissed) return;

        int x = (int) currentX;
        int y = screenY;
        int w = WIDTH;
        int h = (int) currentHeight;

        if (h < 2) return;

        hovered = mouseX >= x && mouseX <= x + w
                && mouseY >= y && mouseY <= y + h;

        Font font = Minecraft.getInstance().font;

        int borderColor = (hovered && !clicked) ? C_BORDER_HOVER : C_BORDER;
        //? if >=26 {
        graphics.outline(x - 1, y - 1, w + 2, h + 2, borderColor);
        //? } else {
        /*graphics.fill(x - 1,     y - 1,     x + w + 1, y,         borderColor);
        graphics.fill(x - 1,     y + h,     x + w + 1, y + h + 1, borderColor);
        graphics.fill(x - 1,     y - 1,     x,         y + h + 1, borderColor);
        graphics.fill(x + w,     y - 1,     x + w + 1, y + h + 1, borderColor);*/
        //? }
        graphics.fill(x, y, x + w, y + h, C_BG);

        if (h > 10) {
            String displayTitle = trimWithEllipsis(title, w - 14, font);
            String displayText  = trimWithEllipsis(text,  w - 14, font);
            //? if >=26 {
            graphics.text(font, displayTitle, x + 7, y + 7,  C_TITLE, true);
            graphics.text(font, displayText,  x + 7, y + 18, C_TEXT,  false);
            //? } else {
            /*graphics.drawString(font, displayTitle, x + 7, y + 7,  C_TITLE, true);
            graphics.drawString(font, displayText,  x + 7, y + 18, C_TEXT,  false);*/
            //? }
        }

        if (actionLabel != null && h > 40) {
            int btnW = font.width(actionLabel) + 14;
            int btnH = 12;
            int btnX = x + w - btnW - 6;
            int btnY = y + h - btnH - 6;

            actionBtnX = btnX;
            actionBtnY = btnY;
            actionBtnW = btnW;
            actionBtnH = btnH;

            btnHovered = mouseX >= btnX && mouseX <= btnX + btnW
                    && mouseY >= btnY && mouseY <= btnY + btnH;

            int btnColor = btnHovered ? C_BTN_HOVER : C_BTN;
            graphics.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnColor);
            //? if >=26 {
            graphics.text(font, actionLabel, btnX + 7, btnY + 2, C_BTN_TEXT, false);
            //? } else {
            /*graphics.drawString(font, actionLabel, btnX + 7, btnY + 2, C_BTN_TEXT, false);*/
            //? }
        }

        if (!persistent && timerStarted && h >= TIMER_H) {
            int timerColor = hovered ? C_TIMER_HOVER : C_TIMER;
            int timerW = (int) (w * timerProgress);
            int timerY = y + h - TIMER_H;
            if (actionLabel != null) timerY = y + h - TIMER_H;
            if (timerW > 0) {
                graphics.fill(x, timerY, x + timerW, timerY + TIMER_H, timerColor);
            }
        }

        if (persistent && actionLabel == null && h > 16) {
            closeBtnX = x + w - CLOSE_SIZE - 5;
            closeBtnY = y + 5;
            graphics.fill(closeBtnX, closeBtnY,
                    closeBtnX + CLOSE_SIZE, closeBtnY + CLOSE_SIZE, C_CLOSE);
            //? if >=26 {
            graphics.text(font, "x", closeBtnX + 3, closeBtnY + 2, C_CLOSE_X, false);
            //? } else {
            /*graphics.drawString(font, "x", closeBtnX + 3, closeBtnY + 2, C_CLOSE_X, false);*/
            //? }
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (dismissed || button != 0) return false;

        if (actionLabel != null
                && mouseX >= actionBtnX && mouseX <= actionBtnX + actionBtnW
                && mouseY >= actionBtnY && mouseY <= actionBtnY + actionBtnH) {
            actionCallback.run();
            animateCompleteTimerThenOut();
            return true;
        }

        if (persistent && actionLabel == null && isInsideCloseBtn((int) mouseX, (int) mouseY)) {
            animateOut();
            return true;
        }

        int x = (int) currentX;
        int y = screenY;
        if (mouseX >= x && mouseX <= x + WIDTH
                && mouseY >= y && mouseY <= y + currentHeight) {
            dragging      = true;
            dragStartX    = (float) mouseX;
            couldBeAClick = true;
            clicked       = false;
            return true;
        }
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!dragging || button != 0) return false;
        dragging = false;

        float pct = Mth.clamp(dragOffsetPx / 170f * 100f, 0f, 100f);

        if (couldBeAClick) {
            if (!didTriggerAction) {
                onClick.run();
                clicked          = true;
                didTriggerAction = true;
                animateCompleteTimerThenOut();
            }
        } else if (pct < 25f) {
            dragOffsetPx = 0f;
        } else {
            animateOut();
        }
        return true;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button,
                                double deltaX, double deltaY) {
        if (!dragging || button != 0) return false;
        dragOffsetPx = Math.max(0f, dragOffsetPx + (float)(mouseX - dragStartX));
        dragStartX   = (float) mouseX;
        if ((dragOffsetPx / 170f * 100f) > 2f) couldBeAClick = false;
        return true;
    }

    @Override public boolean isFocused()           { return focused; }
    @Override public void    setFocused(boolean f) { this.focused = f; }

    public void animateIn() { animatingIn = true; }

    private void animateOut() {
        if (animatingOut || animatingClose || dismissed) return;
        animatingOut = true;
    }

    private void animateCompleteTimerThenOut() {
        if (persistent) { animateOut(); return; }
        timerProgress = 1f;
        animateOut();
    }

    public void dismissInstantly() {
        dismissed = true;
        onClosed.run();
    }

    public boolean isDismissed()       { return dismissed; }
    public int     getRenderedHeight() { return (int) currentHeight; }

    private boolean isInsideCloseBtn(int mx, int my) {
        return mx >= closeBtnX && mx <= closeBtnX + CLOSE_SIZE
                && my >= closeBtnY && my <= closeBtnY + CLOSE_SIZE;
    }

    private static String trimWithEllipsis(String str, int maxWidth, Font font) {
        if (font.width(str) <= maxWidth) return str;
        while (!str.isEmpty() && font.width(str + "...") > maxWidth)
            str = str.substring(0, str.length() - 1);
        return str + "...";
    }

    private static float expDecay(float cur, float target, float speed, float dt) {
        return target + (cur - target) * (float) Math.exp(-speed * dt);
    }
}