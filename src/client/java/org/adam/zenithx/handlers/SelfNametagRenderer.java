package org.adam.zenithx.handlers;

import net.minecraft.client.Minecraft;

public class SelfNametagRenderer {

    public static boolean shouldRenderOwnNametag() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null || mc.player == null) return false;
        
        return Minecraft.renderNames() && !mc.options.getCameraType().isFirstPerson();
    }
}