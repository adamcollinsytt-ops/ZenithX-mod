package org.adam.zenithx.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.client.multiplayer.PlayerInfo;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.*;

public class OnlineIndicator {

    public static final ThreadLocal<Boolean> currentlyDrawingPlayerEntityName =
            ThreadLocal.withInitial(() -> false);

    private static final ResourceLocation TAB_LIST_ICON =
            ResourceLocation.fromNamespaceAndPath("zenithx", "textures/tab_list_icon.png");

    private static final Map<String, UUID> PLAYER_CACHE = new Object2ObjectOpenHashMap<>();

    public static boolean currentlyDrawingPlayerEntityName() {
        return currentlyDrawingPlayerEntityName.get();
    }

    public static void drawTabIndicator(GuiGraphics graphics, PlayerInfo playerInfo, int x, int y) {
        if (graphics == null || playerInfo == null) return;
        if (playerInfo.getProfile() != null) {
            renderIcon(graphics, x - 12, y);
        }
    }

    public static void drawNametagIndicator(GuiGraphics graphics, int x, int y) {
        if (graphics == null) return;
        renderIcon(graphics, x - 12, y - 10);
    }

    private static void renderIcon(GuiGraphics graphics, int x, int y) {
        graphics.blit(TAB_LIST_ICON, x, y, 0, 0f, 0f, 8, 8, 8, 8);
    }

    public static int getTextBackgroundOpacity() {
        try {
            return (int) (Minecraft.getInstance()
                    .options.textBackgroundOpacity().get() * 255);
        } catch (Exception e) {
            return 128;
        }
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
                var method = mc.getConnection().getClass()
                        .getMethod("getPlayerInfoMap");
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
                if (profile != null && profile.getName() != null && profile.getId() != null) {
                    PLAYER_CACHE.put(profile.getName(), profile.getId());
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
}