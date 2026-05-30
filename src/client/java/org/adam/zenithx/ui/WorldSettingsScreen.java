package org.adam.zenithx.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelSummary;
import org.adam.zenithx.HostSessionConfig;
import org.adam.zenithx.ui.components.MenuButton;

public class WorldSettingsScreen extends HostBaseScreen {

    private static final int PW = 300;
    private static final int PH = 260;

    private static final int C_BG     = 0xFF1A1A1A;
    private static final int C_TOPBAR = 0xFF212121;
    private static final int C_BORDER = 0xFF3A3A3A;
    private static final int C_TEXT   = 0xFFD4D0C8;
    private static final int C_DIM    = 0xFF888888;

    private String gameMode  = "Survival";
    private String difficulty = "Normal";
    private boolean cheats   = false;
    private boolean shareRP  = false;

    private boolean gameModeOpen   = false;
    private boolean difficultyOpen = false;

    private static final String[] GAMEMODES    = {"Survival", "Creative", "Adventure", "Spectator"};
    private static final String[] DIFFICULTIES = {"Peaceful", "Easy", "Normal", "Hard"};

    // world = null إذا جاء من PauseScreen
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

        // Back
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

        // Next
        addRenderableWidget(new MenuButton(
                px + 12 + (PW - 20) / 2, py + PH - 24, (PW - 20) / 2, 18,
                Component.literal("Next"),
                MenuButton.BLUE, MenuButton.LIGHT_BLUE, MenuButton.DISABLED, true,
                btn -> {
                    HostSessionConfig.setGameMode(parseGameMode(gameMode));
                    HostSessionConfig.setDifficulty(parseDifficulty(difficulty));
                    HostSessionConfig.setCheats(cheats);
                    HostSessionConfig.setShareRP(shareRP);
                    minecraft.setScreen(new FriendsScreen(this, world));
                }
        ));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mx, int my, float a) {
        int px = px(), py = py();

        gfx.fill(px + 4, py + 4, px + PW + 4, py + PH + 4, 0x55000000);
        gfx.fill(px, py, px + PW, py + PH, C_BG);
        gfx.outline(px, py, PW, PH, C_BORDER);

        gfx.fill(px, py, px + PW, py + 26, C_TOPBAR);
        hline(gfx, px, py + 26, PW, C_BORDER);
        gfx.centeredText(font, Component.literal("Configure world settings"),
                px + PW / 2, py + 9, C_TEXT);

        gfx.text(font, Component.literal("Configure a few basic world settings"),
                px + 12, py + 34, C_DIM);
        gfx.text(font, Component.literal("to get started."),
                px + 12, py + 44, C_DIM);

        int rowY = py + 62;
        int rowH = 22;

        drawSettingRow(gfx, px, rowY, "Game Mode", gameMode, gameModeOpen, mx, my);
        rowY += rowH + 6;
        drawSettingRow(gfx, px, rowY, "Difficulty", difficulty, difficultyOpen, mx, my);
        rowY += rowH + 6;
        drawToggleRow(gfx, px, rowY, "Cheats", cheats);
        rowY += rowH + 6;
        drawToggleRow(gfx, px, rowY, "Share RP", shareRP);

        hline(gfx, px, py + PH - 28, PW, C_BORDER);
        gfx.fill(px + 1, py + PH - 27, px + PW - 1, py + PH - 1, 0xFF1A1E2E);

        super.extractRenderState(gfx, mx, my, a);

        // Dropdowns فوق كل شي
        if (gameModeOpen)   drawDropdown(gfx, px, py + 62 + 22,          GAMEMODES,    gameMode,   mx, my);
        if (difficultyOpen) drawDropdown(gfx, px, py + 62 + (22+6) + 22, DIFFICULTIES, difficulty, mx, my);
    }

    private void drawSettingRow(GuiGraphicsExtractor gfx, int px, int y,
                                String label, String value, boolean open,
                                int mx, int my) {
        int rowH = 22;
        gfx.text(font, Component.literal(label), px + 12, y + (rowH - 7) / 2, C_TEXT);

        int bx = px + PW - 108, bw = 100;
        boolean hov = mx >= bx && mx < bx + bw && my >= y && my < y + rowH;
        int bg = open ? 0xFF2A3A5A : (hov ? 0xFF303030 : 0xFF252525);

        gfx.fill(bx, y, bx + bw, y + rowH, bg);
        gfx.outline(bx, y, bw, rowH, open ? 0xFF4A90D9 : 0xFF3A3A3A);
        gfx.text(font, Component.literal(value), bx + 6, y + (rowH - 7) / 2, C_TEXT);

        // Arrow ▼
        int ax = bx + bw - 14, ay = y + rowH / 2 - 2;
        gfx.fill(ax,     ay,     ax + 7, ay + 1, C_DIM);
        gfx.fill(ax + 1, ay + 1, ax + 6, ay + 2, C_DIM);
        gfx.fill(ax + 2, ay + 2, ax + 5, ay + 3, C_DIM);
        gfx.fill(ax + 3, ay + 3, ax + 4, ay + 4, C_DIM);
    }

    private void drawToggleRow(GuiGraphicsExtractor gfx, int px, int y,
                               String label, boolean value) {
        int rowH = 22;
        gfx.text(font, Component.literal(label), px + 12, y + (rowH - 7) / 2, C_TEXT);

        int tw = 32, th = 14;
        int tx = px + PW - 50, ty = y + (rowH - th) / 2;

        gfx.fill(tx, ty, tx + tw, ty + th, value ? 0xFF1A6ACA : 0xFF3A3A3A);
        gfx.outline(tx, ty, tw, th, value ? 0xFF4A90D9 : 0xFF555555);

        int thumbX = value ? tx + tw - th : tx;
        gfx.fill(thumbX, ty, thumbX + th, ty + th, 0xFFFFFFFF);
        gfx.outline(thumbX, ty, th, th, 0xFFAAAAAA);
    }

    private void drawDropdown(GuiGraphicsExtractor gfx, int px, int y, int w,
                              String[] options, String selected, int mx, int my) {
        int bx = px + PW - 108;
        int itemH = 16;
        int totalH = options.length * itemH;

        gfx.fill(bx, y, bx + w, y + totalH, 0xFF1A1A1A);
        gfx.outline(bx, y, w, totalH, 0xFF4A90D9);

        for (int i = 0; i < options.length; i++) {
            int iy = y + i * itemH;
            boolean hov = mx >= bx && mx < bx + w && my >= iy && my < iy + itemH;
            boolean sel = options[i].equals(selected);
            if (sel)       gfx.fill(bx + 1, iy, bx + w - 1, iy + itemH, 0xFF1E3050);
            else if (hov)  gfx.fill(bx + 1, iy, bx + w - 1, iy + itemH, 0xFF2A2A2A);
            gfx.text(font, Component.literal(options[i]),
                    bx + 6, iy + (itemH - 7) / 2, sel ? 0xFFFFFFFF : C_TEXT);
            if (i < options.length - 1)
                gfx.fill(bx + 1, iy + itemH - 1, bx + w - 1, iy + itemH, 0xFF2A2A2A);
        }
    }

    // helper overload
    private void drawDropdown(GuiGraphicsExtractor gfx, int px, int y,
                              String[] options, String selected, int mx, int my) {
        drawDropdown(gfx, px, y, 100, options, selected, mx, my);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean dc) {
        if (event.button() != 0) return super.mouseClicked(event, dc);

        double mx = event.x(), my = event.y();
        int px = px(), py = py();
        int rowH = 22;
        int itemH = 16;
        int bx = px + PW - 108;

        // GameMode dropdown اختيار
        if (gameModeOpen) {
            int by = py + 62 + 22;
            for (int i = 0; i < GAMEMODES.length; i++) {
                int iy = by + i * itemH;
                if (mx >= bx && mx < bx + 100 && my >= iy && my < iy + itemH) {
                    gameMode = GAMEMODES[i];
                }
            }
            gameModeOpen = false;
            return true;
        }

        // Difficulty dropdown اختيار
        if (difficultyOpen) {
            int by = py + 62 + (rowH + 6) + 22;
            for (int i = 0; i < DIFFICULTIES.length; i++) {
                int iy = by + i * itemH;
                if (mx >= bx && mx < bx + 100 && my >= iy && my < iy + itemH) {
                    difficulty = DIFFICULTIES[i];
                }
            }
            difficultyOpen = false;
            return true;
        }

        // فتح GameMode dropdown
        int gmBy = py + 62;
        if (mx >= bx && mx < bx + 100 && my >= gmBy && my < gmBy + rowH) {
            gameModeOpen = true;
            return true;
        }

        // فتح Difficulty dropdown
        int dfBy = py + 62 + rowH + 6;
        if (mx >= bx && mx < bx + 100 && my >= dfBy && my < dfBy + rowH) {
            difficultyOpen = true;
            return true;
        }

        // Cheats toggle
        int chY = py + 62 + (rowH + 6) * 2;
        int tw = 32, th = 14;
        int tx = px + PW - 50;
        if (mx >= tx && mx < tx + tw && my >= chY + (rowH-th)/2 && my < chY + (rowH-th)/2 + th) {
            cheats = !cheats;
            return true;
        }

        // Share RP toggle
        int rpY = py + 62 + (rowH + 6) * 3;
        if (mx >= tx && mx < tx + tw && my >= rpY + (rowH-th)/2 && my < rpY + (rowH-th)/2 + th) {
            shareRP = !shareRP;
            return true;
        }

        return super.mouseClicked(event, dc);
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

    private void hline(GuiGraphicsExtractor g, int x, int y, int w, int c) {
        if (w > 0) g.fill(x, y, x + w, y + 1, c);
    }

    @Override public boolean isPauseScreen() { return false; }
}