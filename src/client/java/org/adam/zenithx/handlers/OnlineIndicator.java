package org.adam.zenithx.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.*;

public class OnlineIndicator {

    public static final ThreadLocal<Boolean> currentlyDrawingPlayerEntityName =
            ThreadLocal.withInitial(() -> false);

    public static final Set<String> ONLINE_MOD_PLAYERS = new HashSet<>();

    private static final Identifier TAB_LIST_ICON =
            Identifier.fromNamespaceAndPath("zenithx", "textures/tab_list_icon.png");

    private static final Map<String, UUID> PLAYER_CACHE = new Object2ObjectOpenHashMap<>();

    public static void trackChatMessage(Component message) {
        if (message == null) return;
        UUID uuid = findUUIDFromDisplayName(message);
        if (uuid == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        PlayerInfo info = mc.getConnection().getPlayerInfo(uuid);
        if (info != null && info.getProfile() != null && info.getProfile().name() != null) {
            ONLINE_MOD_PLAYERS.add(info.getProfile().name());
        }
    }

    public static void drawTabListOverlay(GuiGraphicsExtractor graphics, PlayerTabOverlay overlay, int screenWidth) {
        if (graphics == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        Collection<PlayerInfo> players;
        try {
            players = mc.getConnection().getOnlinePlayers();
        } catch (Exception e) {
            return;
        }

        int i = 0;
        List<PlayerInfo> sorted = players.stream()
                .sorted(Comparator.comparingInt(p -> -p.getTabListOrder()))
                .limit(80)
                .toList();

        int cols = 1;
        int rows = sorted.size();
        while (rows > 20) {
            cols++;
            rows = (sorted.size() + cols - 1) / cols;
        }

        int slotWidth = Math.min(cols * (9 + 200 + 13), screenWidth - 50) / cols;
        int xxo = screenWidth / 2 - (slotWidth * cols + (cols - 1) * 5) / 2;
        int yyo = 10;

        for (int idx = 0; idx < sorted.size(); idx++) {
            int col = idx / rows;
            int row = idx % rows;
            int xo = xxo + col * slotWidth + col * 5;
            int yo = yyo + row * 9;

            PlayerInfo info = sorted.get(idx);
            GameProfile profile = info.getProfile();
            if (profile != null && profile.name() != null
                    && ONLINE_MOD_PLAYERS.contains(profile.name())) {
                renderIcon(graphics, xo - 10, yo);
            }
        }
    }

    public static void drawTabIndicator(Object graphics, PlayerInfo playerInfo, int x, int y) {
        if (graphics == null || playerInfo == null) return;
        GameProfile profile = playerInfo.getProfile();
        if (profile != null && profile.name() != null
                && ONLINE_MOD_PLAYERS.contains(profile.name())) {
            renderIconRaw(graphics, x - 12, y);
        }
    }

    public static void drawNametagIndicator(Object graphics, int x, int y) {
        if (graphics == null) return;
        renderIconRaw(graphics, x - 12, y - 10);
    }

    private static void renderIcon(GuiGraphicsExtractor graphics, int x, int y) {
        try {
            var m = graphics.getClass().getMethod("blitSprite",
                    net.minecraft.client.renderer.RenderPipelines.class,
                    Identifier.class, int.class, int.class, int.class, int.class);
            m.invoke(graphics,
                    net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                    TAB_LIST_ICON, x, y, 8, 8);
        } catch (Exception ignored) {
            renderIconRaw(graphics, x, y);
        }
    }

    private static void renderIconRaw(Object graphics, int x, int y) {
        try {
            for (var m : graphics.getClass().getMethods()) {
                if ((m.getName().equals("blit")
                        || m.getName().equals("drawTexture"))
                        && m.getParameterCount() >= 9) {
                    if (m.getParameterCount() == 9) {
                        m.invoke(graphics, TAB_LIST_ICON, x, y, 0f, 0f, 8, 8, 8, 8);
                    } else {
                        m.invoke(graphics, TAB_LIST_ICON, x, y, 0, 0f, 0f, 8, 8, 8, 8);
                    }
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    public static UUID findUUIDFromDisplayName(Component displayName) {
        if (displayName == null) return null;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return null;

        try {
            Collection<PlayerInfo> players;
            try {
                players = mc.getConnection().getOnlinePlayers();
            } catch (NoSuchMethodError e) {
                var method = mc.getConnection().getClass().getMethod("getPlayerInfoMap");
                Object result = method.invoke(mc.getConnection());
                if (result instanceof Map<?, ?> map) {
                    players = map.values().stream()
                            .filter(v -> v instanceof PlayerInfo)
                            .map(v -> (PlayerInfo) v)
                            .toList();
                } else {
                    return null;
                }
            }

            PLAYER_CACHE.clear();

            for (PlayerInfo info : players) {
                GameProfile profile = info.getProfile();
                if (profile != null && profile.name() != null && profile.id() != null) {
                    PLAYER_CACHE.put(profile.name(), profile.id());
                }
            }

            String text = displayName.getString();
            String[] parts = text.split("[^\\w]+");

            for (String part : parts) {
                if (PLAYER_CACHE.containsKey(part)) {
                    return PLAYER_CACHE.get(part);
                }
            }

        } catch (Exception ignored) {}

        return null;
    }

    public static int getTextBackgroundOpacity() {
        try {
            return (int) (Minecraft.getInstance()
                    .options.textBackgroundOpacity().get() * 255);
        } catch (Exception e) {
            return 128;
        }
    }
}