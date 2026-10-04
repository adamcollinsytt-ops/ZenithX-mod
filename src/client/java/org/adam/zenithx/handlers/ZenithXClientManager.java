package org.adam.zenithx.handlers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ZenithXClientManager {
    private static final Map<UUID, Boolean> PLAYER_TAG_VISIBILITY = new ConcurrentHashMap<>();

    public static void setTagVisible(UUID playerUuid, boolean isVisible) {
        PLAYER_TAG_VISIBILITY.put(playerUuid, isVisible);
    }

    public static boolean isTagVisible(UUID playerUuid) {
        return PLAYER_TAG_VISIBILITY.getOrDefault(playerUuid, true);
    }
}