package org.adam.zenithx.ui;

//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? } else {
/*import net.minecraft.client.gui.GuiGraphics;*/
//? }
//? if >=1.21.9 {
import net.minecraft.client.input.MouseButtonEvent;
//? }
import net.minecraft.network.chat.Component;
import org.adam.zenithx.compat.CompatibilityResult;
import org.adam.zenithx.ui.components.MenuButton;

import java.util.ArrayList;
import java.util.List;

/**
 * Shows the result of a {@link org.adam.zenithx.compat.CompatibilityChecker}
 * run before a guest actually connects into a hosted world: which
 * environment fields and mods matched, which didn't, and why. Follows the
 * same panel layout and drawing helpers as {@link FriendsScreen} /
 * {@link HostWorldScreen} for a consistent look.
 */
public class CompatibilityScreen extends HostBaseScreen {

    private static final int PW = 280;
    private static final int PH = 300;
    private static final int RH = 22;

    private static final int C_BG      = 0xFF1A1A1A;
    private static final int C_TOPBAR  = 0xFF212121;
    private static final int C_BORDER  = 0xFF3A3A3A;
    private static final int C_TEXT    = 0xFFD4D0C8;
    private static final int C_DIM     = 0xFF888888;
    private static final int C_ROW     = 0xFF1E1E1E;
    private static final int C_ROW_HOV = 0xFF2A2A2A;
    private static final int C_ROWLINE = 0xFF222222;
    private static final int C_SECT    = 0xFF484848;

    private static final int C_OK    = 0xFF55AA55;
    private static final int C_WARN  = 0xFFDDAA33;
    private static final int C_BAD   = 0xFFCC5555;

    private final CompatibilityResult result;
    private final Runnable onContinue;
    private final Runnable onCancel;

    private int scrollY = 0;
    private Row selectedRow = null;

    private enum RowKind { SECTION, FIELD, MOD }

    private record Row(RowKind kind, String label, String expected, String actual,
                        CompatibilityResult.Severity severity, String reason) {}

    public CompatibilityScreen(CompatibilityResult result, Runnable onContinue, Runnable onCancel) {
        super(Component.literal("World Compatibility"));
        this.result = result;
        this.onContinue = onContinue;
        this.onCancel = onCancel;
    }

    private int px() { return (width - PW) / 2; }
    private int py() { return (height - PH) / 2; }

    private static int colorFor(CompatibilityResult.Severity s) {
        return switch (s) {
            case COMPATIBLE -> C_OK;
            case WARNING -> C_WARN;
            case INCOMPATIBLE -> C_BAD;
        };
    }

    private static String iconFor(CompatibilityResult.Severity s) {
        return switch (s) {
            case COMPATIBLE -> "\u2713"; // check
            case WARNING -> "\u26A0";    // warning triangle
            case INCOMPATIBLE -> "\u2717"; // cross
        };
    }

    private List<Row> buildRows() {
        List<Row> rows = new ArrayList<>();
        rows.add(new Row(RowKind.SECTION, "Environment", null, null, null, null));

        rows.add(fieldRow("Minecraft Version", result.minecraftVersion()));
        rows.add(fieldRow("Fabric Loader", result.loaderVersion()));
        rows.add(fieldRow("ZenithX Version", result.zenithxVersion()));

        rows.add(new Row(RowKind.SECTION, "Mods (" + result.problemCount() + " problem"
                + (result.problemCount() == 1 ? "" : "s") + ")", null, null, null, null));

        for (CompatibilityResult.ModCheckEntry m : result.mods()) {
            if (m.status() == CompatibilityResult.ModStatus.EXTRA) continue;
            rows.add(new Row(RowKind.MOD, m.displayName(), m.requiredVersion(),
                    m.installedVersion(), m.severity(), m.reason()));
        }

        return rows;
    }

