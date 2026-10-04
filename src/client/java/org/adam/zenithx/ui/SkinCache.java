package org.adam.zenithx.ui;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
//? if >=26 {
import net.minecraft.resources.Identifier;
//? } else {
/*import net.minecraft.resources.ResourceLocation;*/
//? }

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SkinCache {

    private static final String MINOTAR_URL = "https://minotar.net/skin/%s";

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final ExecutorService POOL =
            Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "skin-loader");
                t.setDaemon(true);
                return t;
            });

    //? if >=26 {
    private static final Map<String, Identifier> READY = new ConcurrentHashMap<>();
    //? } else {
    /*private static final Map<String, ResourceLocation> READY = new ConcurrentHashMap<>();*/
    //? }
    private static final Set<String> FETCHING = ConcurrentHashMap.newKeySet();

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

    //? if >=26 {
    public static Identifier get(String username) {
    //? } else {
    /*public static ResourceLocation get(String username) {*/
    //? }
        if (username == null) return null;
        return READY.get(username.toLowerCase());
    }

    public static void clear() {
        READY.forEach((name, id) -> {
            try {
                Minecraft.getInstance().getTextureManager().release(id);
            } catch (Exception ignored) {
            }
        });

        READY.clear();
        FETCHING.clear();
    }

    private static void fetchAndRegister(String username) {
        try {
            HttpRequest req = HttpRequest.newBuilder(new URI(MINOTAR_URL.formatted(username)))
                    .GET()
                    .timeout(Duration.ofSeconds(8))
                    .header("User-Agent", "ZenithX/1.0")
                    .build();

            HttpResponse<InputStream> response = HTTP.send(req, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                NativeImage skin = NativeImage.read(response.body());
                registerTexture(username, skin);
            } else {
                FETCHING.remove(username);
            }
        } catch (Exception e) {
            FETCHING.remove(username);
        }
    }

    private static void registerTexture(String username, NativeImage image) {
        Minecraft.getInstance().execute(() -> {
            try {
                //? if >=26 {
                Identifier id = Identifier.fromNamespaceAndPath("zenithx", "skin/" + username);
                //? } else if >=1.20.5 {
                /*ResourceLocation id = ResourceLocation.fromNamespaceAndPath("zenithx", "skin/" + username);*/
                //? } else {
                /*ResourceLocation id = new ResourceLocation("zenithx", "skin/" + username);*/
                //? }
                
                Minecraft.getInstance().getTextureManager().release(id);
                
                //? if >=1.20.5 {
                DynamicTexture texture = new DynamicTexture(() -> "skin:" + username, image);
                //? } else {
                /*DynamicTexture texture = new DynamicTexture(image);*/
                //? }
                texture.upload();
                
                Minecraft.getInstance().getTextureManager().register(id, texture);
                READY.put(username, id);
            } catch (Exception e) {
                image.close();
            } finally {
                FETCHING.remove(username);
            }
        });
    }
}