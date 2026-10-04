package org.adam.zenithx.mixin;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.player.Player;

import org.adam.zenithx.handlers.TagManager;
import org.adam.zenithx.handlers.ZenithXClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Mixin(Player.class)
public abstract class PlayerTagMixin {

    @Unique
    private static final String ROLES_URL = "https://gist.githubusercontent.com/adamcollinsytt-ops/949fb50461b36bca39b87f9a3c019531/raw/roles.json";

    @Unique
    private static final Map<String, RoleData> PLAYER_ROLES = new HashMap<>();

    @Unique
    private static boolean isThreadStarted = false;

    @Unique
    private static class RoleData {
        String role;
        String color;
    }

    @Inject(method = "getDisplayName", at = @At("RETURN"), cancellable = true)
    private void zenithx$addCustomPrefix(CallbackInfoReturnable<Component> cir) {
        if (!isThreadStarted) {
            isThreadStarted = true;
            startRolesAutoUpdater();
        }

        Player player = (Player) (Object) this;

        if (!ZenithXClient.isTagVisibleForPlayer(player.getUUID())) {
            return;
        }

        String playerName = player.getScoreboardName();

        synchronized (PLAYER_ROLES) {
            if (PLAYER_ROLES.containsKey(playerName)) {
                RoleData data = PLAYER_ROLES.get(playerName);

                int colorHex = 0xFFFF55;
                try {
                    colorHex = Integer.decode(data.color);
                } catch (Exception ignored) {}

                MutableComponent prefix = Component.literal("[" + data.role + " of ZenithX Mod] ")
                        .setStyle(Style.EMPTY.withColor(colorHex));

                MutableComponent whiteName = Component.literal(playerName)
                        .setStyle(Style.EMPTY.withColor(0xFFFFFF));

                cir.setReturnValue(prefix.append(whiteName));
            }
        }
    }

    @Unique
    private static void startRolesAutoUpdater() {
        Thread thread = new Thread(() -> {
            while (true) {
                try {
                    String urlWithCacheBust = ROLES_URL + "?t=" + System.currentTimeMillis();

                    URLConnection connection = URI.create(urlWithCacheBust).toURL().openConnection();
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 ZenithX-Mod");
                    connection.setUseCaches(false);

                    try (InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                        Type type = new TypeToken<Map<String, RoleData>>() {}.getType();
                        Map<String, RoleData> downloadedData = new Gson().fromJson(reader, type);

                        if (downloadedData != null) {
                            synchronized (PLAYER_ROLES) {
                                PLAYER_ROLES.clear();
                                PLAYER_ROLES.putAll(downloadedData);
                            }
                        }
                    }

                    Thread.sleep(300000); 
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    try {
                        Thread.sleep(10000);
                    } catch (InterruptedException ignored) {}
                }
            }
        });

        thread.setDaemon(true);
        thread.setName("ZenithX-Roles-Updater");
        thread.start();
    }
}