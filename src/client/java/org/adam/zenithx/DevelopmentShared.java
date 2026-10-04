package org.adam.zenithx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class DevelopmentShared {

    private static final Set<String> DEVELOPERS = new HashSet<>(Arrays.asList(
            "adamcollinsytalt",
            "adam_collinsyt",
            "matrixcfrank",
            "alkrky99",
            "bashaiq",
            "mustafa_go",
            "beroiq"
    ));

    public static void add(String username) {
        if (username != null && !username.trim().isEmpty()) {
            DEVELOPERS.add(username.trim().toLowerCase());
        }
    }

    public static void remove(String username) {
        if (username != null) {
            DEVELOPERS.remove(username.trim().toLowerCase());
        }
    }

    public static boolean isDev(String username) {
        if (username == null) return false;
        return DEVELOPERS.contains(username.trim().toLowerCase());
    }

    public static boolean isCurrentPlayerDev() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getUser() != null) {
                return isDev(mc.getUser().getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean block(String blockedUsername) {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getUser() != null) {
                String currentUsername = mc.getUser().getName();
                return isDev(currentUsername) && !currentUsername.equalsIgnoreCase(blockedUsername);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean isOfflineSession(User user) {
        if (user == null) return true;

        String token = user.getAccessToken();
        if (token == null || token.isEmpty() || token.equals("0") || token.equalsIgnoreCase("null") || token.equalsIgnoreCase("invalid")) {
            return true;
        }

        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + user.getName()).getBytes(StandardCharsets.UTF_8));
        UUID currentUuid = user.getProfileId();
        
        if (currentUuid == null) {
            return true;
        }

        return currentUuid.equals(offlineUuid);
    }

    public static boolean isFakeDevAuth() {
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.getUser() != null) {
                User user = mc.getUser();
                return isDev(user.getName()) && isOfflineSession(user);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static void checkAndCrashIfFakeDev() {
        if (isFakeDevAuth()) {
            throw new RuntimeException("[ZenithX Anti-Impersonation] Unauthorized developer account detected! Game closing.");
        }
    }

    public static boolean enforceFakeAuth() {
        if (isFakeDevAuth()) {
            Minecraft mc = Minecraft.getInstance();
            mc.execute(() -> {
                if (mc.level != null) {
                    mc.clearClientLevel(null);
                }
            });
            return true;
        }
        return false;
    }

    public static Set<String> getDevelopers() {
        return new HashSet<>(DEVELOPERS);
    }
}