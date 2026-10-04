package org.adam.zenithx;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Servers {
    private static final String[] BACKUPS = {
        "wss://zenithx-backup1.wisp.uno"
    };

    private static final String[] STORAGE_BACKUPS = {
        "https://zenithx-storage1.wisp.uno"
    };

    private static final String SERVERS_URL =
        "https://gist.githubusercontent.com/adamcollinsytt-ops/edcb81a391b8f91f393d7d35697142ea/raw/zenithx-servers.json";

    public static volatile String[] LIST = BACKUPS.clone();
    public static volatile String[] STORAGE_LIST = STORAGE_BACKUPS.clone();
    
    public static int backupCount() { return BACKUPS.length; }
    public static volatile boolean lastFetchOk = false;
    
    private static final Random RANDOM = new Random();

    public static int firstBackupIndex() {
        return Math.max(0, LIST.length - BACKUPS.length);
    }

    public static synchronized void fetchServers() {
        lastFetchOk = false;
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(4))
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(SERVERS_URL + "?t=" + System.currentTimeMillis()))
                        .header("Cache-Control", "no-cache")
                        .header("User-Agent", "ZenithX-Mod")
                        .timeout(Duration.ofSeconds(4))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    System.err.println("[ZenithX] Gist returned HTTP " + response.statusCode() + " (attempt " + attempt + ")");
                    continue;
                }

                JsonObject obj = new Gson().fromJson(response.body(), JsonObject.class);
                
                var arr = obj == null ? null : obj.getAsJsonArray("servers");
                if (arr != null && !arr.isEmpty()) {
                    List<String> backups = Arrays.asList(BACKUPS);
                    Set<String> merged = new LinkedHashSet<>();
                    for (int i = 0; i < arr.size(); i++) {
                        String s = arr.get(i).getAsString().trim();
                        if (!s.isEmpty() && !backups.contains(s)) merged.add(s);
                    }
                    merged.addAll(backups);
                    LIST = merged.toArray(new String[0]);
                }

                var storageArr = obj == null ? null : obj.getAsJsonArray("servers_storage");
                if (storageArr != null && !storageArr.isEmpty()) {
                    List<String> storageBackups = Arrays.asList(STORAGE_BACKUPS);
                    Set<String> storageMerged = new LinkedHashSet<>();
                    for (int i = 0; i < storageArr.size(); i++) {
                        String s = storageArr.get(i).getAsString().trim();
                        if (!s.isEmpty() && !storageBackups.contains(s)) storageMerged.add(s);
                    }
                    storageMerged.addAll(storageBackups);
                    STORAGE_LIST = storageMerged.toArray(new String[0]);
                }

                lastFetchOk = true;
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception e) {
                System.err.println("[ZenithX] Fetch failed (attempt " + attempt + "): " + e);
            }
        }
        System.err.println("[ZenithX] Using backups only for servers and storage.");
    }

    public static String getRandomServer() {
        String[] currentList = LIST;
        if (currentList.length == 0) currentList = BACKUPS;
        return currentList[RANDOM.nextInt(currentList.length)];
    }

    public static String getRandomStorageServer() {
        String[] currentStorage = STORAGE_LIST;
        if (currentStorage.length == 0) currentStorage = STORAGE_BACKUPS;
        return currentStorage[RANDOM.nextInt(currentStorage.length)];
    }
}