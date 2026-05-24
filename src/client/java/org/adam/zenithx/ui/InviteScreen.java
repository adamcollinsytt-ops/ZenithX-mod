package org.adam.zenithx.ui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.adam.zenithx.FriendManager;
import org.adam.zenithx.HostClient;
import org.adam.zenithx.ui.components.MenuButton;

import java.util.List;

public class InviteScreen extends Screen {

    private final Screen parent;

    public InviteScreen(Screen parent) {
        super(Component.literal("Invites"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int cx = width / 2;
        int cy = height / 2;

        List<String> invites = FriendManager.getInstance().getPendingInvites();

        if (invites.isEmpty()) {
            addRenderableWidget(new MenuButton(
                    cx - 75, cy, 150, 20,
                    Component.literal("No invites"),
                    btn -> {}
            ));
        } else {
            int y = cy - 40;

            for (String from : invites) {
                addRenderableWidget(new MenuButton(
                        cx - 100, y, 200, 20,
                        Component.literal("Accept " + from),
                        MenuButton.LIGHT_GREEN, MenuButton.GREEN,
                        MenuButton.DISABLED, true,
                        btn -> {
                            HostClient.getInstance().acceptInvite(from);
                            FriendManager.getInstance().removePendingInvite(from);
                            minecraft.setScreen(null);
                        }
                ));
                y += 24;
            }
        }

        addRenderableWidget(new MenuButton(
                cx - 50, cy + 80, 100, 20,
                Component.literal("Back"),
                btn -> minecraft.setScreen(parent)
        ));
    }
}