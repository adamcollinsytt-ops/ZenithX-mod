package org.adam.zenithx.mixin;

import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.adam.zenithx.HostClient;
import org.adam.zenithx.ui.FriendsScreen;
import org.adam.zenithx.ui.HostWorldScreen;
import org.adam.zenithx.ui.AddFriendScreen;
import org.adam.zenithx.ui.components.MenuButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin() {
        super(Component.literal(""));
    }

    @Inject(method = "init()V", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (this.minecraft.level != null) return;

        int rightX = this.width - 110;
        int startY = this.height / 4 + 48;
        int offset = 0;

        this.addRenderableWidget(new MenuButton(
                rightX, startY + offset,
                100, 20,
                Component.literal("Host World"),
                btn -> this.minecraft.setScreen(new HostWorldScreen())
        ).setIcon(Identifier.fromNamespaceAndPath("zenithx", "textures/icons/world_8x8.png"), 0));
        offset += 24;

        this.addRenderableWidget(new MenuButton(
                rightX, startY + offset,
                100, 20,
                Component.literal("Social"),
                btn -> this.minecraft.setScreen(new AddFriendScreen(null))
        ).setIcon(Identifier.fromNamespaceAndPath("zenithx", "textures/icons/social.png"), 0));

        Component creditsText = Component.literal("By Adam_CollinsYT, ALKRKY99, xek")
                .withStyle(style -> style.withColor(ARGB.white(1.0F)));
        int textWidth = this.font.width(creditsText);

        this.addRenderableOnly(new StringWidget(
                2,
                this.height - 20,
                textWidth,
                10,
                creditsText,
                this.font
        ));
    }
}