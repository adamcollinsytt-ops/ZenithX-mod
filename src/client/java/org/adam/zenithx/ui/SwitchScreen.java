package org.adam.zenithx.ui;

//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
//? } else {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;*/
//? }
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.*;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;
import org.adam.zenithx.account.AccountManager;
import org.adam.zenithx.ui.components.MenuButton;

import java.util.List;

public class SwitchScreen extends Screen {
    private static final int PANEL_W = 224;
    private static final int ROW_W = 190;
    private static final int ROW_H = 20;
    private static final int ROW_STEP = 24;
    private static final int BTN_W = 120;
    private static final int BTN_H = 20;

    private static final int PANEL_BG = 0xF0181818;
    private static final int PANEL_BORDER = 0xFF3A3A3A;
    private static final int ROW_BG = 0xFF2B2B2B;
    private static final int ROW_BORDER = 0xFF4A4A4A;
    private static final int ROW_HOVER = 0xFF363636;
    private static final int BLUE = 0xFF4A90E2;
    private static final int BTN_BG = 0xFF2B5797;
    private static final int BTN_HOVER = 0xFF3468B0;
    private static final int BTN_BORDER = 0xFF4A86E8;
    private static final int RED_COLOR = 0xFFFF5555;

    private final Screen parent;
    private volatile String status = "";
    private volatile boolean busy = false;
    private MenuButton addAccountButton;
    //? if >=26 {
    private static final Identifier STEVE_SKIN = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    //? } else if >=1.20.5 {
    /*private static final ResourceLocation STEVE_SKIN = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");*/
    //? } else {
    /*private static final ResourceLocation STEVE_SKIN = new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");*/
    //? }

    private AccountManager.Account menuAccount = null;
    private int menuX = 0;
    private int menuY = 0;

    public SwitchScreen(Screen parent) {
        super(Component.literal("Your Accounts"));
        this.parent = parent;
        AccountManager.load();
    }

    @Override
    protected void init() {
        this.clearWidgets();
        super.init();
        //? if >=26 {
        if (parent != null) parent.init(this.width, this.height);
        //? }

        List<AccountManager.Account> accounts = AccountManager.accounts();
        int n = Math.max(accounts.size(), 1);
        int cx = this.width / 2;
        int rowsTop = panelTop(n) + 44;
        int btnX = cx - BTN_W / 2;
        int btnY = rowsTop + n * ROW_STEP + 11;

        MenuButton.Style defaultStyle = new MenuButton.Style(0xFFFFFFFF, BTN_BG, BTN_BORDER);
        MenuButton.Style hoverStyle = new MenuButton.Style(0xFFFFFFFF, BTN_HOVER, BTN_BORDER);

        this.addAccountButton = this.addRenderableWidget(
            new MenuButton(
                btnX, btnY, BTN_W, BTN_H,
                Component.literal("Add Account"),
                defaultStyle, hoverStyle, MenuButton.DISABLED, true,
                btn -> {
                    if (!busy) {
                        busy = true;
                        status = "Opening Microsoft login...";
                        AccountManager.addAccountAsync(this::onDeviceCode, s -> status = s, () -> {
                            busy = false;
                            if (this.minecraft != null) {
                                this.minecraft.execute(this::init);
                            }
                        });
                    }
                }
            )
        );
    }

    private int panelTop(int n) {
        return this.height / 2 - (91 + 24 * n) / 2;
    }

    //? if >=26 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (parent != null) parent.extractRenderState(g, -1, -1, partialTick);
    //? } else {
    /*@Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (parent != null) parent.render(g, -1, -1, partialTick);*/
    //? }
        g.fill(0, 0, this.width, this.height, 0x88000000);

        List<AccountManager.Account> accounts = AccountManager.accounts();
        int n = Math.max(accounts.size(), 1);
        int cx = this.width / 2;
        int panelX = cx - PANEL_W / 2;
        int top = panelTop(n);
        int panelHeight = 91 + 24 * n;

        g.fill(panelX - 1, top - 1, panelX + PANEL_W + 1, top + panelHeight + 1, PANEL_BORDER);
        g.fill(panelX, top, panelX + PANEL_W, top + panelHeight, PANEL_BG);

        //? if >=26 {
        g.centeredText(this.font, this.title, cx, top + 19, 0xFFFFFFFF);
        //? } else {
        /*g.drawCenteredString(this.font, this.title, cx, top + 19, 0xFFFFFFFF);*/
        //? }

