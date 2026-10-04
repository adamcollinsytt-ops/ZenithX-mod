package org.adam.zenithx.skins;

public enum SkinType {
    DEFAULT("Default", 64, 64),

    CLASSIC("Classic", 64, 64),

    LEGACY("Legacy", 64, 32),

    CUSTOM("Custom", 0, 0);

    private final String displayName;
    private final int width;
    private final int height;

    SkinType(String displayName, int width, int height) {
        this.displayName = displayName;
        this.width = width;
        this.height = height;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public static SkinType fromDimensions(int width, int height) {
        if (width == 64 && height == 64) {
            return CLASSIC;
        } else if (width == 64 && height == 32) {
            return LEGACY;
        } else {
            return CUSTOM;
        }
    }

    public boolean isValidDimensions(int width, int height) {
        if (this == CUSTOM) {
            return true;
        }
        return this.width == width && this.height == height;
    }
}
