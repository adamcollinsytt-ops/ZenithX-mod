package org.adam.zenithx.handlers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TagManager {
    private static final Map<UUID, Boolean> tagVisibilities = new HashMap<>();

    public static void setTagVisible(UUID uuid, boolean visible) {
        tagVisibilities.put(uuid, visible);
    }

    public static boolean isTagVisible(UUID uuid) {
        return tagVisibilities.getOrDefault(uuid, true);
    }
}