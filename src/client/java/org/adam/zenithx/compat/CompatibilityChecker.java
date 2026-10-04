package org.adam.zenithx.compat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pure comparison logic: given what the host requires and what is
 * actually installed locally, decides per-mod status and an overall
 * {@link CompatibilityResult.Severity}. No networking, no file access,
 * no mod loading — just string comparisons over two already-parsed
 * {@link CompatibilitySnapshot} objects.
 */
public final class CompatibilityChecker {

    private CompatibilityChecker() {}

    public static CompatibilityResult check(CompatibilitySnapshot required, CompatibilitySnapshot local) {
        CompatibilityResult.FieldCheck mcCheck = compareField(
                "Minecraft Version", required.minecraftVersion(), local.minecraftVersion());
        CompatibilityResult.FieldCheck loaderCheck = compareField(
                "Fabric Loader", required.loaderVersion(), local.loaderVersion());
        CompatibilityResult.FieldCheck zenithxCheck = compareField(
                "ZenithX Version", required.zenithxVersion(), local.zenithxVersion());

        Map<String, ModInfo> localById = new HashMap<>();
        for (ModInfo m : local.mods()) localById.put(m.modId(), m);

        Map<String, ModInfo> requiredById = new HashMap<>();
        for (ModInfo m : required.mods()) requiredById.put(m.modId(), m);

        List<CompatibilityResult.ModCheckEntry> entries = new ArrayList<>();

        for (ModInfo req : required.mods()) {
            ModInfo installed = localById.get(req.modId());

            if (installed == null) {
                CompatibilityResult.Severity sev = req.required()
                        ? CompatibilityResult.Severity.INCOMPATIBLE
                        : CompatibilityResult.Severity.WARNING;
                entries.add(new CompatibilityResult.ModCheckEntry(
                        req.modId(), req.displayName(), req.version(), "",
                        CompatibilityResult.ModStatus.MISSING, sev,
                        "Not installed"
                ));
                continue;
            }

            boolean versionOk = req.version().isBlank()
                    || req.version().equalsIgnoreCase(installed.version());

            if (versionOk) {
                entries.add(new CompatibilityResult.ModCheckEntry(
                        req.modId(), req.displayName(), req.version(), installed.version(),
                        CompatibilityResult.ModStatus.OK, CompatibilityResult.Severity.COMPATIBLE,
                        "Matches"
                ));
            } else {
                entries.add(new CompatibilityResult.ModCheckEntry(
                        req.modId(), req.displayName(), req.version(), installed.version(),
                        CompatibilityResult.ModStatus.VERSION_MISMATCH, CompatibilityResult.Severity.WARNING,
                        "Version mismatch"
                ));
            }
        }

        for (ModInfo installed : local.mods()) {
            if (requiredById.containsKey(installed.modId())) continue;
            entries.add(new CompatibilityResult.ModCheckEntry(
                    installed.modId(), installed.displayName(), "", installed.version(),
                    CompatibilityResult.ModStatus.EXTRA, CompatibilityResult.Severity.COMPATIBLE,
                    "Not required by host"
            ));
        }

        entries.sort((a, b) -> {
            int sa = severityRank(a.severity());
            int sb = severityRank(b.severity());
            if (sa != sb) return Integer.compare(sa, sb);
            return a.displayName().compareToIgnoreCase(b.displayName());
        });

        CompatibilityResult.Severity overall = worstOf(
                worstOf(mcCheck.matches() ? CompatibilityResult.Severity.COMPATIBLE : CompatibilityResult.Severity.INCOMPATIBLE,
                        loaderCheck.matches() ? CompatibilityResult.Severity.COMPATIBLE : CompatibilityResult.Severity.INCOMPATIBLE),
                worstOf(zenithxCheck.matches() ? CompatibilityResult.Severity.COMPATIBLE : CompatibilityResult.Severity.WARNING,
                        worstModSeverity(entries))
        );

        return new CompatibilityResult(mcCheck, loaderCheck, zenithxCheck, entries, overall);
    }

    private static CompatibilityResult.FieldCheck compareField(String label, String expected, String actual) {
        boolean matches = expected.isBlank() || expected.equalsIgnoreCase(actual);
        return new CompatibilityResult.FieldCheck(label, expected, actual, matches);
    }

    private static CompatibilityResult.Severity worstModSeverity(List<CompatibilityResult.ModCheckEntry> entries) {
        CompatibilityResult.Severity worst = CompatibilityResult.Severity.COMPATIBLE;
        for (CompatibilityResult.ModCheckEntry e : entries) worst = worstOf(worst, e.severity());
        return worst;
    }

    private static CompatibilityResult.Severity worstOf(CompatibilityResult.Severity a, CompatibilityResult.Severity b) {
        return severityRank(a) >= severityRank(b) ? a : b;
    }

    private static int severityRank(CompatibilityResult.Severity s) {
        return switch (s) {
            case COMPATIBLE -> 0;
            case WARNING -> 1;
            case INCOMPATIBLE -> 2;
        };
    }
}
