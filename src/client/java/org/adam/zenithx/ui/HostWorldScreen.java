package org.adam.zenithx.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.storage.LevelSummary;
import org.adam.zenithx.ui.components.MenuButton;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HostWorldScreen extends HostBaseScreen {

    private static final int PW = 300;
    private static final int PH = 310;
    private static final int RH = 36;

    private static final int C_BG      = 0xFF1A1A1A;
    private static final int C_TOPBAR  = 0xFF212121;
    private static final int C_BORDER  = 0xFF3A3A3A;
    private static final int C_TEXT    = 0xFFD4D0C8;
    private static final int C_DIM     = 0xFF888888;
    private static final int C_ROW     = 0xFF1E1E1E;
    private static final int C_ROW_HOV = 0xFF2A2A2A;
    private static final int C_SEL     = 0xFF1E3050;
    private static final int C_ROWLINE = 0xFF222222;
    private static final int C_SRCHBG  = 0xFF111111;
    private static final int C_SECT    = 0xFF484848;

    private EditBox searchBox;
    private String query = "";
    private int scrollY = 0;
    private int selectedIndex = -1;
    private List<LevelSummary> worlds = new ArrayList<>();
    private List<LevelSummary> filtered = new ArrayList<>();

    public HostWorldScreen() {
        super(Component.literal("Select world to host"));
    }

    private int px() { return (width - PW) / 2; }
    private int py() { return (height - PH) / 2; }

    @Override
    protected void init() {
        int px = px(), py = py();

        searchBox = new EditBox(font,
                px + 20, py + 31, PW - 32, 14,
                Component.literal("search"));
        searchBox.setHint(Component.literal("Search..."));
        searchBox.setBordered(false);
        searchBox.setMaxLength(32);
        searchBox.setResponder(q -> {
            query = q;
            scrollY = 0;
            selectedIndex = -1;
            filterWorlds();
        });
        addRenderableWidget(searchBox);

        // Create New World
        addRenderableWidget(new MenuButton(
                px + 8, py + 52, PW - 16, 20,
                Component.literal("Create New World"),
                MenuButton.BLUE, MenuButton.LIGHT_BLUE, MenuButton.DISABLED, true,
                btn -> openCreateWorld()
        ));

        // Cancel
        addRenderableWidget(new MenuButton(
                px + 8, py + PH - 24, (PW - 20) / 2, 18,
                Component.literal("Cancel"),
                btn -> onClose()
        ));

        // Next
        addRenderableWidget(new MenuButton(
                px + 12 + (PW - 20) / 2, py + PH - 24, (PW - 20) / 2, 18,
                Component.literal("Next"),
                MenuButton.BLUE, MenuButton.LIGHT_BLUE, MenuButton.DISABLED, true,
                btn -> {
                    if (selectedIndex >= 0 && selectedIndex < filtered.size()) {
                        minecraft.setScreen(new WorldSettingsScreen(
                                filtered.get(selectedIndex), this));
                    }
                }
        ));

        loadWorlds();
    }

    private void openCreateWorld() {
        try {
            String name = "World_" + System.currentTimeMillis();

            net.minecraft.world.level.LevelSettings settings =
                    new net.minecraft.world.level.LevelSettings(
                            name,
                            net.minecraft.world.level.GameType.SURVIVAL,
                            net.minecraft.world.level.LevelSettings.DifficultySettings.DEFAULT,
                            false,
                            minecraft.getConnection() != null
                                    ? new net.minecraft.world.level.WorldDataConfiguration(
                                    net.minecraft.world.level.DataPackConfig.DEFAULT,
                                    minecraft.getConnection().enabledFeatures())
                                    : net.minecraft.world.level.WorldDataConfiguration.DEFAULT
                    );

            net.minecraft.world.level.levelgen.WorldOptions options =
                    net.minecraft.world.level.levelgen.WorldOptions.defaultWithRandomSeed();

            minecraft.createWorldOpenFlows().createFreshLevel(
                    name, settings, options,
                    holder -> holder.lookupOrThrow(
                                    net.minecraft.core.registries.Registries.WORLD_PRESET)
                            .getOrThrow(
                                    net.minecraft.world.level.levelgen.presets.WorldPresets.NORMAL)
                            .value().createWorldDimensions(),
                    this
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadWorlds() {
        try {
            worlds = minecraft.getLevelSource()
                    .loadLevelSummaries(minecraft.getLevelSource().findLevelCandidates())
                    .join();
        } catch (Exception e) {
            worlds = new ArrayList<>();
        }
        filterWorlds();
    }

    private void filterWorlds() {
        String q = query.toLowerCase();
        filtered = worlds.stream()
                .filter(w -> q.isEmpty() || w.getLevelName().toLowerCase().contains(q))
                .collect(Collectors.toList());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mx, int my, float a) {
        int px = px(), py = py();

        gfx.fill(px + 4, py + 4, px + PW + 4, py + PH + 4, 0x55000000);
        gfx.fill(px, py, px + PW, py + PH, C_BG);
        gfx.outline(px, py, PW, PH, C_BORDER);

        gfx.fill(px, py, px + PW, py + 26, C_TOPBAR);
        hline(gfx, px, py + 26, PW, C_BORDER);
        gfx.centeredText(font, Component.literal("Select world to host"),
                px + PW / 2, py + 9, C_TEXT);

        gfx.fill(px + 8, py + 27, px + PW - 8, py + 49, C_SRCHBG);
        gfx.outline(px + 8, py + 27, PW - 16, 22, C_SECT);
        drawSearchIcon(gfx, px + 10, py + 30);

        int listTop = py + 76, listBot = py + PH - 28;
        drawWorldList(gfx, px, listTop, listBot, mx, my);
        drawScrollbar(gfx, px + PW - 4, listTop, listBot);

        hline(gfx, px, py + PH - 28, PW, C_BORDER);
        gfx.fill(px + 1, py + PH - 27, px + PW - 1, py + PH - 1, 0xFF1A1E2E);

        super.extractRenderState(gfx, mx, my, a);
    }

    private void drawWorldList(GuiGraphicsExtractor gfx,
                               int px, int listTop, int listBot, int mx, int my) {
        gfx.enableScissor(px + 1, listTop, px + PW - 1, listBot);

        int y = listTop - scrollY;
        for (int i = 0; i < filtered.size(); i++) {
            LevelSummary w = filtered.get(i);
            if (y + RH < listTop) { y += RH; continue; }
            if (y > listBot) break;

            boolean hov = mx >= px + 1 && mx < px + PW - 5 && my >= y && my < y + RH;
            boolean sel = i == selectedIndex;

            gfx.fill(px + 1, y, px + PW - 5, y + RH, sel ? C_SEL : (hov ? C_ROW_HOV : C_ROW));
            if (sel) gfx.fill(px + 1, y, px + 3, y + RH, 0xFF4A90D9);
            hline(gfx, px + 1, y + RH - 1, PW - 7, C_ROWLINE);

            gfx.fill(px + 6, y + 4, px + 34, y + RH - 4, 0xFF2A3A2A);
            gfx.outline(px + 6, y + 4, 28, RH - 8, 0xFF404040);

            gfx.text(font, Component.literal(w.getLevelName()), px + 40, y + 6, C_TEXT);
            gfx.text(font, Component.literal(w.getLevelId()), px + 40, y + 17, C_DIM);

            y += RH;
        }

        if (filtered.isEmpty()) {
            gfx.centeredText(font, Component.literal("No worlds found"),
                    px + PW / 2, listTop + (listBot - listTop) / 2, C_DIM);
        }

        gfx.disableScissor();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        double mx = event.x(), my = event.y();
        int px = px(), py = py();
        int listTop = py + 76, listBot = py + PH - 28;

        if (mx >= px + 1 && mx < px + PW - 5 && my >= listTop && my < listBot) {
            int y = listTop - scrollY;
            for (int i = 0; i < filtered.size(); i++) {
                if (my >= y && my < y + RH) {
                    selectedIndex = i;
                    if (doubleClick) {
                        minecraft.setScreen(new WorldSettingsScreen(filtered.get(i), this));
                    }
                    return true;
                }
                y += RH;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int listTop = py() + 76, listBot = py() + PH - 28;
        if (mx < px() || mx > px() + PW || my < listTop || my > listBot)
            return super.mouseScrolled(mx, my, sx, sy);
        int totalH = filtered.size() * RH;
        int viewH = listBot - listTop;
        scrollY = (int) Math.max(0, Math.min(scrollY - sy * RH, Math.max(0, totalH - viewH)));
        return true;
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int x, int top, int bot) {
        int h = bot - top;
        int total = filtered.size() * RH;
        g.fill(x, top, x + 3, bot, 0xFF181818);
        if (total <= h) return;
        int th = Math.max(18, h * h / total);
        int ty = top + (int) ((long) scrollY * (h - th) / Math.max(1, total - h));
        g.fill(x, ty, x + 3, ty + th, 0xFF484848);
    }

    private void drawSearchIcon(GuiGraphicsExtractor g, int x, int y) {
        int ic = 0xFF888888;
        g.fill(x + 2, y,     x + 6, y + 1, ic);
        g.fill(x + 2, y + 6, x + 6, y + 7, ic);
        g.fill(x,     y + 2, x + 1, y + 6, ic);
        g.fill(x + 6, y + 2, x + 7, y + 6, ic);
        g.fill(x + 1, y + 1, x + 2, y + 2, ic);
        g.fill(x + 6, y + 1, x + 7, y + 2, ic);
        g.fill(x + 1, y + 6, x + 2, y + 7, ic);
        g.fill(x + 6, y + 6, x + 7, y + 7, ic);
        g.fill(x + 7, y + 7, x + 8, y + 8, ic);
        g.fill(x + 8, y + 8, x + 10, y + 10, ic);
    }

    private void hline(GuiGraphicsExtractor g, int x, int y, int w, int c) {
        if (w > 0) g.fill(x, y, x + w, y + 1, c);
    }

    @Override public boolean isPauseScreen() { return false; }
}