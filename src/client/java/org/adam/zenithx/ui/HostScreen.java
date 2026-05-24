package org.adam.zenithx.ui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.adam.zenithx.FriendManager;
import org.adam.zenithx.HostMain;
import org.adam.zenithx.ui.components.MenuButton;

public class HostScreen extends HostBaseScreen {

    public HostScreen() {
        super(Component.literal("ZenithX"));
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;

        MenuButton infoBtn = new MenuButton(
                cx - 100, cy - 40, 200, 20,
                Component.literal("§bZenithX §7- §f" + (HostMain.getCurrentUsername() != null
                        ? HostMain.getCurrentUsername()
                        : "Connecting...")),
                MenuButton.DARK_GRAY, MenuButton.DARK_GRAY,
                MenuButton.DARK_GRAY, false,
                btn -> {}
        );
        infoBtn.active = false;
        this.addRenderableWidget(infoBtn);

        int friendCount = FriendManager.getInstance().getFriends().size();
        this.addRenderableWidget(new MenuButton(
                cx - 105, cy - 10, 100, 20,
                Component.literal("Friends (" + friendCount + ")"),
                btn -> this.minecraft.setScreen(new FriendsScreen(this))
        ));

        int invites = FriendManager.getInstance().getPendingInvites().size();
        this.addRenderableWidget(new MenuButton(
                cx + 5, cy - 10, 100, 20,
                Component.literal("Invites" + (invites > 0 ? " (" + invites + ")" : "")),
                invites > 0 ? MenuButton.LIGHT_BLUE : MenuButton.DARK_GRAY,
                invites > 0 ? MenuButton.BLUE       : MenuButton.GRAY,
                MenuButton.DISABLED, true,
                btn -> this.minecraft.setScreen(new InviteScreen(this))
        ));

        // زر Close
        this.addRenderableWidget(new MenuButton(
                cx - 50, cy + 20, 100, 20,
                Component.literal("Close"),
                btn -> this.onClose()
        ));
    }

    @Override
    public boolean isPauseScreen() { return false; }
}