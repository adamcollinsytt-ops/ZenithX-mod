package org.adam.zenithx.ui;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class HostBaseScreen extends Screen {

    protected HostBaseScreen(Component title) {
        super(title);
    }

    protected int cx() { return this.width  / 2; }
    protected int cy() { return this.height / 2; }
}