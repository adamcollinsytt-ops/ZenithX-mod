package org.adam.zenithx.compat;

import com.google.gson.JsonObject;
import org.adam.zenithx.HostClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Orchestrates the compatibility-check feature: sending the host's snapshot
 * over the existing relay JSON channel, receiving it on the guest side,
 * running {@link CompatibilityChecker}, and handing the result to whichever
 * UI wants to show it.
 * <p>
 * Message types added to the existing protocol (see {@link HostClient} /
 * {@code HostMain.handleMessage}): {@code COMPAT_SNAPSHOT} carries a
 * {@link CompatibilitySnapshot#toJson()} payload under the key
 * {@code "snapshot"}. Both directions reuse {@link HostClient#sendJson}, so
 * no new networking code is introduced.
 * <p>
 * Everything received from the network here is treated as untrusted: it is
 * parsed defensively by {@link CompatibilitySnapshot#fromJson} and only
 * ever used for string comparisons — never as a class name, path, or
 * command.
 */
public final class CompatibilityManager {

    private static volatile CompatibilitySnapshot lastReceivedSnapshot;
    private static volatile CompatibilityResult lastResult;
    private static volatile CompletableFuture<CompatibilitySnapshot> pendingWait;

    private static Consumer<CompatibilityResult> onResultListener;

    private CompatibilityManager() {}

    /** Registers the (single) UI callback that wants to know about new results. */
    public static void setOnResultListener(Consumer<CompatibilityResult> listener) {
        onResultListener = listener;
    }

    public static CompatibilityResult lastResult() {
        return lastResult;
    }

    /**
     * Host side: called once the host's world is actually up and hosting,
     * so guests joining afterwards can compare against it. Capturing and
     * sending is fire-and-forget — failure here should never block hosting.
     */
    public static void publishLocalSnapshotAsHost() {
        try {
            CompatibilitySnapshot snapshot = CompatibilitySnapshot.captureLocal(true);
            JsonObject o = new JsonObject();
            o.addProperty("type", "COMPAT_SNAPSHOT");
            o.add("snapshot", snapshot.toJson());
            HostClient.getInstance().sendJson(o);
        } catch (Exception e) {
            System.err.println("[ZenithX] Failed to publish compatibility snapshot: " + e.getMessage());
        }
    }

    /**
     * Guest side: called from {@code HostMain.handleMessage} when a
     * {@code COMPAT_SNAPSHOT} message arrives. Parses the (untrusted)
     * payload, compares it against the local environment, stores the
     * result, and notifies the UI listener if one is registered.
     */
    public static void handleIncomingSnapshot(JsonObject msg) {
        try {
            JsonObject snapshotJson = msg.has("snapshot") && msg.get("snapshot").isJsonObject()
                    ? msg.getAsJsonObject("snapshot")
                    : null;

            CompatibilitySnapshot required = CompatibilitySnapshot.fromJson(snapshotJson);
            CompatibilitySnapshot local = CompatibilitySnapshot.captureLocal(false);

            lastReceivedSnapshot = required;
            lastResult = CompatibilityChecker.check(required, local);

            CompletableFuture<CompatibilitySnapshot> waiter = pendingWait;
            if (waiter != null) waiter.complete(required);

            if (onResultListener != null) {
                onResultListener.accept(lastResult);
            }
        } catch (Exception e) {
            System.err.println("[ZenithX] Failed to process compatibility snapshot: " + e.getMessage());
        }
    }

    /**
     * Guest side: called right when a join starts (before connecting).
     * Gives the host a short window to deliver its snapshot — older hosts
     * (or a host that failed to publish one) simply never send one, in
     * which case this resolves empty and the caller should proceed without
     * blocking the join. This never delays a join by more than the
     * timeout.
     */
    public static CompletableFuture<CompatibilitySnapshot> awaitHostSnapshot(long timeoutSeconds) {
        lastReceivedSnapshot = null;
        lastResult = null;

        CompletableFuture<CompatibilitySnapshot> future = new CompletableFuture<>();
        pendingWait = future;

        CompletableFuture.delayedExecutor(timeoutSeconds, TimeUnit.SECONDS)
                .execute(() -> future.complete(null));

        return future;
    }

    public static CompatibilitySnapshot lastReceivedSnapshot() {
        return lastReceivedSnapshot;
    }
}
