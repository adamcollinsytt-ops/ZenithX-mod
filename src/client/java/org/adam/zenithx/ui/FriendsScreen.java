package org.adam.zenithx.ui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.adam.zenithx.ui.notification.Notification;
import org.adam.zenithx.ui.notification.NotificationManager;
import org.adam.zenithx.ui.components.MenuButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.storage.LevelSummary;
import org.adam.zenithx.FriendManager;
import org.adam.zenithx.HostClient;

import java.util.ArrayList;
import java.util.List;

public class FriendsScreen extends HostBaseScreen {

    private static final int PW = 262;
    private static final int PH = 322;
    private static final int RH = 26;
    private static final int AV = 16;

    private static final int C_BG       = 0xFF1A1A1A;
    private static final int C_TOPBAR   = 0xFF212121;
    private static final int C_ROW      = 0xFF1E1E1E;
    private static final int C_ROW_HOV  = 0xFF2A2A2A;
    private static final int C_BORDER   = 0xFF3A3A3A;
    private static final int C_SECT     = 0xFF484848;
    private static final int C_TEXT     = 0xFFD4D0C8;
    private static final int C_DIM      = 0xFF888888;
    private static final int C_ONLINE   = 0xFF55AA55;
    private static final int C_OFFLINE  = 0xFF555555;
    private static final int C_TABLINE  = 0xFF4A90D9;
    private static final int C_TABSEL   = 0xFF252525;
    private static final int C_SRCHBG   = 0xFF111111;
    private static final int C_ACCEPT   = 0xFF4E7A2C;
    private static final int C_ACCEPT_B = 0xFF3A6A1A;
    private static final int C_REJECT   = 0xFF8A2222;
    private static final int C_REJECT_B = 0xFF6A1212;
    private static final int C_PLUS     = 0xFF2E2E2E;
    private static final int C_PLUS_B   = 0xFF484848;
    private static final int C_BADGE    = 0xFFC03030;
    private static final int C_WHITE    = 0xFFFFFFFF;
    private static final int C_SCRBAR   = 0xFF181818;
    private static final int C_SCRTHUMB = 0xFF484848;
    private static final int C_ROWLINE  = 0xFF222222;

    private static final MenuButton.Style STYLE_DEFAULT =
            new MenuButton.Style(C_TEXT,  0xFF252525, C_BORDER);
    private static final MenuButton.Style STYLE_TAB_ACTIVE =
            new MenuButton.Style(C_WHITE, C_TABSEL,  C_TABLINE);
    private static final MenuButton.Style STYLE_TAB_HOVER =
            new MenuButton.Style(C_WHITE, 0xFF303030, C_BORDER);
    private static final MenuButton.Style STYLE_PLUS =
            new MenuButton.Style(C_WHITE, C_PLUS,    C_PLUS_B);
    private static final MenuButton.Style STYLE_PLUS_HOV =
            new MenuButton.Style(C_WHITE, C_PLUS_B,  C_BORDER);
    private static final MenuButton.Style STYLE_ACCEPT =
            new MenuButton.Style(C_WHITE, C_ACCEPT,  C_ACCEPT_B);
    private static final MenuButton.Style STYLE_ACCEPT_HOV =
            new MenuButton.Style(C_WHITE, C_ACCEPT_B, 0xFF2A5A10);
    private static final MenuButton.Style STYLE_REJECT =
            new MenuButton.Style(C_WHITE, C_REJECT,  C_REJECT_B);
    private static final MenuButton.Style STYLE_REJECT_HOV =
            new MenuButton.Style(C_WHITE, C_REJECT_B, 0xFF4A0A0A);

    private enum Tab { FRIENDS, REQUESTS, INVITES }
    private Tab tab = Tab.FRIENDS;

    private int    scrollY = 0;
    private String query   = "";

    private EditBox    searchBox;
    private MenuButton tabFriends;
    private MenuButton tabRequests;
    private MenuButton tabInvites;

    private List<String> friends  = new ArrayList<>();
    private List<String> requests = new ArrayList<>();
    private List<String> invites  = new ArrayList<>();