        String active = AccountManager.activeUuid();
        int rowX = cx - ROW_W / 2;
        int rowsTop = top + 44;
        boolean hasMultipleAccounts = accounts.size() > 1;

        for (int i = 0; i < accounts.size(); i++) {
            AccountManager.Account acc = accounts.get(i);
            int rowY = rowsTop + i * ROW_STEP;
            boolean hover = inside(mouseX, mouseY, rowX, rowY, ROW_W, ROW_H);

            g.fill(rowX - 1, rowY - 1, rowX + ROW_W + 1, rowY + ROW_H + 1, ROW_BORDER);
            g.fill(rowX, rowY, rowX + ROW_W, rowY + ROW_H, hover ? ROW_HOVER : ROW_BG);

            SkinCache.request(acc.name);
            drawHead(g, acc.name, rowX + 5, rowY + 4);

            //? if >=26 {
            g.text(this.font, acc.name, rowX + 22, rowY + 6, 0xFFFFFFFF, true);
            //? } else {
            /*g.drawString(this.font, acc.name, rowX + 22, rowY + 6, 0xFFFFFFFF, true);*/
            //? }

            if (isSameUuid(acc.uuid, active)) {
                int checkX = hasMultipleAccounts ? (rowX + ROW_W - 28) : (rowX + ROW_W - 16);
                //? if >=26 {
                g.text(this.font, "✔", checkX, rowY + 6, BLUE, true);
                //? } else {
                /*g.drawString(this.font, "✔", checkX, rowY + 6, BLUE, true);*/
                //? }
            }

            if (hasMultipleAccounts) {
                boolean dotsHover = inside(mouseX, mouseY, rowX + ROW_W - 16, rowY, 16, ROW_H);
                //? if >=26 {
                g.text(this.font, "⋮", rowX + ROW_W - 12, rowY + 5, dotsHover ? BLUE : 0xFFAAAAAA, true);
                //? } else {
                /*g.drawString(this.font, "⋮", rowX + ROW_W - 12, rowY + 5, dotsHover ? BLUE : 0xFFAAAAAA, true);*/
                //? }
            }
        }

        if (addAccountButton != null) {
            int btnX = cx - BTN_W / 2;
            int btnY = rowsTop + n * ROW_STEP + 11;
            addAccountButton.setX(btnX);
            addAccountButton.setY(btnY);
            addAccountButton.active = !busy;
            addAccountButton.setMessage(Component.literal(busy ? "Please wait..." : "Add Account"));
        }

        //? if >=26 {
        super.extractRenderState(g, mouseX, mouseY, partialTick);
        //? } else {
        /*super.render(g, mouseX, mouseY, partialTick);*/
        //? }

        if (menuAccount != null) {
            int mw = 70;
            int mh = 18;
            g.fill(menuX - 1, menuY - 1, menuX + mw + 1, menuY + mh + 1, PANEL_BORDER);
            boolean deleteHover = inside(mouseX, mouseY, menuX, menuY, mw, mh);
            g.fill(menuX, menuY, menuX + mw, menuY + mh, deleteHover ? 0xFF552222 : ROW_BG);
            //? if >=26 {
            g.text(this.font, "🗑 Delete", menuX + 6, menuY + 5, RED_COLOR, true);
            //? } else {
            /*g.drawString(this.font, "🗑 Delete", menuX + 6, menuY + 5, RED_COLOR, true);*/
            //? }
        }

