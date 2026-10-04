package org.adam.zenithx.compat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.adam.zenithx.HostClient;

import java.util.ArrayList;
import java.util.List;

/**
 * A snapshot of a "modded multiplayer" environment: Minecraft version,
 * Fabric Loader version, ZenithX version, and a list of mods.
 * <p>
 * This is metadata only — ids and version strings. It never contains a
 * jar, a download URL, class bytes, or anything else that could be
 * executed. It is safe to send over the existing JSON relay channel and
 * safe to treat as untrusted input when it comes from the network (see
 * {@link #fromJson(JsonObject)}).
 */
public final class CompatibilitySnapshot {

    private static final Gson GSON = new GsonBuilder().create();

    private final String minecraftVersion;
    private final String loaderVersion;
    private final String zenithxVersion;
    private final List<ModInfo> mods;

    public CompatibilitySnapshot(String minecraftVersion, String loaderVersion,
                                  String zenithxVersion, List<ModInfo> mods) {
        this.minecraftVersion = minecraftVersion == null ? "" : minecraftVersion;
        this.loaderVersion = loaderVersion == null ? "" : loaderVersion;
        this.zenithxVersion = zenithxVersion == null ? "" : zenithxVersion;
        this.mods = mods == null ? List.of() : List.copyOf(mods);
    }

    public String minecraftVersion() { return minecraftVersion; }
    public String loaderVersion() { return loaderVersion; }
    public String zenithxVersion() { return zenithxVersion; }
    public List<ModInfo> mods() { return mods; }

    /**
     * Builds a snapshot of the mods actually installed on this client right
     * now, read purely from Fabric Loader's own metadata — no network
     * access, no file scanning outside what Loader already indexed at
     * startup.
     *
     * @param requiredOnly if true, only user-facing (non-library, non-builtin)
     *                     mods are included, which is what a host should
     *                     publish as "what you need to have".
     */
    public static CompatibilitySnapshot captureLocal(boolean requiredOnly) {
        FabricLoader loader = FabricLoader.getInstance();

        String mcVersion = loader.getModContainer("minecraft")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");

        String loaderVersion = loader.getModContainer("fabricloader")
                .map(c -> c.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");

        List<ModInfo> mods = new ArrayList<>();
        for (ModContainer container : loader.getAllMods()) {
            String id = container.getMetadata().getId();
            if (requiredOnly && isInfrastructureMod(id)) continue;

            // Defaults to "optional" (missing = Warning, never blocks a join)
            // rather than "required" (missing = Incompatible) - matching
            // "not strict by default": a guest without some client-side
            // cosmetic mod shouldn't be refused entry. There's currently no
            // UI for a host to mark a specific mod as actually required;
            // when that's added, only those should use ModInfo.required(...).
            mods.add(ModInfo.optional(
                    id,
                    container.getMetadata().getVersion().getFriendlyString(),
                    container.getMetadata().getName()
            ));
        }

        return new CompatibilitySnapshot(mcVersion, loaderVersion, HostClient.MOD_VERSION, mods);
    }

    /**
     * Mods that are effectively always present on any Fabric client and
     * carry no real "did the player install this on purpose" meaning —
     * kept out of required-mod comparisons so the list stays meaningful.
     */
    private static boolean isInfrastructureMod(String id) {
        return id.equals("minecraft")
                || id.equals("fabricloader")
                || id.equals("java")
                || id.equals("zenithx");
    }

    public JsonObject toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("minecraft_version", minecraftVersion);
        root.addProperty("loader_version", loaderVersion);
        root.addProperty("zenithx_version", zenithxVersion);

        JsonArray modsArr = new JsonArray();
        for (ModInfo m : mods) {
            JsonObject mo = new JsonObject();
            mo.addProperty("id", m.modId());
            mo.addProperty("version", m.version());
            mo.addProperty("name", m.displayName());
            mo.addProperty("required", m.required());
            modsArr.add(mo);
        }
        root.add("mods", modsArr);
        return root;
    }

    /**
     * Parses a snapshot received from the relay/host. Treated as untrusted
     * input: every field is defensively read with a fallback, and nothing
     * here is ever used as a class name, a file path, or a command — it
     * only ever feeds into {@link CompatibilityChecker}'s string
     * comparisons.
     */
    public static CompatibilitySnapshot fromJson(JsonObject root) {
        if (root == null) return new CompatibilitySnapshot("", "", "", List.of());

        String mcVersion = safeString(root, "minecraft_version");
        String loaderVersion = safeString(root, "loader_version");
        String zenithxVersion = safeString(root, "zenithx_version");

        List<ModInfo> mods = new ArrayList<>();
        if (root.has("mods") && root.get("mods").isJsonArray()) {
            JsonArray arr = root.getAsJsonArray("mods");
            for (int i = 0; i < arr.size(); i++) {
                if (!arr.get(i).isJsonObject()) continue;
                JsonObject mo = arr.get(i).getAsJsonObject();
                String id = safeString(mo, "id");
                if (id.isEmpty()) continue;
                mods.add(new ModInfo(
                        id,
                        safeString(mo, "version"),
                        safeString(mo, "name"),
                        mo.has("required") && mo.get("required").isJsonPrimitive()
                                && mo.get("required").getAsBoolean()
                ));
            }
        }

        return new CompatibilitySnapshot(mcVersion, loaderVersion, zenithxVersion, mods);
    }

    private static String safeString(JsonObject o, String key) {
        try {
            return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : "";
        } catch (Exception e) {
            return "";
        }
    }

    public String toJsonString() {
        return GSON.toJson(toJson());
    }
}
