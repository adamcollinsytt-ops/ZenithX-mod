package org.adam.zenithx.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.User;

public class AuthUtils {

    public static boolean isOfficialAccount() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return false;

        User user = mc.getUser();
        if (user == null) return false;

        String accessToken = user.getAccessToken();
        if (accessToken == null || accessToken.trim().isEmpty()) {
            return false;
        }

        String lowerToken = accessToken.toLowerCase().trim();
        if (lowerToken.equals("0") 
                || lowerToken.equals("-") 
                || lowerToken.contains("offline") 
                || lowerToken.contains("null")
                || lowerToken.contains("invalid")
                || lowerToken.startsWith("token:")) {
            return false;
        }

        String xuid = user.getXuid().orElse("");
        if (xuid.trim().isEmpty()) {
            return false;
        }

        return user.getProfileId() != null;
    }
}