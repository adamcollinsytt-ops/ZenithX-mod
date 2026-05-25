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

    public static boolean currentlyDrawingPlayerEntityName() {
        return currentlyDrawingPlayerEntityName.get();
    }

    public static void trackChatMessage(Component message) {
        if (message == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        String text = message.getString();
        String[] parts = text.split("[^\\w]+");

        for (String part : parts) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(part);
            if (info != null && info.getProfile() != null && info.getProfile().name() != null) {
                ONLINE_MOD_PLAYERS.add(info.getProfile().name());
                return;
            }
        }
    }

    public static void drawTabListOverlay(GuiGraphicsExtractor graphics, PlayerTabOverlay overlay, int screenWidth) {
        if (graphics == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        List<PlayerInfo> sorted = mc.getConnection().getListedOnlinePlayers().stream()
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

    private static void renderIcon(GuiGraphicsExtractor graphics, int x, int y) {
        try {
            var method = graphics.getClass().getMethod("blitSprite",
                    net.minecraft.client.renderer.RenderPipelines.class,
                    Identifier.class, int.class, int.class, int.class, int.class);
            method.invoke(graphics,
                    net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                    TAB_LIST_ICON, x, y, 8, 8);
        } catch (Exception ignored) {}
    }

    public static UUID findUUIDFromDisplayName(Component displayName) {
        if (displayName == null) return null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return null;

        try {
            PLAYER_CACHE.clear();
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
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