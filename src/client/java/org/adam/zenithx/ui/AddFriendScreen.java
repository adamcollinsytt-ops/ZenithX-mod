package org.adam.zenithx.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.adam.zenithx.HostClient;
import org.adam.zenithx.ui.components.MenuButton;

public class AddFriendScreen extends HostBaseScreen {

    private static final int DW = 220;
    private static final int DH = 110;

    private static final int C_BG        = 0xFF1A1A1A;
    private static final int C_TOPBAR    = 0xFF212121;
    private static final int C_BORDER    = 0xFF3A3A3A;
    private static final int C_TEXT      = 0xFFD4D0C8;
    private static final int C_DIM       = 0xFF888888;
    private static final int C_WHITE     = 0xFFFFFFFF;
    private static final int C_INPUT_BG  = 0xFF111111;
    private static final int C_INPUT_OK  = 0xFF3A3A3A;
    private static final int C_INPUT_ERR = 0xFF8A2222;

    private static final MenuButton.Style STYLE_ADD =
            new MenuButton.Style(C_WHITE, 0xFF2A5296, 0xFF1A3A76);
    private static final MenuButton.Style STYLE_ADD_HOV =
            new MenuButton.Style(C_WHITE, 0xFF1A3A76, 0xFF4A90D9);

    private static final MenuButton.Style STYLE_CANCEL =
            new MenuButton.Style(C_TEXT, 0xFF252525, C_BORDER);
    private static final MenuButton.Style STYLE_CANCEL_HOV =
            new MenuButton.Style(C_WHITE, 0xFF303030, C_BORDER);

    private static final int KEY_ENTER    = 257;
    private static final int KEY_KP_ENTER = 335;
    private static final int KEY_ESCAPE   = 256;

    private final HostBaseScreen parent;
    private EditBox  inputBox;
    private boolean  invalid   = false;
    private boolean  submitted = false;

    public AddFriendScreen(HostBaseScreen parent) {
        super(Component.literal("Add Friend"));
        this.parent = parent;
    }

    private int px() { return (width  - DW) / 2; }
    private int py() { return (height - DH) / 2; }

    @Override
    protected void init() {
        int px = px(), py = py();

        inputBox = new EditBox(
                font,
                px + 12, py + 46,
                DW - 24, 14,
                Component.literal("username")
        );
        inputBox.setHint(Component.literal("Enter a Minecraft username..."));
        inputBox.setBordered(false);
        inputBox.setMaxLength(32);
        inputBox.setValue("");
        inputBox.setResponder(v -> invalid = false);
        addRenderableWidget(inputBox);
        setFocused(inputBox);

        addRenderableWidget(new MenuButton(
                px + 8,
                py + DH - 26,
                (DW - 24) / 2,
                16,
                Component.literal("Cancel"),
                STYLE_CANCEL, STYLE_CANCEL_HOV,
                MenuButton.DISABLED, true,
                btn -> goBack()
        ));

        addRenderableWidget(new MenuButton(
                px + 12 + (DW - 24) / 2,
                py + DH - 26,
                (DW - 24) / 2,
                16,
                Component.literal("Add"),
                STYLE_ADD, STYLE_ADD_HOV,
                MenuButton.DISABLED, true,
                btn -> trySend()
        ));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mx, int my, float a) {
        int px = px(), py = py();

        gfx.fill(0, 0, width, height, 0x88000000);
        gfx.fill(px + 4, py + 4, px + DW + 4, py + DH + 4, 0x55000000);
        gfx.fill(px, py, px + DW, py + DH, C_BG);
        drawOutline(gfx, px, py, DW, DH, C_BORDER);

        gfx.fill(px, py, px + DW, py + 24, C_TOPBAR);
        gfx.fill(px, py + 24, px + DW, py + 25, C_BORDER);

        gfx.centeredText(font, Component.literal("Add Friend"),
                px + DW / 2, py + 8, C_TEXT);
        gfx.centeredText(font, Component.literal("Enter a Minecraft username"),
                px + DW / 2, py + 30, C_DIM);

        gfx.fill(px + 10, py + 42, px + DW - 10, py + 64, C_INPUT_BG);
        drawOutline(gfx, px + 10, py + 42, DW - 20, 22, invalid ? C_INPUT_ERR : C_INPUT_OK);

        super.extractRenderState(gfx, mx, my, a);
    }

    private void drawOutline(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.fill(x,         y,         x + w, y + 1,     c);
        g.fill(x,         y + h - 1, x + w, y + h,     c);
        g.fill(x,         y,         x + 1, y + h,     c);
        g.fill(x + w - 1, y,         x + w, y + h,     c);
    }

    private void trySend() {
        if (submitted) return;
        if (inputBox == null) return;

        String raw  = inputBox.getValue();
        String name = (raw == null) ? "" : raw.trim();

        if (!isValidUsername(name)) {
            invalid = true;
            return;
        }

        submitted = true;

        new Thread(() -> {
            try {
                HostClient.getInstance().sendFriendRequest(name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

        goBack();
    }

    private void goBack() {
        if (minecraft == null) return;
        minecraft.execute(() -> {
            try {
                if (parent != null)
                    minecraft.setScreen(parent);
                else
                    onClose();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static boolean isValidUsername(String s) {
        if (s == null || s.length() < 3 || s.length() > 16) return false;
        for (char c : s.toCharArray()) {
            if (!Character.isLetterOrDigit(c) && c != '_') return false;
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == KEY_ENTER || key == KEY_KP_ENTER) {
            trySend();
            return true;
        }
        if (key == KEY_ESCAPE) {
            goBack();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}