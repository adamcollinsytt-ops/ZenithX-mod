package org.adam.zenithx.compat;

import java.util.List;

/**
 * The outcome of comparing a host's {@link CompatibilitySnapshot} against
 * this client's local environment. Purely a data holder — produced by
 * {@link CompatibilityChecker}, consumed by the compatibility UI.
 */
public final class CompatibilityResult {

    public enum Severity {
        COMPATIBLE,
        WARNING,
        INCOMPATIBLE
    }

    public enum ModStatus {
        OK,               // installed, version matches (or host didn't pin a version)
        VERSION_MISMATCH, // installed, but a different version than required
        MISSING,          // required by host, not installed locally
        EXTRA             // installed locally, not requested by host (informational only)
    }

    public record FieldCheck(String label, String expected, String actual, boolean matches) {}

    public record ModCheckEntry(
            String modId,
            String displayName,
            String requiredVersion,
            String installedVersion,
            ModStatus status,
            Severity severity,
            String reason
    ) {}

    private final FieldCheck minecraftVersion;
    private final FieldCheck loaderVersion;
    private final FieldCheck zenithxVersion;
    private final List<ModCheckEntry> mods;
    private final Severity overall;

    public CompatibilityResult(FieldCheck minecraftVersion, FieldCheck loaderVersion,
                                FieldCheck zenithxVersion, List<ModCheckEntry> mods,
                                Severity overall) {
        this.minecraftVersion = minecraftVersion;
        this.loaderVersion = loaderVersion;
        this.zenithxVersion = zenithxVersion;
        this.mods = List.copyOf(mods);
        this.overall = overall;
    }

    public FieldCheck minecraftVersion() { return minecraftVersion; }
    public FieldCheck loaderVersion() { return loaderVersion; }
    public FieldCheck zenithxVersion() { return zenithxVersion; }
    public List<ModCheckEntry> mods() { return mods; }
    public Severity overall() { return overall; }

    public long problemCount() {
        long fieldProblems = List.of(minecraftVersion, loaderVersion, zenithxVersion).stream()
                .filter(f -> !f.matches()).count();
        long modProblems = mods.stream()
                .filter(m -> m.status() != ModStatus.OK && m.status() != ModStatus.EXTRA)
                .count();
        return fieldProblems + modProblems;
    }

    public boolean blocksJoin() {
        return overall == Severity.INCOMPATIBLE;
    }
}
