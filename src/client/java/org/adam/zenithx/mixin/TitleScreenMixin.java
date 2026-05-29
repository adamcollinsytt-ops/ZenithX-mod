package org.adam.zenithx.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.adam.zenithx.HostClient;
import org.adam.zenithx.ui.FriendsScreen;
import org.adam.zenithx.ui.HostScreen;
import org.adam.zenithx.ui.components.MenuButton;
import org.adam.zenithx.ui.notification.Notification;
import org.adam.zenithx.ui.notification.NotificationManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {

    protected TitleScreenMixin() {
        super(Component.literal(""));
    }

    private static boolean updateChecked = false;
    private static boolean updateRequired = false;

    @Inject(method = "init()V", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        if (this.minecraft.level != null) return;

        HostClient.updateCallback = (serverVersion) -> {
            String client = net.fabricmc.loader.api.FabricLoader.getInstance()
                    .getModContainer("zenithx")
                    .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                    .orElse("1.0.0");

            if (!serverVersion.equals(client)) {
                updateRequired = true;
                updateChecked = true;
                Minecraft mc = Minecraft.getInstance();
                if (mc.screen instanceof TitleScreen) {
                    mc.setScreen(new TitleScreen(false, null));
                }
            }
        };

        checkUpdate();

        int rightX = this.width - 110;
        int startY = this.height / 4 + 48;
        int offset = 0;

        if (updateRequired) {
            this.addRenderableWidget(new MenuButton(
                    rightX, 10,
                    100, 20,
                    Component.literal("Update"),
                    btn -> {
                        com.google.gson.JsonObject o = new com.google.gson.JsonObject();
                        o.addProperty("type", "UPDATE_ACCEPT");
                        HostClient.getInstance().sendJson(o);
                    }
            ).setIcon(Identifier.fromNamespaceAndPath("zenithx", "icons/update_500x500"), -1, 20));
        }

        this.addRenderableWidget(new MenuButton(
                rightX, startY + offset,
                100, 20,
                Component.literal("Host World"),
                btn -> this.minecraft.setScreen(new HostScreen())
        ).setIcon(Identifier.fromNamespaceAndPath("zenithx", "icons/world_8x8"), 0));
        offset += 24;

        this.addRenderableWidget(new MenuButton(
                rightX, startY + offset,
                100, 20,
                Component.literal("Social"),
                btn -> this.minecraft.setScreen(new FriendsScreen(null))
        ));
        offset += 24;

        this.addRenderableWidget(new MenuButton(
                rightX, startY + offset,
                100, 20,
                Component.literal("ZenithX"),
                btn -> this.minecraft.setScreen(new HostScreen())
        ));

        Component creditsText = Component.literal("By Adam_CollinsYT, ALKRKY99, xek").withStyle(style -> style.withColor(ARGB.white(1.0F)));
        int textWidth = this.font.width(creditsText);

        StringWidget creditsWidget = new StringWidget(
                2,
                this.height - 20,
                textWidth,
                10,
                creditsText,
                this.font
        );
        this.addRenderableOnly(creditsWidget);
    }

    private void checkUpdate() {
        if (updateChecked) return;
        updateChecked = true;

        String client = net.fabricmc.loader.api.FabricLoader.getInstance()
                .getModContainer("zenithx")
                .map(mod -> mod.getMetadata().getVersion().getFriendlyString())
                .orElse("1.0.0");
        String server = HostClient.getServerVersion();

        System.out.println("[ZenithX] Client version: " + client);
        System.out.println("[ZenithX] Server version: " + server);

        if (server != null && !server.equals(client)) {
            updateRequired = true;
            NotificationManager.show(new Notification(
                    "Update Available",
                    "New version detected",
                    6f,
                    true,
                    "update",
                    () -> true,
                    () -> {},
                    () -> {},
                    null,
                    null
            ));
        }
    }
}