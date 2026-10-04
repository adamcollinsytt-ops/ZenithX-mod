package org.adam.zenithx.mixin;

//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
//? }
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
//? if >=26 {
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
//? } else {
/*import net.minecraft.resources.ResourceLocation;*/
//? }

import org.adam.zenithx.DevelopmentShared;
import org.adam.zenithx.HostClient;
import org.adam.zenithx.ui.FriendsScreen;
import org.adam.zenithx.ui.HostWorldScreen;
import org.adam.zenithx.ui.SwitchScreen;
import org.adam.zenithx.ui.AddFriendScreen;
import org.adam.zenithx.ui.components.MenuButton;
import org.adam.zenithx.ui.components.SkinMenuButton;
import org.adam.zenithx.ui.SkinCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin() {
        super(Component.literal(""));
    }

    private int interpolateColor(int color1, int color2, float ratio) {
        int a1 = (color1 >> 24) & 0xFF;
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;

        int a2 = (color2 >> 24) & 0xFF;
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;

        int a = (int) (a1 + ratio * (a2 - a1));
        int r = (int) (r1 + ratio * (r2 - r1));
        int g = (int) (g1 + ratio * (g2 - g1));
        int b = (int) (b1 + ratio * (b2 - b1));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    //? if <26 {
    /*private static int argbColor(int a, int r, int g, int b) {
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int argbWhiteWithAlpha(float alpha) {
        int a = (int) (alpha * 255.0F);
        return (a << 24) | 0xFFFFFF;
    }*/
    //? }

    @Inject(method = "init()V", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (this.minecraft.level != null) return;
        
        String playerName = this.minecraft.getUser().getName();
        SkinCache.request(playerName);

        int rightX = this.width - 110;
        int leftX = 10;
        int startY = this.height / 4 + 48;
        int offset = 0;

        this.addRenderableWidget(new MenuButton(
                rightX, startY + offset,
                100, 20,
                Component.literal("Host World"),
                btn -> this.minecraft.setScreen(new HostWorldScreen())
        //? if >=26 {
        ).setIcon(Identifier.fromNamespaceAndPath("zenithx", "textures/icons/world_8x8.png"), 0));
        //? } else if >=1.20.5 {
        /*).setIcon(ResourceLocation.fromNamespaceAndPath("zenithx", "textures/icons/world_8x8.png"), 0));*/
        //? } else {
        /*).setIcon(new ResourceLocation("zenithx", "textures/icons/world_8x8.png"), 0));*/
        //? }
        offset += 24;

        this.addRenderableWidget(new MenuButton(
                rightX, startY + offset,
                100, 20,
                Component.literal("Social"),
                btn -> this.minecraft.setScreen(new FriendsScreen(null))
        //? if >=26 {
        ).setIcon(Identifier.fromNamespaceAndPath("zenithx", "textures/icons/social.png"), 0));
        //? } else if >=1.20.5 {
        /*).setIcon(ResourceLocation.fromNamespaceAndPath("zenithx", "textures/icons/social.png"), 0));*/
        //? } else {
        /*).setIcon(new ResourceLocation("zenithx", "textures/icons/social.png"), 0));*/
        //? }

        this.addRenderableWidget(new SkinMenuButton(
                leftX, startY,
                20, 20,
                playerName,
                -1, // Location
                btn -> this.minecraft.setScreen(new SwitchScreen(null))
        ));

        if ("Mainzo".equals(playerName)) { // BlackListed (People)
            throw new RuntimeException("Access Denied: You are blacklisted from ZenithX Mod!");
        }

        if (DevelopmentShared.isDev(playerName)) { 
            
            String text = "Development Mode";
            MutableComponent devModeText = Component.empty();

            int colorStart = 0xFFFF7700; 
            int colorEnd = 0xFFFFD700;   

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                float ratio = text.length() > 1 ? (float) i / (text.length() - 1) : 0;
                int interpolatedColor = interpolateColor(colorStart, colorEnd, ratio);

                int a = (interpolatedColor >> 24) & 0xFF;
                int r = (interpolatedColor >> 16) & 0xFF;
                int g = (interpolatedColor >> 8) & 0xFF;
                int b = interpolatedColor & 0xFF;

                //? if >=26 {
                MutableComponent charComponent = Component.literal(String.valueOf(c))
                        .withStyle(style -> style.withColor(ARGB.color(a, r, g, b)));
                //? } else {
                /*int packedColor = argbColor(a, r, g, b);
                MutableComponent charComponent = Component.literal(String.valueOf(c))
                        .withStyle(style -> style.withColor(packedColor));*/
                //? }
                
                devModeText.append(charComponent);
            }

            int devTextWidth = this.font.width(devModeText);

            this.addRenderableOnly(new StringWidget(
                    2,
                    4,
                    devTextWidth,
                    10,
                    devModeText,
                    this.font
            ));
        }

        //? if >=26 {
        Component creditsText = Component.literal("By Adam_CollinsYT, xekek")
                .withStyle(style -> style.withColor(ARGB.white(1.0F)));
        //? } else {
        /*Component creditsText = Component.literal("By Adam_CollinsYT, xekek")
                .withStyle(style -> style.withColor(argbWhiteWithAlpha(1.0F)));*/
        //? }
        int textWidth = this.font.width(creditsText);

        this.addRenderableOnly(new StringWidget(
                2,
                this.height - 20,
                textWidth,
                10,
                creditsText,
                this.font
        ));

        org.adam.zenithx.HostClient clientInstance = org.adam.zenithx.HostClient.getInstance();

        String lastReason = org.adam.zenithx.HostClient.getLastFailReason();
        boolean isAlreadyOnline = lastReason != null && 
                (lastReason.toLowerCase().contains("already_online") || lastReason.toLowerCase().contains("already logged in"));

        if (!org.adam.zenithx.HostClient.USER_DISMISSED && 
            (clientInstance == null || (!clientInstance.isOpen() && org.adam.zenithx.handlers.ZenithXClient.connectionChecked))) {
            
            if (!isAlreadyOnline && !(this.minecraft.screen instanceof org.adam.zenithx.ui.NoConnectionWarningScreen)) {
                this.minecraft.execute(() -> {
                    this.minecraft.setScreen(new org.adam.zenithx.ui.NoConnectionWarningScreen(
                        () -> this.minecraft.setScreen(new TitleScreen()),
                        () -> org.adam.zenithx.HostClient.tryReconnectNow(),
                        org.adam.zenithx.HostClient.getLastFailReason()
                    ));
                });
            }
        }
    }
}