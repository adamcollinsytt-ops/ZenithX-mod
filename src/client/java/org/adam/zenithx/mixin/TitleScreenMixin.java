package org.adam.zenithx.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import org.adam.zenithx.HostClient;
import org.adam.zenithx.ui.FriendsScreen;
import org.adam.zenithx.ui.HostScreen;
import org.adam.zenithx.ui.NoConnectionWarningScreen;
import org.adam.zenithx.ui.components.MenuButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin() {
        super(Component.literal(""));
    }

    private static boolean connectingInProgress = false;

    @Inject(method = "init()V", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (this.minecraft.level != null) return;

        int rightX = this.width - 110;
        int startY = this.height / 4 + 48;

        this.addRenderableWidget(new MenuButton(
                rightX, startY, 100, 20,
                Component.literal("Host World"),
                btn -> this.minecraft.setScreen(new HostScreen())
        ));

        this.addRenderableWidget(new MenuButton(
                rightX, startY + 24, 100, 20,
                Component.literal("Social"),
                btn -> this.minecraft.setScreen(new FriendsScreen(null))
        ));

        this.addRenderableWidget(new MenuButton(
                rightX, startY + 48, 100, 20,
                Component.literal("ZenithX"),
                btn -> this.minecraft.setScreen(new HostScreen())
        ));
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onExtractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        graphics.text(
                this.font,
                "By Adam_CollinsYT, ALKRKY99, xek",
                2,
                this.height - 20,
                ARGB.white(1.0F),
                true
        );

        System.out.println("test");
    }
}