    private Row fieldRow(String label, CompatibilityResult.FieldCheck check) {
        return new Row(RowKind.FIELD, label, check.expected(), check.actual(),
                check.matches() ? CompatibilityResult.Severity.COMPATIBLE : CompatibilityResult.Severity.INCOMPATIBLE,
                check.matches() ? "Matches" : "Mismatch");
    }

    @Override
    protected void init() {
        int px = px(), py = py();

        addRenderableWidget(new MenuButton(
                px + 8, py + PH - 24, (PW - 20) / 2, 18,
                Component.literal("Cancel"),
                btn -> {
                    if (onCancel != null) onCancel.run();
                    if (minecraft != null) minecraft.setScreen(null);
                }
        ));

        MenuButton.Style continueStyle = result.blocksJoin() ? MenuButton.DISABLED : MenuButton.BLUE;
        MenuButton.Style continueHover = result.blocksJoin() ? MenuButton.DISABLED : MenuButton.LIGHT_BLUE;

        MenuButton continueBtn = new MenuButton(
                px + 12 + (PW - 20) / 2, py + PH - 24, (PW - 20) / 2, 18,
                Component.literal(result.blocksJoin() ? "Incompatible" : "Continue"),
                continueStyle, continueHover, MenuButton.DISABLED, true,
                btn -> {
                    if (result.blocksJoin()) return;
                    if (onContinue != null) onContinue.run();
                }
        );
        continueBtn.active = !result.blocksJoin();
        addRenderableWidget(continueBtn);
    }

    //? if >=26 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mx, int my, float a) {
    //? } else {
    /*@Override
    public void render(GuiGraphics gfx, int mx, int my, float a) {*/
    //? }
        int px = px(), py = py();

        gfx.fill(0, 0, width, height, 0x88000000);
        gfx.fill(px + 4, py + 4, px + PW + 4, py + PH + 4, 0x55000000);
        gfx.fill(px, py, px + PW, py + PH, C_BG);
        outline(gfx, px, py, PW, PH, colorFor(result.overall()));

        gfx.fill(px, py, px + PW, py + 26, C_TOPBAR);
        hline(gfx, px, py + 26, PW, C_BORDER);
        cText(gfx, Component.literal("World Compatibility"), px + PW / 2, py + 9, C_TEXT);

        String overallText = switch (result.overall()) {
            case COMPATIBLE -> "Compatible";
            case WARNING -> "Compatible (with warnings)";
            case INCOMPATIBLE -> "Incompatible";
        };
        cText(gfx, Component.literal(overallText), px + PW / 2, py + 34, colorFor(result.overall()));

        int listTop = py + 50, listBot = py + PH - 60;
        drawRows(gfx, px, listTop, listBot, mx, my);
        drawScrollbar(gfx, px + PW - 4, listTop, listBot);

        hline(gfx, px, listBot, PW, C_BORDER);