        if (!status.isEmpty()) {
            //? if >=26 {
            g.centeredText(this.font, Component.literal(status), cx, top + panelHeight + 8, 0xFFAAAAAA);
            //? } else {
            /*g.drawCenteredString(this.font, Component.literal(status), cx, top + panelHeight + 8, 0xFFAAAAAA);*/
            //? }
        }
    }

    //? if >=26 {
    private void drawHead(GuiGraphicsExtractor g, String username, int x, int y) {
        Identifier skinId = SkinCache.get(username);
    //? } else {
    /*private void drawHead(GuiGraphics g, String username, int x, int y) {
        ResourceLocation skinId = SkinCache.get(username);*/
    //? }
        if (skinId != null) {
            drawSkinHead(g, skinId, x, y);
        } else {
            drawDefaultHead(g, x, y);
        }
    }

    //? if >=26 {
    private void drawDefaultHead(GuiGraphicsExtractor g, int x, int y) {
    //? } else {
    /*private void drawDefaultHead(GuiGraphics g, int x, int y) {*/
    //? }
        drawSkinHead(g, STEVE_SKIN, x, y);
    }

    //? if >=26 {
    private void drawSkinHead(GuiGraphicsExtractor g, Identifier skin, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8.0F, 8.0F, 12, 12, 8, 8, 64, 64);
        g.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 40.0F, 8.0F, 12, 12, 8, 8, 64, 64);
    }
    //? } else if >=1.21.9 {
    /*private void drawSkinHead(GuiGraphics g, ResourceLocation skin, int x, int y) {
        g.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 8.0F, 8.0F, 12, 12, 8, 8, 64, 64);
        g.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, 40.0F, 8.0F, 12, 12, 8, 8, 64, 64);
    }*/
    //? } else if >=1.20.5 {
    /*private void drawSkinHead(GuiGraphics g, ResourceLocation skin, int x, int y) {
        g.blit(skin, x, y, 8.0F, 8.0F, 12, 12, 8, 8, 64, 64);
        g.blit(skin, x, y, 40.0F, 8.0F, 12, 12, 8, 8, 64, 64);
    }*/
    //? } else {
    /*private void drawSkinHead(GuiGraphics g, ResourceLocation skin, int x, int y) {
        g.blit(skin, x, y, 8, 8, 12, 12, 8, 8, 64, 64);
        g.blit(skin, x, y, 40, 8, 12, 12, 8, 8, 64, 64);
    }*/
    //? }

    private static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static boolean isSameUuid(String u1, String u2) {
        if (u1 == null || u2 == null) return false;
        return u1.replace("-", "").equalsIgnoreCase(u2.replace("-", ""));
    }

    //? if >=26 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || busy) return super.mouseClicked(event, doubleClick);

        double mx = event.x();
        double my = event.y();
    //? } else {
    /*@Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0 || busy) return super.mouseClicked(mx, my, button);*/
    //? }

        if (menuAccount != null) {
            int mw = 70;
            int mh = 18;
            if (inside(mx, my, menuX, menuY, mw, mh)) {
                AccountManager.Account accToDelete = menuAccount;
                menuAccount = null;

                boolean isActive = isSameUuid(accToDelete.uuid, AccountManager.activeUuid());
                AccountManager.removeAccount(accToDelete.uuid);

                List<AccountManager.Account> remaining = AccountManager.accounts();
                if (isActive && !remaining.isEmpty()) {
                    busy = true;
                    AccountManager.switchAsync(remaining.get(0), s -> status = s, () -> {
                        busy = false;
                        if (this.minecraft != null) {
                            this.minecraft.execute(this::init);
                        }
                    });
                } else {
                    this.init();
                }
                return true;
            } else {
                menuAccount = null;
            }
        }

        List<AccountManager.Account> accounts = AccountManager.accounts();
        int n = Math.max(accounts.size(), 1);
        int cx = this.width / 2;
        int rowX = cx - ROW_W / 2;
        int rowsTop = panelTop(n) + 44;
        boolean hasMultipleAccounts = accounts.size() > 1;

        for (int i = 0; i < accounts.size(); i++) {
            int rowY = rowsTop + i * ROW_STEP;
            AccountManager.Account acc = accounts.get(i);

            if (hasMultipleAccounts && inside(mx, my, rowX + ROW_W - 16, rowY, 16, ROW_H)) {
                menuAccount = acc;
                menuX = (int) mx;
                menuY = (int) my;
                return true;
            }

            if (inside(mx, my, rowX, rowY, ROW_W, ROW_H)) {
                if (isSameUuid(acc.uuid, AccountManager.activeUuid())) return true;
                busy = true;
                AccountManager.switchAsync(acc, s -> status = s, () -> busy = false);
                return true;
            }
        }

        //? if >=26 {
        return super.mouseClicked(event, doubleClick);
        //? } else {
        /*return super.mouseClicked(mx, my, button);*/
        //? }
    }

    private void onDeviceCode(StepMsaDeviceCode.MsaDeviceCode code) {
        String uri = code.getDirectVerificationUri();
        if (this.minecraft != null) {
            this.minecraft.execute(() -> {
                this.minecraft.keyboardHandler.setClipboard(code.getUserCode());
                Util.getPlatform().openUri(uri);
            });
        }
        status = "Code: " + code.getUserCode() + " (copied) - finish login in your browser";
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(this.parent);
    }
}
