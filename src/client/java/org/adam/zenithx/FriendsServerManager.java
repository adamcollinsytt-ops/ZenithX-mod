package server;

import com.google.gson.*;
import java.io.*;
import java.util.*;

public class FriendsServerManager {

    private static final File FILE = new File("friends.json");
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static Map<String, Set<String>> friends = new HashMap<>();

    public static void load() {
        try {
            if (!FILE.exists()) return;

            JsonObject root = gson.fromJson(new FileReader(FILE), JsonObject.class);
            if (root == null) return;

            friends.clear();

            for (String key : root.keySet()) {
                Set<String> set = new HashSet<>();
                JsonArray arr = root.getAsJsonArray(key);

                for (int i = 0; i < arr.size(); i++) {
                    set.add(arr.get(i).getAsString());
                }

                friends.put(key, set);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            JsonObject root = new JsonObject();

            for (String user : friends.keySet()) {
                JsonArray arr = new JsonArray();

                for (String f : friends.get(user)) {
                    arr.add(f);
                }

                root.add(user, arr);
            }

            try (FileWriter w = new FileWriter(FILE)) {
                gson.toJson(root, w);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Set<String> getFriends(String user) {
        return friends.getOrDefault(user, new HashSet<>());
    }

    public static void addFriend(String u1, String u2) {
        u1 = u1;
        u2 = u2;

        friends.putIfAbsent(u1, new HashSet<>());
        friends.putIfAbsent(u2, new HashSet<>());

        friends.get(u1).add(u2);
        friends.get(u2).add(u1);

        save();
    }

    public static void removeFriend(String u1, String u2) {
        u1 = u1;
        u2 = u2;

        if (friends.containsKey(u1)) friends.get(u1).remove(u2);
        if (friends.containsKey(u2)) friends.get(u2).remove(u1);

        save();
    }
}
