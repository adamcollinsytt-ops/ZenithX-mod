package org.adam.zenithx.ui;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SkinCache {

    private static final String MOJANG_NAME =
            "https://api.mojang.com/users/profiles/minecraft/%s";

    private static final String MOJANG_PROFILE =
            "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=true";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ExecutorService POOL =
            Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "skin-loader");
                t.setDaemon(true);
                return t;
            });

    private static final Map<String, Identifier> READY =
            new ConcurrentHashMap<>();

    private static final Set<String> FETCHING =
            ConcurrentHashMap.newKeySet();

    public static void request(String username) {

        if (username == null || username.isBlank()) return;

        username = username.toLowerCase();

        if (READY.containsKey(username)) return;

        if (FETCHING.contains(username)) return;

        FETCHING.add(username);

        String finalUsername = username;

        POOL.submit(() -> fetchAndRegister(finalUsername));
    }

    public static void requestAll(Collection<String> names) {
        names.forEach(SkinCache::request);
    }

    public static Identifier get(String username) {

        if (username == null) return null;

        return READY.get(username.toLowerCase());
    }

    public static void clear() {

        READY.forEach((name, id) -> {

            try {
                Minecraft.getInstance()
                        .getTextureManager()
                        .release(id);
            } catch (Exception ignored) {
            }
        });

        READY.clear();
        FETCHING.clear();
    }

    private static void fetchAndRegister(String username) {

        try {

            username = username.toLowerCase();

            String uuid = fetchUUID(username);

            if (uuid == null) {
                FETCHING.remove(username);
                return;
            }

            String skinUrl = fetchSkinUrl(uuid);

            if (skinUrl == null) {
                FETCHING.remove(username);
                return;
            }

            NativeImage skin = download(skinUrl);

            if (skin == null) {
                FETCHING.remove(username);
                return;
            }

            NativeImage face = cropFace(skin);

            skin.close();

            String finalUsername = username;

            Minecraft.getInstance().execute(() -> {

                try {

                    Identifier id =
                            Identifier.fromNamespaceAndPath(
                                    "zenithx",
                                    "skin/" + finalUsername
                            );

                    DynamicTexture texture =
                            new DynamicTexture(
                                    () -> "face:" + finalUsername,
                                    face
                            );

                    Minecraft.getInstance()
                            .getTextureManager()
                            .register(id, texture);

                    READY.put(finalUsername, id);

                } catch (Exception e) {
                    e.printStackTrace();
                }

                FETCHING.remove(finalUsername);
            });

        } catch (Exception e) {

            FETCHING.remove(username);

            e.printStackTrace();
        }
    }

    private static String fetchUUID(String name) throws Exception {

        HttpResponse<String> response = HTTP.send(
                req(MOJANG_NAME.formatted(name)),
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) return null;

        JsonObject json =
                JsonParser.parseString(response.body()).getAsJsonObject();

        if (!json.has("id")) return null;

        return json.get("id").getAsString();
    }

    private static String fetchSkinUrl(String uuid) throws Exception {

        HttpResponse<String> response = HTTP.send(
                req(MOJANG_PROFILE.formatted(uuid)),
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) return null;

        JsonObject profile =
                JsonParser.parseString(response.body()).getAsJsonObject();

        if (!profile.has("properties")) return null;

        String value = profile
                .getAsJsonArray("properties")
                .get(0)
                .getAsJsonObject()
                .get("value")
                .getAsString();

        String decoded =
                new String(Base64.getDecoder().decode(value));

        JsonObject textures =
                JsonParser.parseString(decoded)
                        .getAsJsonObject()
                        .getAsJsonObject("textures");

        if (textures == null) return null;

        if (!textures.has("SKIN")) return null;

        return textures
                .getAsJsonObject("SKIN")
                .get("url")
                .getAsString();
    }

    private static NativeImage download(String url) throws Exception {

        HttpResponse<InputStream> response = HTTP.send(
                req(url),
                HttpResponse.BodyHandlers.ofInputStream()
        );

        if (response.statusCode() != 200) return null;

        return NativeImage.read(response.body());
    }

    private static NativeImage cropFace(NativeImage skin) {

        int dstSize = 16;

        NativeImage face =
                new NativeImage(dstSize, dstSize, false);

        int srcX = 8;
        int srcY = 8;
        int srcSize = 8;

        for (int y = 0; y < dstSize; y++) {

            for (int x = 0; x < dstSize; x++) {

                int sx = srcX + (x * srcSize / dstSize);
                int sy = srcY + (y * srcSize / dstSize);

                int color = skin.getPixel(sx, sy);

                face.setPixel(x, y, color);
            }
        }

        return face;
    }

    private static HttpRequest req(String url) throws Exception {

        return HttpRequest.newBuilder(new URI(url))
                .GET()
                .timeout(Duration.ofSeconds(8))
                .header("User-Agent", "ZenithX/1.0")
                .build();
    }
}