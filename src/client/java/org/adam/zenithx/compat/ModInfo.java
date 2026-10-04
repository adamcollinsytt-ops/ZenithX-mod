package org.adam.zenithx.compat;

/**
 * Immutable, purely-informational description of a single mod: its id and
 * version. Used both for "what the host requires" and "what the local
 * client has installed" — never carries a jar, a URL, or any executable
 * payload. Safe to serialize to/from JSON and to log.
*/
public final class ModInfo {

    private final String modId;
    private final String version;
    private final String displayName;
    private final boolean required;

    public ModInfo(String modId, String version, String displayName, boolean required) {
        this.modId = modId == null ? "" : modId;
        this.version = version == null ? "" : version;
        this.displayName = (displayName == null || displayName.isBlank()) ? this.modId : displayName;
        this.required = required;
    }

    public static ModInfo required(String modId, String version, String displayName) {
        return new ModInfo(modId, version, displayName, true);
    }

    public static ModInfo optional(String modId, String version, String displayName) {
        return new ModInfo(modId, version, displayName, false);
    }

    public String modId() { return modId; }
    public String version() { return version; }
    public String displayName() { return displayName; }
    public boolean required() { return required; }

    @Override
    public String toString() {
        return displayName + " (" + modId + " " + version + ")";
    }
}
