package org.adam.zenithx.ui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
//? } else {
/*import net.minecraft.client.gui.GuiGraphics;*/
//? }
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
//? if >=26 {
import net.minecraft.resources.Identifier;
//? } else {
/*import net.minecraft.resources.ResourceLocation;*/
//? }
import org.adam.zenithx.ui.components.MenuButton;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class FriendRequestNotification extends Screen {
    private static final int NOTIFICATION_WIDTH = 300;
    private static final int NOTIFICATION_HEIGHT = 150;
    
    private final String playerName;
    private final UUID playerUuid;
    private final Runnable onAccept;
    private final Runnable onReject;
    
    //? if >=26 {
    private Identifier skinTexture;
    //? } else {
    /*private ResourceLocation skinTexture;*/
    //? }
    private boolean skinLoaded = false;

    public FriendRequestNotification(String playerName, UUID playerUuid, Runnable onAccept, Runnable onReject) {
        super(Component.literal("Friend request"));
        this.playerName = playerName;
        this.playerUuid = playerUuid;
        this.onAccept = onAccept;
        this.onReject = onReject;
        
        loadPlayerSkin();
    }

    private void loadPlayerSkin() {
        new Thread(() -> {
            try {
                String skinUrl = String.format("https://crafatar.com/avatars/%s?size=64&overlay", playerUuid.toString());
                URL url = new URL(skinUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                InputStream inputStream = connection.getInputStream();
                NativeImage image = NativeImage.read(inputStream);
                
                Minecraft.getInstance().execute(() -> {
                    //? if >=26 {
                    skinTexture = Identifier.fromNamespaceAndPath("zenithx", "friend_request/" + playerUuid.toString());
                    //? } else if >=1.20.5 {
                    /*skinTexture = ResourceLocation.fromNamespaceAndPath("zenithx", "friend_request/" + playerUuid.toString());*/
                    //? } else {
                    /*skinTexture = new ResourceLocation("zenithx", "friend_request/" + playerUuid.toString());*/
                    //? }
                    //? if >=1.20.5 {
                    DynamicTexture texture = new DynamicTexture(() -> "friend_skin:" + playerName, image);
                    //? } else {
                    /*DynamicTexture texture = new DynamicTexture(image);*/
                    //? }
                    Minecraft.getInstance().getTextureManager().register(skinTexture, texture);
                    skinLoaded = true;
                });
                
                inputStream.close();
            } catch (IOException e) {
                System.err.println("[ZenithX] Failed to load player skin: " + e.getMessage());
            }
        }, "zenithx-skin-loader").start();
    }

    @Override
    protected void init() {
        super.init();
        
        int centerX = (width - NOTIFICATION_WIDTH) / 2;
        int centerY = (height - NOTIFICATION_HEIGHT) / 2;
        
        this.addRenderableWidget(
                new MenuButton(
                        centerX + NOTIFICATION_WIDTH / 2 - 80,
                        centerY + NOTIFICATION_HEIGHT - 40,
                        70, 25,
                        Component.literal("✓"),
                        MenuButton.GREEN, MenuButton.LIGHT_GREEN, MenuButton.DISABLED,
                        true,
                        button -> accept()
                )
        );
        
        this.addRenderableWidget(
                new MenuButton(
                        centerX + NOTIFICATION_WIDTH / 2 + 10,
                        centerY + NOTIFICATION_HEIGHT - 40,
                        70, 25,
                        Component.literal("✗"),
                        MenuButton.RED, MenuButton.LIGHT_RED, MenuButton.DISABLED,
                        true,
                        button -> reject()
                )
        );
    }

    //? if >=26 {
    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float delta) {
    //? } else {
    /*@Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float delta) {*/
    //? }
        int centerX = (width - NOTIFICATION_WIDTH) / 2;
        int centerY = (height - NOTIFICATION_HEIGHT) / 2;
        
        gfx.fill(0, 0, width, height, 0x80000000);
        
        gfx.fill(centerX, centerY, centerX + NOTIFICATION_WIDTH, centerY + NOTIFICATION_HEIGHT, 0xEE1A1A1A);
        
        gfx.fill(centerX, centerY, centerX + NOTIFICATION_WIDTH, centerY + 2, 0xFF3399FF);
        gfx.fill(centerX, centerY + NOTIFICATION_HEIGHT - 2, centerX + NOTIFICATION_WIDTH, centerY + NOTIFICATION_HEIGHT, 0xFF3399FF);
        gfx.fill(centerX, centerY, centerX + 2, centerY + NOTIFICATION_HEIGHT, 0xFF3399FF);
        gfx.fill(centerX + NOTIFICATION_WIDTH - 2, centerY, centerX + NOTIFICATION_WIDTH, centerY + NOTIFICATION_HEIGHT, 0xFF3399FF);
        
        String title = "New friend request!";
        int titleWidth = font.width(title);
        //? if >=26 {
        gfx.text(
        //? } else {
        /*gfx.drawString(*/
        //? }
                font,
                Component.literal(title),
                centerX + (NOTIFICATION_WIDTH - titleWidth) / 2,
                centerY + 15,
                0x3399FF,
                true
        );
        
        int skinSize = 64;
        int skinX = centerX + (NOTIFICATION_WIDTH - skinSize) / 2;
        int skinY = centerY + 40;
        
        if (skinLoaded && skinTexture != null) {
            //? if >=26 {
            gfx.blit(
                    RenderPipelines.GUI_TEXTURED,
                    skinTexture,
                    skinX, skinY,
                    0, 0,
                    skinSize, skinSize,
                    skinSize, skinSize
            );
            //? } else {
            /*gfx.blit(
                    skinTexture,
                    skinX, skinY,
                    0, 0,
                    skinSize, skinSize,
                    skinSize, skinSize
            );*/
            //? }
        } else {
            gfx.fill(skinX, skinY, skinX + skinSize, skinY + skinSize, 0xFF333333);
            String loadingText = "...";
            int loadingWidth = font.width(loadingText);
            //? if >=26 {
            gfx.text(
            //? } else {
            /*gfx.drawString(*/
            //? }
                    font,
                    Component.literal(loadingText),
                    skinX + (skinSize - loadingWidth) / 2,
                    skinY + (skinSize - 8) / 2,
                    0xAAAAAA,
                    false
            );
        }
        
        String nameText = playerName;
        int nameWidth = font.width(nameText);
        //? if >=26 {
        gfx.text(
        //? } else {
        /*gfx.drawString(*/
        //? }
                font,
                Component.literal(nameText),
                centerX + (NOTIFICATION_WIDTH - nameWidth) / 2,
                skinY + skinSize + 10,
                0xFFFFFF,
                true
        );
        
        String questionText = "Do you want to accept the friend request?";
        int questionWidth = font.width(questionText);
        //? if >=26 {
        gfx.text(
        //? } else {
        /*gfx.drawString(*/
        //? }
                font,
                Component.literal(questionText),
                centerX + (NOTIFICATION_WIDTH - questionWidth) / 2,
                centerY + NOTIFICATION_HEIGHT - 55,
                0xAAAAAA,
                false
        );
        
        //? if >=26 {
        super.extractRenderState(gfx, mouseX, mouseY, delta);
        //? } else {
        /*super.render(gfx, mouseX, mouseY, delta);*/
        //? }
    }

    private void accept() {
        if (onAccept != null) {
            onAccept.run();
        }
        onClose();
    }

    private void reject() {
        if (onReject != null) {
            onReject.run();
        }
        onClose();
    }

    @Override
    public void onClose() {
        if (skinTexture != null) {
            Minecraft.getInstance().execute(() -> {
                try {
                    Minecraft.getInstance().getTextureManager().release(skinTexture);
                } catch (Exception ignored) {}
            });
        }
        
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public static void show(String playerName, UUID playerUuid, Runnable onAccept, Runnable onReject) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft != null) {
            minecraft.execute(() -> {
                minecraft.setScreen(new FriendRequestNotification(playerName, playerUuid, onAccept, onReject));
            });
        }
    }
}
