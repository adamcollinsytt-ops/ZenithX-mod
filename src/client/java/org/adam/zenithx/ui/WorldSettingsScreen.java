package org.adam.zenithx.ui;

//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
//? } else {
/*import net.minecraft.client.gui.GuiGraphics;*/
//? }
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelSummary;
import org.adam.zenithx.HostSessionConfig;
import org.adam.zenithx.ui.components.MenuButton;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class WorldSettingsScreen extends HostBaseScreen {

    private static final int PW = 300;
    private static final int PH = 280;

    private static final int C_BG     = 0xFF1A1A1A;
    private static final int C_TOPBAR = 0xFF212121;
    private static final int C_BORDER = 0xFF3A3A3A;
    private static final int C_TEXT   = 0xFFD4D0C8;
    private static final int C_DIM    = 0xFF888888;
    private static final int C_ROW      = 0xFF1E1E1E;
    private static final int C_ROW_HOV  = 0xFF2A2A2A;
    private static final int C_ROWLINE  = 0xFF222222;
    private static final int C_TABSEL   = 0xFF252525;
    private static final int C_TABLINE  = 0xFF4A90D9;

    private enum Tab { SETTINGS, PACKS }
    private Tab tab = Tab.SETTINGS;

    private String gameMode   = "Survival";
    private String difficulty = "Normal";
    private boolean cheats    = false;
    private boolean shareRP   = false;

    private int maxPlayers          = 4;
    private boolean isDraggingSlider = false;

    private boolean gameModeOpen   = false;
    private boolean difficultyOpen = false;

    private static final String[] GAMEMODES    = {"Survival", "Creative", "Adventure", "Spectator"};
    private static final String[] DIFFICULTIES = {"Peaceful", "Easy", "Normal", "Hard"};

    // Resource packs available locally, and the subset the host has chosen
    // to actually share with guests. Populated from the pack repository in
    // init() rather than guessed at, since the ids/names only exist there.
    private final List<Pack> availablePacks = new ArrayList<>();
    private final Set<String> selectedPackIds = new LinkedHashSet<>();
    private int packsScrollY = 0;
    private static final int PACK_ROW_H = 22;

    private MenuButton tabSettingsBtn;
    private MenuButton tabPacksBtn;

    private final LevelSummary world;
    private final HostBaseScreen parent;

    public WorldSettingsScreen(LevelSummary world, HostBaseScreen parent) {
        super(Component.literal("Configure world settings"));
        this.world  = world;
        this.parent = parent;
    }

    private int px() { return (width  - PW) / 2; }
    private int py() { return (height - PH) / 2; }

    @Override
    protected void init() {
        int px = px(), py = py();

        loadAvailablePacks();

        int tabW = (PW - 6) / 2;
        int tabY = py + 28;

        tabSettingsBtn = new MenuButton(
                px + 2, tabY, tabW, 18,
                Component.literal("Settings"),
                new MenuButton.Style(0xFFFFFFFF, C_TABSEL, C_TABLINE),
                new MenuButton.Style(0xFFFFFFFF, 0xFF303030, C_BORDER),
                MenuButton.DISABLED, false,
                btn -> setTab(Tab.SETTINGS)
        );
        addRenderableWidget(tabSettingsBtn);

        tabPacksBtn = new MenuButton(
                px + 3 + tabW, tabY, tabW, 18,
                Component.literal("Packs"),
                new MenuButton.Style(C_TEXT, 0xFF252525, C_BORDER),
                new MenuButton.Style(0xFFFFFFFF, 0xFF303030, C_BORDER),
                MenuButton.DISABLED, false,
                btn -> setTab(Tab.PACKS)
        );
        addRenderableWidget(tabPacksBtn);

        addRenderableWidget(new MenuButton(
                px + 8, py + PH - 24, (PW - 20) / 2, 18,
                Component.literal("Back"),
                btn -> {
                    if (world != null)
                        minecraft.setScreen(new HostWorldScreen());
                    else if (parent != null)
                        minecraft.setScreen(parent);
                    else
                        onClose();
                }
        ));

        addRenderableWidget(new MenuButton(
                px + 12 + (PW - 20) / 2, py + PH - 24, (PW - 20) / 2, 18,
                Component.literal("Next"),
                MenuButton.BLUE, MenuButton.LIGHT_BLUE, MenuButton.DISABLED, true,
                btn -> {
                    GameType parsedGameMode = parseGameMode(gameMode);
                    Difficulty parsedDifficulty = parseDifficulty(difficulty);

                    HostSessionConfig.setGameMode(parsedGameMode);
                    HostSessionConfig.setDifficulty(parsedDifficulty);
                    HostSessionConfig.setCheats(cheats);
                    HostSessionConfig.setShareRP(shareRP);
                    HostSessionConfig.setMaxPlayers(maxPlayers);
                    HostSessionConfig.markPendingApply();

                    if (shareRP) {
                        HostSessionConfig.setActiveResourcePacks(new ArrayList<>(selectedPackIds));
                    }

                    minecraft.setScreen(new FriendsScreen(this, world));
                }
        ));

        updateTabStyles();
    }

    private void setTab(Tab t) {
        this.tab = t;
        updateTabStyles();
    }

    private void updateTabStyles() {
        if (tabSettingsBtn == null) return;
        tabSettingsBtn.setDefaultStyle(tab == Tab.SETTINGS
                ? new MenuButton.Style(0xFFFFFFFF, C_TABSEL, C_TABLINE)
                : new MenuButton.Style(C_TEXT, 0xFF252525, C_BORDER));
        tabPacksBtn.setDefaultStyle(tab == Tab.PACKS
                ? new MenuButton.Style(0xFFFFFFFF, C_TABSEL, C_TABLINE)
                : new MenuButton.Style(C_TEXT, 0xFF252525, C_BORDER));
    }

    /**
     * Pulls the packs the host currently has available (minus vanilla/fabric
     * internals) from the real repository - never guessed or hardcoded.
     * Packs already active get pre-checked so turning "Share RP" on without
     * visiting this tab still behaves like it used to (share what's active).
     */
    private void loadAvailablePacks() {
        availablePacks.clear();
        if (minecraft == null || minecraft.getResourcePackRepository() == null) return;

        for (Pack pack : minecraft.getResourcePackRepository().getAvailablePacks()) {
            // Fabric automatically registers every loaded mod as its own
            // resource pack source (so mods can ship textures/sounds) - those
            // show up here with ids like "fabric-api", "zenithx", etc. Real,
            // user-installed resource packs (from the resourcepacks folder or
            // a .zip there) always use the "file/..." id convention, so
            // filtering to that excludes every mod-provided pack in one go
            // instead of trying to exclude mods by name one at a time.
            if (!pack.getId().startsWith("file/")) continue;
            availablePacks.add(pack);
        }

        if (selectedPackIds.isEmpty()) {
            for (Pack pack : minecraft.getResourcePackRepository().getSelectedPacks()) {
                if (!pack.getId().startsWith("file/")) continue;
                selectedPackIds.add(pack.getId());
            }
        }
    }

    //? if >=26 {
    private void cText(GuiGraphicsExtractor g, Component text, int x, int y, int color) {
        g.centeredText(font, text, x, y, color);
    }
    private void dText(GuiGraphicsExtractor g, Component text, int x, int y, int color) {
        g.text(font, text, x, y, color, false);
    }
    private void outline(GuiGraphicsExtractor g, int x, int y, int w, int h, int c) {
        g.outline(x, y, w, h, c);
    }
    //? } else {
    /*private void cText(GuiGraphics g, Component text, int x, int y, int color) {
        g.drawCenteredString(font, text, x, y, color);
    }
    private void dText(GuiGraphics g, Component text, int x, int y, int color) {
        g.drawString(font, text, x, y, color, false);
    }
    private void outline(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x,         y,         x + w, y + 1, c);
        g.fill(x,         y + h - 1, x + w, y + h, c);
        g.fill(x,         y,         x + 1, y + h, c);
        g.fill(x + w - 1, y,         x + w, y + h, c);
    }*/
    //? }

    //? if >=26 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mx, int my, float a) {
    //? } else {
    /*@Override
    public void render(GuiGraphics gfx, int mx, int my, float a) {*/
    //? }
        int px = px(), py = py();

        if (isDraggingSlider) {
            updateMaxPlayers(mx, px + PW - 108, 100);
        }

        gfx.fill(px + 4, py + 4, px + PW + 4, py + PH + 4, 0x55000000);
        gfx.fill(px, py, px + PW, py + PH, C_BG);
        outline(gfx, px, py, PW, PH, C_BORDER);

        gfx.fill(px, py, px + PW, py + 26, C_TOPBAR);
        hline(gfx, px, py + 26, PW, C_BORDER);
        cText(gfx, Component.literal("Configure world settings"),
                px + PW / 2, py + 9, C_TEXT);

        int contentTop = py + 50;

        if (tab == Tab.SETTINGS) {
            dText(gfx, Component.literal("Configure a few basic world settings"),
                    px + 12, contentTop, C_DIM);
            dText(gfx, Component.literal("to get started."),
                    px + 12, contentTop + 10, C_DIM);

            int rowY = contentTop + 18;
            int rowH = 22;

            drawSettingRow(gfx, px, rowY, "Game Mode", gameMode, gameModeOpen, mx, my);
            rowY += rowH + 6;
            drawSettingRow(gfx, px, rowY, "Difficulty", difficulty, difficultyOpen, mx, my);
            rowY += rowH + 6;
            drawToggleRow(gfx, px, rowY, "Cheats", cheats);
            rowY += rowH + 6;
            drawToggleRow(gfx, px, rowY, "Share RP", shareRP);
            rowY += rowH + 6;
            drawSliderRow(gfx, px, rowY, "Max Players", maxPlayers, 2, 10);

            if (gameModeOpen)   drawDropdown(gfx, px, contentTop + 18 + 22,              GAMEMODES,    gameMode,   mx, my);
            if (difficultyOpen) drawDropdown(gfx, px, contentTop + 18 + (22 + 6) + 22, DIFFICULTIES, difficulty, mx, my);
        } else {
            int listTop = contentTop;
            int listBot = py + PH - 28;

            if (!shareRP) {
                dText(gfx, Component.literal("Turn on \"Share RP\" in Settings"), px + 12, listTop, C_DIM);
                dText(gfx, Component.literal("to let guests pick from these."), px + 12, listTop + 10, C_DIM);
                listTop += 24;
            }

            if (availablePacks.isEmpty()) {
                dText(gfx, Component.literal("No resource packs installed."), px + 12, listTop, C_DIM);
            } else {
                drawPackRows(gfx, px, listTop, listBot, mx, my);
                drawScrollbar(gfx, px + PW - 4, listTop, listBot);
            }
        }

        hline(gfx, px, py + PH - 28, PW, C_BORDER);
        gfx.fill(px + 1, py + PH - 27, px + PW - 1, py + PH - 1, 0xFF1A1E2E);

        //? if >=26 {
        super.extractRenderState(gfx, mx, my, a);
        //? } else {
        /*super.render(gfx, mx, my, a);*/
        //? }
    }

    //? if >=26 {
    private void drawPackRows(GuiGraphicsExtractor gfx, int px, int listTop, int listBot, int mx, int my) {
    //? } else {
    /*private void drawPackRows(GuiGraphics gfx, int px, int listTop, int listBot, int mx, int my) {*/
    //? }
        gfx.enableScissor(px + 1, listTop, px + PW - 1, listBot);

        int y = listTop - packsScrollY;
        for (Pack pack : availablePacks) {
            if (y + PACK_ROW_H < listTop) { y += PACK_ROW_H; continue; }
            if (y > listBot) break;

            boolean hov = mx >= px + 1 && mx < px + PW - 5 && my >= y && my < y + PACK_ROW_H;
            boolean selected = selectedPackIds.contains(pack.getId());

            gfx.fill(px + 1, y, px + PW - 5, y + PACK_ROW_H, hov ? C_ROW_HOV : C_ROW);
            hline(gfx, px + 1, y + PACK_ROW_H - 1, PW - 7, C_ROWLINE);

            int boxSize = 14;
            int boxX = px + 8, boxY = y + (PACK_ROW_H - boxSize) / 2;
            gfx.fill(boxX, boxY, boxX + boxSize, boxY + boxSize, selected ? 0xFF1A6ACA : 0xFF252525);
            outline(gfx, boxX, boxY, boxSize, boxSize, selected ? 0xFF4A90D9 : 0xFF555555);
            if (selected) {
                dText(gfx, Component.literal("\u2713"), boxX + 3, boxY + 3, 0xFFFFFFFF);
            }

            dText(gfx, pack.getTitle(), px + 8 + boxSize + 8, y + (PACK_ROW_H - 7) / 2, C_TEXT);

            y += PACK_ROW_H;
        }

        gfx.disableScissor();
    }

    //? if >=26 {
    private void drawScrollbar(GuiGraphicsExtractor g, int x, int top, int bot) {
    //? } else {
    /*private void drawScrollbar(GuiGraphics g, int x, int top, int bot) {*/
    //? }
        int h = bot - top;
        int total = availablePacks.size() * PACK_ROW_H;
        g.fill(x, top, x + 3, bot, 0xFF181818);
        if (total <= h) return;
        int th = Math.max(18, h * h / total);
        int ty = top + (int) ((long) packsScrollY * (h - th) / Math.max(1, total - h));
        g.fill(x, ty, x + 3, ty + th, 0xFF484848);
    }

    //? if >=26 {
    private void drawSettingRow(GuiGraphicsExtractor gfx, int px, int y,
    //? } else {
    /*private void drawSettingRow(GuiGraphics gfx, int px, int y,*/
    //? }
                                String label, String value, boolean open,
                                int mx, int my) {
        int rowH = 22;
        dText(gfx, Component.literal(label), px + 12, y + (rowH - 7) / 2, C_TEXT);

        int bx = px + PW - 108, bw = 100;
        boolean hov = mx >= bx && mx < bx + bw && my >= y && my < y + rowH;
        int bg = open ? 0xFF2A3A5A : (hov ? 0xFF303030 : 0xFF252525);

        gfx.fill(bx, y, bx + bw, y + rowH, bg);
        outline(gfx, bx, y, bw, rowH, open ? 0xFF4A90D9 : 0xFF3A3A3A);
        dText(gfx, Component.literal(value), bx + 6, y + (rowH - 7) / 2, C_TEXT);

        int ax = bx + bw - 14, ay = y + rowH / 2 - 2;
        gfx.fill(ax,     ay,     ax + 7, ay + 1, C_DIM);
        gfx.fill(ax + 1, ay + 1, ax + 6, ay + 2, C_DIM);
        gfx.fill(ax + 2, ay + 2, ax + 5, ay + 3, C_DIM);
        gfx.fill(ax + 3, ay + 3, ax + 4, ay + 4, C_DIM);
    }

    //? if >=26 {
    private void drawToggleRow(GuiGraphicsExtractor gfx, int px, int y,
    //? } else {
    /*private void drawToggleRow(GuiGraphics gfx, int px, int y,*/
    //? }
                               String label, boolean value) {
        int rowH = 22;
        dText(gfx, Component.literal(label), px + 12, y + (rowH - 7) / 2, C_TEXT);

        int tw = 32, th = 14;
        int tx = px + PW - 50, ty = y + (rowH - th) / 2;

        gfx.fill(tx, ty, tx + tw, ty + th, value ? 0xFF1A6ACA : 0xFF3A3A3A);
        outline(gfx, tx, ty, tw, th, value ? 0xFF4A90D9 : 0xFF555555);

        int thumbX = value ? tx + tw - th : tx;
        gfx.fill(thumbX, ty, thumbX + th, ty + th, 0xFFFFFFFF);
        outline(gfx, thumbX, ty, th, th, 0xFFAAAAAA);
    }

    //? if >=26 {
    private void drawSliderRow(GuiGraphicsExtractor gfx, int px, int y,
    //? } else {
    /*private void drawSliderRow(GuiGraphics gfx, int px, int y,*/
    //? }
                               String label, int value, int min, int max) {
        int rowH = 22;
        dText(gfx, Component.literal(label), px + 12, y + (rowH - 7) / 2, C_TEXT);

        int bx = px + PW - 108, bw = 100;
        int sh = 14;
        int sy = y + (rowH - sh) / 2;

        gfx.fill(bx, sy, bx + bw, sy + sh, 0xFF252525);
        outline(gfx, bx, sy, bw, sh, 0xFF3A3A3A);

        float ratio = (float) (value - min) / (max - min);
        int fillW = (int) (bw * ratio);
        if (fillW > 0) {
            gfx.fill(bx + 1, sy + 1, bx + fillW, sy + sh - 1, 0xFF1A6ACA);
        }

        int handleW = 8;
        int handleX = bx + (int) ((bw - handleW) * ratio);
        gfx.fill(handleX, sy - 1, handleX + handleW, sy + sh + 1, 0xFFFFFFFF);
        outline(gfx, handleX, sy - 1, handleW, sh + 2, 0xFF4A90D9);

        String valText = String.valueOf(value);
        int txtW = font.width(valText);
        dText(gfx, Component.literal(valText), bx - txtW - 8, y + (rowH - 7) / 2, C_TEXT);
    }

    private void updateMaxPlayers(int mx, int bx, int bw) {
        float pct = (float) (mx - bx) / bw;
        pct = Math.max(0.0f, Math.min(1.0f, pct));
        this.maxPlayers = Math.round(2 + pct * (10 - 2));
    }

    //? if >=26 {
    private void drawDropdown(GuiGraphicsExtractor gfx, int px, int y, int w,
    //? } else {
    /*private void drawDropdown(GuiGraphics gfx, int px, int y, int w,*/
    //? }
                              String[] options, String selected, int mx, int my) {
        int bx = px + PW - 108;
        int itemH = 16;
        int totalH = options.length * itemH;

        gfx.fill(bx, y, bx + w, y + totalH, 0xFF1A1A1A);
        outline(gfx, bx, y, w, totalH, 0xFF4A90D9);

        for (int i = 0; i < options.length; i++) {
            int iy = y + i * itemH;
            boolean hov = mx >= bx && mx < bx + w && my >= iy && my < iy + itemH;
            boolean sel = options[i].equals(selected);
            if (sel)      gfx.fill(bx + 1, iy, bx + w - 1, iy + itemH, 0xFF1E3050);
            else if (hov) gfx.fill(bx + 1, iy, bx + w - 1, iy + itemH, 0xFF2A2A2A);
            dText(gfx, Component.literal(options[i]),
                    bx + 6, iy + (itemH - 7) / 2, sel ? 0xFFFFFFFF : C_TEXT);
            if (i < options.length - 1)
                gfx.fill(bx + 1, iy + itemH - 1, bx + w - 1, iy + itemH, 0xFF2A2A2A);
        }
    }

    //? if >=26 {
    private void drawDropdown(GuiGraphicsExtractor gfx, int px, int y,
    //? } else {
    /*private void drawDropdown(GuiGraphics gfx, int px, int y,*/
    //? }
                              String[] options, String selected, int mx, int my) {
        drawDropdown(gfx, px, y, 100, options, selected, mx, my);
    }

    //? if >=26 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean dc) {
        if (event.button() != 0) return super.mouseClicked(event, dc);

        int mx = (int) event.x();
        int my = (int) event.y();
    //? } else {
    /*@Override
    public boolean mouseClicked(double mxd, double myd, int button) {
        if (button != 0) return super.mouseClicked(mxd, myd, button);

        int mx = (int) mxd;
        int my = (int) myd;*/
    //? }
        int px = px(), py = py();
        int rowH = 22;
        int itemH = 16;
        int bx = px + PW - 108;
        int bw = 100;
        int contentTop = py + 50;

        if (tab == Tab.PACKS) {
            int listTop = contentTop + (shareRP ? 0 : 24);
            int listBot = py + PH - 28;
            if (mx >= px + 1 && mx < px + PW - 5 && my >= listTop && my < listBot) {
                int y = listTop - packsScrollY;
                for (Pack pack : availablePacks) {
                    if (my >= y && my < y + PACK_ROW_H) {
                        if (selectedPackIds.contains(pack.getId())) selectedPackIds.remove(pack.getId());
                        else selectedPackIds.add(pack.getId());
                        return true;
                    }
                    y += PACK_ROW_H;
                }
            }
            //? if >=26 {
            return super.mouseClicked(event, dc);
            //? } else {
            /*return super.mouseClicked(mxd, myd, button);*/
            //? }
        }

        if (gameModeOpen) {
            int dropY = contentTop + 18 + rowH;
            for (int i = 0; i < GAMEMODES.length; i++) {
                int iy = dropY + i * itemH;
                if (mx >= bx && mx < bx + bw && my >= iy && my < iy + itemH) {
                    gameMode = GAMEMODES[i];
                    gameModeOpen = false;
                    return true;
                }
            }
            gameModeOpen = false;
            return true;
        }

        if (difficultyOpen) {
            int dropY = contentTop + 18 + (rowH + 6) + rowH;
            for (int i = 0; i < DIFFICULTIES.length; i++) {
                int iy = dropY + i * itemH;
                if (mx >= bx && mx < bx + bw && my >= iy && my < iy + itemH) {
                    difficulty = DIFFICULTIES[i];
                    difficultyOpen = false;
                    return true;
                }
            }
            difficultyOpen = false;
            return true;
        }

        if (mx >= bx && mx < bx + bw) {
            int gmY = contentTop + 18;
            int dfY = contentTop + 18 + (rowH + 6);
            if (my >= gmY && my < gmY + rowH) { gameModeOpen = true; return true; }
            if (my >= dfY && my < dfY + rowH) { difficultyOpen = true; return true; }
        }

        int tw = 32, th = 14;
        int tx = px + PW - 50;
        int cheatsY  = contentTop + 18 + (rowH + 6) * 2;
        int shareRPY = contentTop + 18 + (rowH + 6) * 3;
        int maxPlayersY = contentTop + 18 + (rowH + 6) * 4;

        if (mx >= tx && mx < tx + tw) {
            if (my >= cheatsY + (rowH - th) / 2 && my < cheatsY + (rowH - th) / 2 + th) {
                cheats = !cheats;
                return true;
            }
            if (my >= shareRPY + (rowH - th) / 2 && my < shareRPY + (rowH - th) / 2 + th) {
                shareRP = !shareRP;
                return true;
            }
        }

        if (mx >= bx && mx < bx + bw && my >= maxPlayersY && my < maxPlayersY + rowH) {
            updateMaxPlayers(mx, bx, bw);
            isDraggingSlider = true;
            return true;
        }

        //? if >=26 {
        return super.mouseClicked(event, dc);
        //? } else {
        /*return super.mouseClicked(mxd, myd, button);*/
        //? }
    }

    //? if >=26 {
    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            isDraggingSlider = false;
        }
        return super.mouseReleased(event);
    }
    //? } else {
    /*@Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (button == 0) {
            isDraggingSlider = false;
        }
        return super.mouseReleased(mx, my, button);
    }*/
    //? }

    //? if >=1.20.5 {
    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
    //? } else {
    /*@Override
    public boolean mouseScrolled(double mx, double my, double sy) {
        double sx = 0;*/
    //? }
        if (tab == Tab.PACKS) {
            int px = px(), py = py();
            int contentTop = py + 50;
            int listTop = contentTop + (shareRP ? 0 : 24);
            int listBot = py + PH - 28;
            if (mx >= px && mx <= px + PW && my >= listTop && my <= listBot) {
                int totalH = availablePacks.size() * PACK_ROW_H;
                int viewH = listBot - listTop;
                packsScrollY = (int) Math.max(0, Math.min(packsScrollY - sy * PACK_ROW_H, Math.max(0, totalH - viewH)));
                return true;
            }
        }
        //? if >=1.20.5 {
        return super.mouseScrolled(mx, my, sx, sy);
        //? } else {
        /*return super.mouseScrolled(mx, my, sy);*/
        //? }
    }

    private GameType parseGameMode(String s) {
        return switch (s) {
            case "Creative"  -> GameType.CREATIVE;
            case "Adventure" -> GameType.ADVENTURE;
            case "Spectator" -> GameType.SPECTATOR;
            default          -> GameType.SURVIVAL;
        };
    }

    private Difficulty parseDifficulty(String s) {
        return switch (s) {
            case "Peaceful" -> Difficulty.PEACEFUL;
            case "Easy"     -> Difficulty.EASY;
            case "Hard"     -> Difficulty.HARD;
            default         -> Difficulty.NORMAL;
        };
    }

    //? if >=26 {
    private void hline(GuiGraphicsExtractor g, int x, int y, int w, int c) {
    //? } else {
    /*private void hline(GuiGraphics g, int x, int y, int w, int c) {*/
    //? }
        if (w > 0) g.fill(x, y, x + w, y + 1, c);
    }

    @Override public boolean isPauseScreen() { return false; }
}