        int detailY = listBot + 6;
        if (selectedRow != null && selectedRow.kind() != RowKind.SECTION) {
            drawDetail(gfx, px, detailY, selectedRow);
        } else {
            dText(gfx, Component.literal("Click a row to see details"), px + 10, detailY + 8, C_DIM);
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
    private void drawRows(GuiGraphicsExtractor gfx, int px, int listTop, int listBot, int mx, int my) {
    //? } else {
    /*private void drawRows(GuiGraphics gfx, int px, int listTop, int listBot, int mx, int my) {*/
    //? }
        gfx.enableScissor(px + 1, listTop, px + PW - 1, listBot);

        List<Row> rows = buildRows();
        int y = listTop - scrollY;

        for (Row row : rows) {
            if (y + RH < listTop) { y += RH; continue; }
            if (y > listBot) break;

            if (row.kind() == RowKind.SECTION) {
                dText(gfx, Component.literal(row.label()), px + 8, y + (RH - 7) / 2, C_SECT);
                hline(gfx, px + 8, y + RH - 2, PW - 16, C_ROWLINE);
                y += RH;
                continue;
            }

            boolean hov = mx >= px + 1 && mx < px + PW - 5 && my >= y && my < y + RH;
            boolean sel = row == selectedRow;
            gfx.fill(px + 1, y, px + PW - 5, y + RH, sel ? 0xFF1E3050 : (hov ? C_ROW_HOV : C_ROW));
            hline(gfx, px + 1, y + RH - 1, PW - 7, C_ROWLINE);

            dText(gfx, Component.literal(iconFor(row.severity())), px + 10, y + (RH - 7) / 2, colorFor(row.severity()));
            dText(gfx, Component.literal(row.label()), px + 26, y + (RH - 7) / 2, C_TEXT);

            y += RH;
        }

        gfx.disableScissor();
    }

    //? if >=26 {
    private void drawDetail(GuiGraphicsExtractor gfx, int px, int y, Row row) {
    //? } else {
    /*private void drawDetail(GuiGraphics gfx, int px, int y, Row row) {*/
    //? }
        dText(gfx, Component.literal(row.label()), px + 10, y, C_TEXT);
        dText(gfx, Component.literal("Required: " + (row.expected().isBlank() ? "any" : row.expected())),
                px + 10, y + 12, C_DIM);
        dText(gfx, Component.literal("Installed: " + (row.actual().isBlank() ? "Not Found" : row.actual())),
                px + 10, y + 24, C_DIM);
        dText(gfx, Component.literal("Status: " + row.reason()), px + 10, y + 36, colorFor(row.severity()));
    }

    //? if >=26 {
    private void hline(GuiGraphicsExtractor g, int x, int y, int w, int c) {
    //? } else {
    /*private void hline(GuiGraphics g, int x, int y, int w, int c) {*/
    //? }
        if (w > 0) g.fill(x, y, x + w, y + 1, c);
    }

    //? if >=26 {
    private void drawScrollbar(GuiGraphicsExtractor g, int x, int top, int bot) {
    //? } else {
    /*private void drawScrollbar(GuiGraphics g, int x, int top, int bot) {*/
    //? }
        int h = bot - top;
        int total = buildRows().size() * RH;
        g.fill(x, top, x + 3, bot, 0xFF181818);
        if (total <= h) return;
        int th = Math.max(18, h * h / total);
        int ty = top + (int) ((long) scrollY * (h - th) / Math.max(1, total - h));
        g.fill(x, ty, x + 3, ty + th, 0xFF484848);
    }

    //? if >=1.21.9 {
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);
        double mx = event.x(), my = event.y();
        if (handleRowClick(mx, my)) return true;
        return super.mouseClicked(event, doubleClick);
    }
    //? } else {
    /*@Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        if (handleRowClick(mx, my)) return true;
        return super.mouseClicked(mx, my, button);
    }*/
    //? }

    private boolean handleRowClick(double mx, double my) {
        int px = px(), py = py();
        int listTop = py + 50, listBot = py + PH - 60;
        if (mx < px + 1 || mx > px + PW - 5 || my < listTop || my > listBot) return false;

        List<Row> rows = buildRows();
        int y = listTop - scrollY;
        for (Row row : rows) {
            if (row.kind() != RowKind.SECTION && my >= y && my < y + RH) {
                selectedRow = row;
                return true;
            }
            y += RH;
        }
        return false;
    }

    //? if >=1.20.5 {
    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
    //? } else {
    /*@Override
    public boolean mouseScrolled(double mx, double my, double sy) {
        double sx = 0;*/
    //? }
        int listTop = py() + 50, listBot = py() + PH - 60;
        if (mx < px() || mx > px() + PW || my < listTop || my > listBot)
            //? if >=1.20.5 {
            return super.mouseScrolled(mx, my, sx, sy);
            //? } else {
            /*return super.mouseScrolled(mx, my, sy);*/
            //? }
        int totalH = buildRows().size() * RH;
        int viewH = listBot - listTop;
        scrollY = (int) Math.max(0, Math.min(scrollY - sy * RH, Math.max(0, totalH - viewH)));
        return true;
    }

    @Override public boolean isPauseScreen() { return false; }
}
