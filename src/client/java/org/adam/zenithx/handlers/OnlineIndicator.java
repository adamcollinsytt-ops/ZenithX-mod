package org.adam.zenithx.handlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.authlib.GameProfile;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.*;

public class OnlineIndicator {

    public static final Set<String> ONLINE_MOD_PLAYERS = new HashSet<>();

    private static final Identifier TAB_LIST_ICON =
            Identifier.fromNamespaceAndPath("zenithx", "textures/tab_list_icon.png");

    private static final Map<String, UUID> PLAYER_CACHE =
            new Object2ObjectOpenHashMap<>();

    public static void trackChatMessage(Component message) {
        if (message == null) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        String[] parts = message.getString().split("[^\\w]+");

        for (String part : parts) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(part);
            if (info != null && info.getProfile() != null) {
                String name = info.getProfile().name();
                if (name != null) {
                    ONLINE_MOD_PLAYERS.add(name);
                    return;
                }
            }
        }
    }

    public static void drawTabListOverlay(GuiGraphics graphics, int screenWidth) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        List<PlayerInfo> players = mc.getConnection().getListedOnlinePlayers()
                .stream()
                .sorted(Comparator.comparingInt(p -> -p.getTabListOrder()))
                .limit(80)
                .toList();

        if (players.isEmpty()) return;

        int rows = players.size();
        int cols = 1;

        while (rows > 20) {
            cols++;
            rows = (players.size() + cols - 1) / cols;
        }

        int slotWidth = Math.min(cols * 120, screenWidth - 50) / cols;

        int xStart = screenWidth / 2 - (slotWidth * cols) / 2;
        int yStart = 10;

        float guiScale = mc.getWindow().getGuiScale();

        float scale;
        float offset;

        if (guiScale < 4) {
            scale = 1.0f;
            offset = 0f;
        } else {
            scale = 0.75f;
            offset = 0.375f;
        }

        int iconSize = Math.round(5 * scale);

        for (int i = 0; i < players.size(); i++) {

            int col = i / rows;
            int row = i % rows;

            int x = xStart + col * slotWidth + col * 5;
            int y = yStart + row * 9;

            PlayerInfo info = players.get(i);
            GameProfile profile = info.getProfile();

            if (profile == null || profile.name() == null) continue;

            if (!ONLINE_MOD_PLAYERS.contains(profile.name())) continue;

            graphics.blit(
                    TAB_LIST_ICON,
                    x - 10 + offset,
                    y + offset,
                    0,
                    0,
                    iconSize,
                    iconSize,
                    8,
                    8
            );
        }
    }

    public static UUID findUUIDFromDisplayName(Component displayName) {
        if (displayName == null) return null;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return null;

        PLAYER_CACHE.clear();

        for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
            GameProfile profile = info.getProfile();
            if (profile != null && profile.name() != null && profile.id() != null) {
                PLAYER_CACHE.put(profile.name(), profile.id());
            }
        }

        String[] parts = displayName.getString().split("[^\\w]+");

        for (String part : parts) {
            UUID id = PLAYER_CACHE.get(part);
            if (id != null) return id;
        }

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