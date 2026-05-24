package org.adam.zenithx.handlers;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class Settings {

    public static boolean lowFire = false;
    public static boolean noTotemPop = false;

    public static KeyMapping openGui;

    public static void init() {
        //openGui = new KeyMapping(
        //        "ZenithX Settings",
        //        GLFW.GLFW_KEY_B,
        //        KeyMapping.Category.MISC
        //);
    }

    public static void tick() {
        if (openGui == null) return;
        Minecraft mc = Minecraft.getInstance();
        while (openGui.consumeClick()) {
            mc.setScreen(new SettingsScreen());
        }
    }

    public static class SettingsScreen extends Screen {

        protected SettingsScreen() {
            super(Component.literal("ZenithX Settings"));
        }

        @Override
        protected void init() {

            addRenderableWidget(
                    Button.builder(
                            Component.literal("Low Fire: " + (lowFire ? "ON" : "OFF")),
                            b -> {
                                lowFire = !lowFire;
                                b.setMessage(Component.literal("Low Fire: " + (lowFire ? "ON" : "OFF")));
                            }
                    ).bounds(width / 2 - 100, height / 2 - 40, 200, 20).build()
            );

            addRenderableWidget(
                    Button.builder(
                            Component.literal("No Totem Pop: " + (noTotemPop ? "ON" : "OFF")),
                            b -> {
                                noTotemPop = !noTotemPop;
                                b.setMessage(Component.literal("No Totem Pop: " + (noTotemPop ? "ON" : "OFF")));
                            }
                    ).bounds(width / 2 - 100, height / 2 - 10, 200, 20).build()
            );
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}