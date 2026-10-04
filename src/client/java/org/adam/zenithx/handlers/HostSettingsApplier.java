package org.adam.zenithx.handlers;

import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.adam.zenithx.HostSessionConfig;

public class HostSettingsApplier {

    public static void register() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();

            if (!isSingleplayerOwner(server, player)) return;
            if (!HostSessionConfig.hasPendingApply()) return;

            server.execute(() -> applyNow(server, player));
        });
    }

    private static void applyNow(MinecraftServer server, ServerPlayer player) {
        server.setDefaultGameType(HostSessionConfig.getGameMode());
        server.setDifficulty(HostSessionConfig.getDifficulty(), true);
        server.getPlayerList().setAllowCommandsForAllPlayers(false);

        String hostName = player.getScoreboardName();
        var source = server.createCommandSourceStack().withSuppressedOutput();
        String cmd = HostSessionConfig.hasCheats() ? "op " : "deop ";
        server.getCommands().performPrefixedCommand(source, cmd + hostName);

        server.getPlayerList().sendPlayerPermissionLevel(player);

        HostSessionConfig.clearPendingApply();

        System.out.println("[ZenithX] Host settings applied: cheats="
                + HostSessionConfig.hasCheats()
                + " gameMode=" + HostSessionConfig.getGameMode()
                + " difficulty=" + HostSessionConfig.getDifficulty());
    }

    private static boolean isSingleplayerOwner(MinecraftServer server, ServerPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getUUID().equals(player.getUUID());
    }
}