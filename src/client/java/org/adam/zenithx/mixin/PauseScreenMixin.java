package org.adam.zenithx.mixin;

import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.adam.zenithx.ui.HostBaseScreen;
import org.adam.zenithx.ui.FriendsScreen;
import org.adam.zenithx.ui.HostScreen;
import org.adam.zenithx.ui.WorldSettingsScreen;
import org.adam.zenithx.ui.components.MenuButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
public class PauseScreenMixin extends net.minecraft.client.gui.screens.Screen {

    protected PauseScreenMixin() {
        super(Component.empty());
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        int rightX  = this.width - 110;
        int startY  = this.height / 4 - 16;

        this.addRenderableWidget(new MenuButton(
                rightX, startY, 100, 20,
                Component.literal("Invite"),
                btn -> this.minecraft.setScreen(new WorldSettingsScreen(null, null))
        ).setIcon(Identifier.fromNamespaceAndPath("zenithx", "textures/icons/friendplus.png"), 1));

        this.addRenderableWidget(new MenuButton(
                rightX, startY + 24, 100, 20,
                Component.literal("Social"),
                btn -> this.minecraft.setScreen(new FriendsScreen((HostBaseScreen) (Object) this))
        ).setIcon(Identifier.fromNamespaceAndPath("zenithx", "textures/icons/social.png"), 1));
    }
}