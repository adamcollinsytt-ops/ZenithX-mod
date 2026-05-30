package org.adam.zenithx;

import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameType;

public class HostSessionConfig {

    private static GameType gameMode = GameType.SURVIVAL;
    private static Difficulty difficulty = Difficulty.NORMAL;
    private static boolean cheats = false;
    private static boolean shareRP = false;

    public static void setGameMode(GameType gm) {
        gameMode = gm;
    }

    public static GameType getGameMode() {
        return gameMode;
    }

    public static void setDifficulty(Difficulty diff) {
        difficulty = diff;
    }

    public static Difficulty getDifficulty() {
        return difficulty;
    }

    public static void setCheats(boolean value) {
        cheats = value;
    }

    public static boolean hasCheats() {
        return cheats;
    }

    public static void setShareRP(boolean value) {
        shareRP = value;
    }

    public static boolean isShareRP() {
        return shareRP;
    }
}