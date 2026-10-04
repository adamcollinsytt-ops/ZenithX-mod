package org.adam.zenithx.ui.notification;

import net.minecraft.client.Minecraft;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//? } else {
/*import net.minecraft.client.gui.GuiGraphics;*/
//? }

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class NotificationManager {

    private static final List<Notification> NOTIFICATIONS = new ArrayList<>();

    private NotificationManager() {
    }

    public static void show(Notification notification) {
        if (notification == null) return;

        if (notification.uniqueId != null) {
            NOTIFICATIONS.removeIf(n ->
                    notification.uniqueId.equals(n.uniqueId) && !n.isDismissed()
            );
        }

        NOTIFICATIONS.add(notification);
    }

    public static void tick(float deltaSeconds) {
        Minecraft mc = Minecraft.getInstance();

        Iterator<Notification> it = NOTIFICATIONS.iterator();

        while (it.hasNext()) {
            Notification n = it.next();

            n.tick(deltaSeconds, mc.getWindow().getGuiScaledWidth());

            if (n.isDismissed()) {
                it.remove();
            }
        }
    }

    //? if >=26 {
    public static void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
    //? } else {
    /*public static void render(GuiGraphics graphics, int mouseX, int mouseY) {*/
    //? }
        Minecraft mc = Minecraft.getInstance();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int y = screenHeight - 6;

        for (int i = NOTIFICATIONS.size() - 1; i >= 0; i--) {
            Notification n = NOTIFICATIONS.get(i);
            y -= n.getRenderedHeight();
            n.screenY = y;
            n.render(graphics, mouseX, mouseY);
            y -= 4;
        }
    }
}