    private final HostBaseScreen parent;
    private final LevelSummary   worldToHost;

    private void startHosting() {
        if (worldToHost == null) return;
        try {
            minecraft.createWorldOpenFlows().openWorld(worldToHost.getLevelId(), () -> {});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public FriendsScreen(HostBaseScreen parent) {
        super(Component.literal("Invite Friends"));
        this.parent      = parent;
        this.worldToHost = null;
    }

    public FriendsScreen(HostBaseScreen parent, LevelSummary world) {
        super(Component.literal("Invite Friends"));
        this.parent      = parent;
        this.worldToHost = world;
    }

    private int px() { return (width  - PW) / 2; }
    private int py() { return (height - PH) / 2; }

    @Override
    protected void init() {
        int px = px(), py = py();

        searchBox = new EditBox(font,
                px + 20, py + 31, PW - 52, 14,
                Component.literal("search"));
        searchBox.setHint(Component.literal("Search for players..."));
        searchBox.setBordered(false);
        searchBox.setMaxLength(32);
        searchBox.setResponder(q -> { query = q; scrollY = 0; refresh(); });
        addRenderableWidget(searchBox);

        addRenderableWidget(new MenuButton(
                px + PW - 30, py + 27, 22, 22,
                Component.literal("+"),
                STYLE_PLUS, STYLE_PLUS_HOV,
                MenuButton.DISABLED, true,
                btn -> minecraft.setScreen(new AddFriendScreen(this))
        ));

        int tabW = (PW - 6) / 3;
        int tabY = py + 52;

        tabFriends = new MenuButton(
                px + 2, tabY, tabW, 18,
                Component.literal("Friends"),
                STYLE_TAB_ACTIVE, STYLE_TAB_HOVER,
                MenuButton.DISABLED, false,
                btn -> setTab(Tab.FRIENDS)
        );
        addRenderableWidget(tabFriends);

        tabRequests = new MenuButton(
                px + 3 + tabW, tabY, tabW, 18,
                Component.literal("Requests"),
                STYLE_DEFAULT, STYLE_TAB_HOVER,
                MenuButton.DISABLED, false,
                btn -> setTab(Tab.REQUESTS)
        );
        addRenderableWidget(tabRequests);

        tabInvites = new MenuButton(
                px + 4 + tabW * 2, tabY, tabW, 18,
                Component.literal("Invites"),
                STYLE_DEFAULT, STYLE_TAB_HOVER,
                MenuButton.DISABLED, false,
                btn -> setTab(Tab.INVITES)
        );
        addRenderableWidget(tabInvites);

        if (worldToHost != null) {
            addRenderableWidget(new MenuButton(
                    px + PW / 2 - 52,
                    py + PH - 42,
                    104, 16,
                    Component.literal("Host World"),
                    MenuButton.GREEN, MenuButton.LIGHT_GREEN, MenuButton.DISABLED, true,
                    btn -> startHosting()
            ));
        }

        addRenderableWidget(new MenuButton(
                px + PW / 2 - 52,
                py + PH - 21,
                104, 16,
                Component.literal("Done"),
                MenuButton.BLUE, MenuButton.LIGHT_BLUE, MenuButton.DISABLED, true,
                btn -> {
                    if (parent != null) minecraft.setScreen(parent);
                    else onClose();
                }
        ));

        refresh();
        updateTabStyles();
    }

    private void setTab(Tab t) {
        tab = t;
        scrollY = 0;
        refresh();
        updateTabStyles();
    }

    private void updateTabStyles() {
        if (tabFriends == null) return;
        tabFriends .setDefaultStyle(tab == Tab.FRIENDS  ? STYLE_TAB_ACTIVE : STYLE_DEFAULT);
        tabRequests.setDefaultStyle(tab == Tab.REQUESTS ? STYLE_TAB_ACTIVE : STYLE_DEFAULT);
        tabInvites .setDefaultStyle(tab == Tab.INVITES  ? STYLE_TAB_ACTIVE : STYLE_DEFAULT);
    }

    private void refresh() {
        FriendManager fm = FriendManager.getInstance();
        String q = query;
        friends  = fm.getFriends().stream()
                .filter(n -> q.isEmpty() || n.contains(q)).toList();
        requests = fm.getPendingRequests().stream()
                .filter(n -> q.isEmpty() || n.contains(q)).toList();
        invites  = fm.getPendingInvites().stream()
                .filter(n -> q.isEmpty() || n.contains(q)).toList();
        friends.forEach(SkinCache::request);
        requests.forEach(SkinCache::request);
        invites.forEach(SkinCache::request);
    }

    public void publicrefresh2026byadam() {
        refresh();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mx, int my, float a) {
        int px = px(), py = py();

        gfx.fill(px + 4, py + 4, px + PW + 4, py + PH + 4, 0x55000000);
        gfx.fill(px, py, px + PW, py + PH, C_BG);
        gfx.outline(px, py, PW, PH, C_BORDER);

        gfx.fill(px, py, px + PW, py + 26, C_TOPBAR);
        hline(gfx, px, py + 26, PW, C_BORDER);
        gfx.centeredText(font, Component.literal("Invite Friends"),
                px + PW / 2, py + 9, C_TEXT);

        gfx.fill(px + 8, py + 27, px + PW - 8, py + 49, C_SRCHBG);
        gfx.outline(px + 8, py + 27, PW - 16, 22, C_SECT);
        drawSearchIcon(gfx, px + 10, py + 30);

        drawTabs(gfx, px, py);

        int rqC  = FriendManager.getInstance().getPendingRequests().size();
        int invC = FriendManager.getInstance().getPendingInvites().size();
        int tabW = (PW - 6) / 3;
        if (rqC  > 0) drawBadge(gfx, px + 3 + tabW + tabW - 8,     py + 53, rqC);
        if (invC > 0) drawBadge(gfx, px + 4 + tabW * 2 + tabW - 8, py + 53, invC);

        int listTop = py + 72, listBot = py + PH - 24;
        drawRows(gfx, px, listTop, listBot, mx, my);
        drawScrollbar(gfx, px + PW - 4, listTop, listBot);

        hline(gfx, px, py + PH - 24, PW, C_BORDER);
        gfx.fill(px + 1, py + PH - 23, px + PW - 1, py + PH - 1, 0xFF1A1E2E);

        super.extractRenderState(gfx, mx, my, a);
    }

    private void drawTabs(GuiGraphicsExtractor gfx, int px, int py) {
        int tabW = (PW - 6) / 3;
        int ty   = py + 52;

        gfx.fill(px + 2, ty, px + PW - 2, ty + 18, 0xFF1C1C1C);
        gfx.outline(px + 2, ty, PW - 4, 18, C_BORDER);

        gfx.fill(px + 2 + tabW,     ty + 2, px + 3 + tabW,     ty + 16, C_BORDER);
        gfx.fill(px + 4 + tabW * 2, ty + 2, px + 5 + tabW * 2, ty + 16, C_BORDER);

        int atx = switch (tab) {
            case FRIENDS  -> px + 2;
            case REQUESTS -> px + 3 + tabW;
            case INVITES  -> px + 4 + tabW * 2;
        };
        hline(gfx, atx, ty + 16, tabW, C_TABLINE);
        hline(gfx, atx, ty + 17, tabW, C_TABLINE);
    }

    private void drawRows(GuiGraphicsExtractor gfx,
                          int px, int listTop, int listBot, int mx, int my) {
        gfx.enableScissor(px + 1, listTop, px + PW - 1, listBot);

        List<Row> rows = buildRows();
        int y = listTop - scrollY;

        for (Row row : rows) {
            if (y + RH < listTop) { y += RH; continue; }
            if (y > listBot)       break;

            if (row.isSection) {
                int lw = font.width(row.name);
                int lx = px + PW / 2 - lw / 2;
                hline(gfx, px + 6,      y + RH / 2, lx - px - 10,          C_SECT);
                hline(gfx, lx + lw + 4, y + RH / 2, px + PW - lx - lw - 9, C_SECT);
                gfx.centeredText(font, Component.literal(row.name),
                        px + PW / 2, y + (RH - 7) / 2, C_DIM);
                y += RH;
                continue;
            }

            boolean hov = mx >= px + 1 && mx < px + PW - 5
                    && my >= y && my < y + RH;
            gfx.fill(px + 1, y, px + PW - 5, y + RH, hov ? C_ROW_HOV : C_ROW);
            hline(gfx, px + 1, y + RH - 1, PW - 7, C_ROWLINE);

            int avX = px + 5, avY = y + (RH - AV) / 2;
            drawAvatar(gfx, avX, avY, row.name);

            String extra = FriendManager.getInstance().getLastSeenText(row.name);
            gfx.text(font, Component.literal(row.name + " " + extra),
                    px + 26, y + (RH - 7) / 2, C_TEXT);

            if (row.type == Row.T.FRIEND) {
                boolean online = FriendManager.getInstance().isOnline(row.name);
                drawOnlineDot(gfx, px + PW - 48, y + RH / 2 - 3, online);
                drawMenuBtn(gfx, px + PW - 36, y + (RH - 14) / 2, 14, 14,
                        STYLE_PLUS, STYLE_PLUS_HOV, "+", mx, my);
                drawMenuBtn(gfx, px + PW - 20, y + (RH - 14) / 2, 14, 14,
                        STYLE_REJECT, STYLE_REJECT_HOV, "X", mx, my);
            }

            if (row.type == Row.T.REQUEST || row.type == Row.T.INVITE) {
                int bx = px + PW - 44, bby = y + (RH - 14) / 2;
                drawMenuBtn(gfx, bx,      bby, 16, 14, STYLE_ACCEPT, STYLE_ACCEPT_HOV, "✓", mx, my);
                drawMenuBtn(gfx, bx + 18, bby, 16, 14, STYLE_REJECT, STYLE_REJECT_HOV, "✗", mx, my);
            }

            y += RH;
        }

        gfx.disableScissor();
    }

    private void drawMenuBtn(GuiGraphicsExtractor gfx,
                             int x, int y, int w, int h,
                             MenuButton.Style normal, MenuButton.Style hover,
                             String label, int mx, int my) {
        boolean hov = mx >= x && mx < x + w && my >= y && my < y + h;
        MenuButton.Style s = hov ? hover : normal;
        gfx.fill(x + 1, y + 1, x + w - 1, y + h - 1, s.buttonColor());
        gfx.fill(x,     y,     x + 1,     y + h,      s.highlightColor());
        gfx.fill(x + 1, y,     x + w,     y + 1,      s.highlightColor());
        gfx.fill(x + w - 1, y,     x + w,     y + h,      s.shadowColor());
        gfx.fill(x,         y + h - 1, x + w - 1, y + h,  s.shadowColor());
        gfx.fill(x - 1,  y - 1, x + w + 1, y,          s.outlineColor());
        gfx.fill(x - 1,  y + h, x + w + 1, y + h + 1,  s.outlineColor());
        gfx.fill(x - 1,  y,     x,         y + h,       s.outlineColor());
        gfx.fill(x + w,  y,     x + w + 1, y + h,       s.outlineColor());
        gfx.centeredText(font, Component.literal(label),
                x + w / 2, y + (h - 7) / 2, s.textColor());
    }

    private void drawAvatar(GuiGraphicsExtractor gfx, int avX, int avY, String name) {
        Identifier skin = SkinCache.get(name);
        if (skin != null) {
            gfx.blit(RenderPipelines.GUI_TEXTURED, skin, avX, avY, 0f, 0f, AV, AV, AV, AV);
            gfx.outline(avX, avY, AV, AV, 0x66000000);
        } else {
            gfx.fill(avX, avY, avX + AV, avY + AV, colorFor(name));
            gfx.outline(avX, avY, AV, AV, 0xFF404040);
            gfx.fill(avX + 3, avY + 4, avX + 5,  avY + 6,  0x99000000);
            gfx.fill(avX + 7, avY + 4, avX + 9,  avY + 6,  0x99000000);
            gfx.fill(avX + 3, avY + 9, avX + 10, avY + 10, 0x99000000);
            String c = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase();
            gfx.centeredText(font, Component.literal(c), avX + AV / 2, avY + 4, 0xCCFFFFFF);
        }
    }

    private void drawOnlineDot(GuiGraphicsExtractor gfx, int x, int y, boolean online) {
        int c = online ? C_ONLINE : C_OFFLINE;
        gfx.fill(x + 1, y,     x + 6, y + 1, c);
        gfx.fill(x,     y + 1, x + 7, y + 6, c);
        gfx.fill(x + 1, y + 6, x + 6, y + 7, c);
        if (online) gfx.fill(x + 1, y + 1, x + 3, y + 3, 0xFF88DD88);
    }

    private void hline(GuiGraphicsExtractor g, int x, int y, int w, int c) {
        if (w > 0) g.fill(x, y, x + w, y + 1, c);
    }

    private void drawBadge(GuiGraphicsExtractor g, int x, int y, int n) {
        n = Math.min(n, 99);
        int w = n >= 10 ? 18 : 13;
        g.fill(x, y, x + w, y + 10, C_BADGE);
        g.outline(x, y, w, 10, 0xFF991010);
        g.centeredText(font, Component.literal(String.valueOf(n)), x + w / 2, y + 1, C_WHITE);
    }

    private void drawScrollbar(GuiGraphicsExtractor g, int x, int top, int bot) {
        int h     = bot - top;
        int total = buildRows().size() * RH;
        g.fill(x, top, x + 3, bot, C_SCRBAR);
        if (total <= h) return;
        int th = Math.max(18, h * h / total);
        int ty = top + (int) ((long) scrollY * (h - th) / Math.max(1, total - h));
        g.fill(x, ty, x + 3, ty + th, C_SCRTHUMB);
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
        g.fill(x + 7, y + 7, x + 8, y + 8,   ic);
        g.fill(x + 8, y + 8, x + 10, y + 10, ic);
        g.fill(x + 2, y + 1, x + 6, y + 2, 0xFF555555);
        g.fill(x + 1, y + 2, x + 2, y + 6, 0xFF555555);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) return super.mouseClicked(event, doubleClick);

        double mx = event.x(), my = event.y();
        int px = px(), py = py();
        int listTop = py + 72, listBot = py + PH - 24;

        List<Row> rows = buildRows();
        int y = listTop - scrollY;

        for (Row row : rows) {
            if (y + RH < listTop) { y += RH; continue; }
            if (y > listBot)       break;

            if (!row.isSection && my >= y && my < y + RH) {
                if (row.type == Row.T.FRIEND) {
                    int yBtn    = y + (RH - 14) / 2;
                    int inviteX = px + PW - 36;
                    int removeX = px + PW - 20;

                    if (inBox(mx, my, inviteX, yBtn, 14, 14)) {
                        HostClient.getInstance().sendInvite(row.name);
                        NotificationManager.show(new Notification(
                                "ZenithX",
                                "Invite sent to " + row.name,
                                3.5f, false,
                                "invite_" + row.name,
                                () -> true,
                                () -> {}, () -> {}, null, null
                        ));
                        return true;
                    }

                    if (inBox(mx, my, removeX, yBtn, 14, 14)) {
                        HostClient.getInstance().removeFriend(row.name);
                        FriendManager.getInstance().removeFriend(row.name);
                        refresh();
                        return true;
                    }
                }
                if (row.type == Row.T.REQUEST) {
                    int bx = px + PW - 44, bby = y + (RH - 14) / 2;
                    if (inBox(mx, my, bx, bby, 16, 14)) {
                        HostClient.getInstance().acceptFriend(row.name);
                        FriendManager.getInstance().removePendingRequest(row.name);
                        refresh(); return true;
                    }
                    if (inBox(mx, my, bx + 18, bby, 16, 14)) {
                        FriendManager.getInstance().removePendingRequest(row.name);
                        refresh(); return true;
                    }
                }
                if (row.type == Row.T.INVITE) {
                    int bx = px + PW - 44, bby = y + (RH - 14) / 2;
                    if (inBox(mx, my, bx, bby, 16, 14)) {
                        HostClient.getInstance().acceptInvite(row.name);
                        FriendManager.getInstance().removePendingInvite(row.name);
                        refresh(); return true;
                    }
                    if (inBox(mx, my, bx + 18, bby, 16, 14)) {
                        FriendManager.getInstance().removePendingInvite(row.name);
                        refresh(); return true;
                    }
                }
            }
            y += RH;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        int listTop = py() + 72, listBot = py() + PH - 24;
        if (mx < px() || mx > px() + PW || my < listTop || my > listBot)
            return super.mouseScrolled(mx, my, sx, sy);
        int totalH = buildRows().size() * RH;
        int viewH  = listBot - listTop;
        scrollY = (int) Math.max(0, Math.min(scrollY - sy * RH, Math.max(0, totalH - viewH)));
        return true;
    }

    private List<Row> buildRows() {
        List<Row> rows = new ArrayList<>();
        FriendManager fm = FriendManager.getInstance();

        switch (tab) {
            case FRIENDS -> {
                List<String> online  = friends.stream().filter(fm::isOnline).toList();
                List<String> offline = friends.stream().filter(n -> !fm.isOnline(n)).toList();
                if (!online.isEmpty()) {
                    rows.add(Row.sect("Online"));
                    online.forEach(n -> rows.add(Row.friend(n, true)));
                }
                if (!offline.isEmpty()) {
                    rows.add(Row.sect("Offline"));
                    offline.forEach(n -> rows.add(Row.friend(n, false)));
                }
                if (!requests.isEmpty()) {
                    rows.add(Row.sect("Friend Requests"));
                    requests.forEach(n -> rows.add(Row.of(n, Row.T.REQUEST)));
                }
                if (!invites.isEmpty()) {
                    rows.add(Row.sect("Incoming Invites"));
                    invites.forEach(n -> rows.add(Row.of(n, Row.T.INVITE)));
                }
                if (rows.isEmpty()) rows.add(Row.sect("No friends yet"));
            }
            case REQUESTS -> {
                if (!requests.isEmpty()) {
                    rows.add(Row.sect("Friend Requests"));
                    requests.forEach(n -> rows.add(Row.of(n, Row.T.REQUEST)));
                } else rows.add(Row.sect("No pending requests"));
            }
            case INVITES -> {
                if (!invites.isEmpty()) {
                    rows.add(Row.sect("Incoming Invites"));
                    invites.forEach(n -> rows.add(Row.of(n, Row.T.INVITE)));
                } else rows.add(Row.sect("No incoming invites"));
            }
        }
        return rows;
    }

    private static boolean inBox(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private static int colorFor(String name) {
        int[] p = { 0xFF7A4A2A, 0xFF2A4A3A, 0xFF6A3A2A,
                0xFF2A3A5A, 0xFF5A3A1A, 0xFF3A2A5A,
                0xFF1A4A2A, 0xFF5A1A1A, 0xFF3A3A5A };
        return p[Math.abs(name.hashCode()) % p.length];
    }

    @Override public boolean isPauseScreen() { return false; }

    private static final class Row {
        enum T { FRIEND, REQUEST, INVITE }
        final String  name;
        final T       type;
        final boolean isSection;
        final boolean online;

        private Row(String name, T type, boolean isSection, boolean online) {
            this.name      = name;
            this.type      = type;
            this.isSection = isSection;
            this.online    = online;
        }

        static Row sect(String label)           { return new Row(label, null,     true,  false); }
        static Row friend(String n, boolean on) { return new Row(n,    T.FRIEND, false, on); }
        static Row of(String n, T t)            { return new Row(n,    t,        false, false); }
    }
}