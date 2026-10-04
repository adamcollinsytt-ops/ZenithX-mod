package org.adam.zenithx.ui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
//? if >=26 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
//? } else {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;*/
//? }
//? if >=1.21.9 {
import net.minecraft.client.renderer.RenderPipelines;
//? }
import net.minecraft.network.chat.Component;
import org.adam.zenithx.ui.SkinCache;

import java.util.function.Consumer;

public class SkinMenuButton extends MenuButton {
    //? if >=26 {
    private static final Identifier FALLBACK_ICON = Identifier.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");
    //? } else if >=1.20.5 {
    /*private static final ResourceLocation FALLBACK_ICON = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/entity/player/wide/steve.png");*/
    //? } else {
    /*private static final ResourceLocation FALLBACK_ICON = new ResourceLocation("minecraft", "textures/entity/player/wide/steve.png");*/
    //? }

    private final String playerName;
    private Component customMessage = Component.empty();
    private int align = 0;

    private static final int HEAD_SIZE = 12;
    private static final int ICON_SPACING = 4;

    public SkinMenuButton(int x, int y, int width, int height, String playerName, Consumer<MenuButton> onPress) {
        this(x, y, width, height, playerName, Component.empty(), 0, onPress);
    }

    public SkinMenuButton(int x, int y, int width, int height, String playerName, int align, Consumer<MenuButton> onPress) {
        this(x, y, width, height, playerName, Component.empty(), align, onPress);
    }

    public SkinMenuButton(int x, int y, int width, int height, String playerName, Component message, int align, Consumer<MenuButton> onPress) {
        super(x, y, width, height, Component.empty(), onPress);
        this.customMessage = message;
        this.playerName = playerName;
        this.align = align;
    }

    public SkinMenuButton(int x, int y, int width, int height, Component message, String playerName, int align, Consumer<MenuButton> onPress) {
        this(x, y, width, height, playerName, message, align, onPress);
    }

    public SkinMenuButton setAlign(int align) {
        this.align = align;
        return this;
    }

    //? if >=26 {
    @Override
    protected void extractContents(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float delta) {
    //? } else {
    /*@Override
    protected void renderWidget(GuiGraphics gfx, int mouseX, int mouseY, float delta) {*/
    //? }
        //? if >=26 {
        super.extractContents(gfx, mouseX, mouseY, delta);
        //? } else {
        /*super.renderWidget(gfx, mouseX, mouseY, delta);*/
        //? }

        Font font = Minecraft.getInstance().font;
        Component msg = this.customMessage;
        boolean hasText = msg != null && !msg.getString().isEmpty();

        int headX;
        int headY = this.getY() + (this.getHeight() - HEAD_SIZE) / 2;
        int textX = this.getX() + this.getWidth() / 2;
        int textY = this.getY() + (this.getHeight() - 8) / 2;

        if (hasText) {
            int textWidth = font.width(msg);

            switch (this.align) {
                case 0 -> {
                    int totalContentWidth = textWidth + ICON_SPACING + HEAD_SIZE;
                    int startX = this.getX() + (this.getWidth() - totalContentWidth) / 2;
                    headX = startX;
                    textX = startX + HEAD_SIZE + ICON_SPACING + (textWidth / 2);
                }
                case 1 -> {
                    int totalContentWidth = textWidth + ICON_SPACING + HEAD_SIZE;
                    int startX = this.getX() + (this.getWidth() - totalContentWidth) / 2;
                    textX = startX + (textWidth / 2);
                    headX = startX + textWidth + ICON_SPACING;
                }
                case -1 -> {
                    headX = this.getX() + 4;
                    int availableWidth = this.getWidth() - (HEAD_SIZE + 8);
                    textX = this.getX() + HEAD_SIZE + 4 + (availableWidth / 2);
                }
                default -> {
                    int totalContentWidth = textWidth + ICON_SPACING + HEAD_SIZE;
                    int startX = this.getX() + (this.getWidth() - totalContentWidth) / 2;
                    headX = startX;
                    textX = startX + HEAD_SIZE + ICON_SPACING + (textWidth / 2);
                }
            }
        } else {
            switch (this.align) {
                case 0 -> headX = this.getX() + (this.getWidth() - HEAD_SIZE) / 2;
                case 1 -> headX = this.getX() + this.getWidth() - HEAD_SIZE - 4;
                case -1 -> headX = this.getX() + 4;
                default -> headX = this.getX() + (this.getWidth() - HEAD_SIZE) / 2;
            }
        }

        //? if >=26 {
        Identifier skin = SkinCache.get(this.playerName);
        //? } else {
        /*ResourceLocation skin = SkinCache.get(this.playerName);*/
        //? }
        if (skin != null) {
            //? if >=1.21.9 {
            gfx.blit(RenderPipelines.GUI_TEXTURED, skin, headX, headY, 8.0F, 8.0F, HEAD_SIZE, HEAD_SIZE, 8, 8, 64, 64);
            gfx.blit(RenderPipelines.GUI_TEXTURED, skin, headX, headY, 40.0F, 8.0F, HEAD_SIZE, HEAD_SIZE, 8, 8, 64, 64);
            //? } else if >=1.20.5 {
            /*gfx.blit(skin, headX, headY, 8.0F, 8.0F, HEAD_SIZE, HEAD_SIZE, 8, 8, 64, 64);
            gfx.blit(skin, headX, headY, 40.0F, 8.0F, HEAD_SIZE, HEAD_SIZE, 8, 8, 64, 64);*/
            //? } else {
            /*gfx.blit(skin, headX, headY, 8, 8, HEAD_SIZE, HEAD_SIZE, 8, 8, 64, 64);
            gfx.blit(skin, headX, headY, 40, 8, HEAD_SIZE, HEAD_SIZE, 8, 8, 64, 64);*/
            //? }
        } else {
            //? if >=1.21.9 {
            gfx.blit(RenderPipelines.GUI_TEXTURED, FALLBACK_ICON, headX, headY, 0.0F, 0.0F, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE);
            //? } else if >=1.20.5 {
            /*gfx.blit(FALLBACK_ICON, headX, headY, 0.0F, 0.0F, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE);*/
            //? } else {
            /*gfx.blit(FALLBACK_ICON, headX, headY, 0, 0, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE, HEAD_SIZE);*/
            //? }
        }

        if (hasText) {
            int textColor = this.active ? 0xFFFFFFFF : 0xFFA0A0A0;
            //? if >=26 {
            gfx.centeredText(font, msg, textX, textY, textColor);
            //? } else {
            /*gfx.drawCenteredString(font, msg, textX, textY, textColor);*/
            //? }
        }
    }
}
