package org.adam.zenithx.account;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.User;
import net.minecraft.server.commands.ExperienceCommand;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.step.AbstractStep;
import net.raphimc.minecraftauth.step.java.StepMCProfile;
import net.raphimc.minecraftauth.step.java.session.StepFullJavaSession;
import net.raphimc.minecraftauth.step.msa.StepMsaDeviceCode;

import org.adam.zenithx.DevelopmentShared;
import org.adam.zenithx.mixin.MinecraftAccessor;
import net.lenni0451.commons.httpclient.HttpClient;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.server.ExportException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public final class AccountManager {
    private static final boolean IS_DEBUG = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final AbstractStep<?, StepFullJavaSession.FullJavaSession> LOGIN = MinecraftAuth.JAVA_DEVICE_CODE_LOGIN;
    private static final HttpClient HTTP = MinecraftAuth.createHttpClient();

    private static User initialLauncherUser = null;

    public static final class Account {
        public String name;
        public String uuid;
        public JsonObject session;
    }

    private static final class Store {
        String active = "";
        List<Account> accounts = new ArrayList<>();
    }

    private static Store store = new Store();
    private static Path file;

    private AccountManager() {}

    public static synchronized void load() {
        DevelopmentShared.checkAndCrashIfFakeDev();

        file = Minecraft.getInstance().gameDirectory.toPath().resolve("config").resolve("zenithx_accounts.json");

        /*if (!IS_DEBUG) { // old way
            try {
                Files.deleteIfExists(file);
                store = new Store();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } */

        try {
            if (Files.exists(file)) {
                Store loaded = GSON.fromJson(Files.readString(file), Store.class);
                if (loaded != null && loaded.accounts != null) store = loaded;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        registerCurrentSessionUser();

        if (IS_DEBUG) {
            addOfflineAccount("Adam_CollinsYT");
            addOfflineAccount("ALKRKY99");
            addOfflineAccount("Mustafa_GO");
            addOfflineAccount("Developer_Test");
        } else { // new way
            removeOfflineByName("Adam_CollinsYT");
            removeOfflineByName("ALKRKY99");
            removeOfflineByName("Mustafa_GO");
            removeOfflineByName("Developer_Test");
        }
    }

    private static void registerCurrentSessionUser() {
        try {
            User current = Minecraft.getInstance().getUser();
            if (current != null && current.getName() != null && !current.getName().isEmpty()) {
                if (initialLauncherUser == null) {
                    initialLauncherUser = current;
                }
                String cleanUuid = current.getProfileId().toString().replace("-", "");

                boolean exists = store.accounts.stream().anyMatch(a -> a.uuid.equals(cleanUuid));
                if (!exists) {
                    Account acc = new Account();
                    acc.name = current.getName();
                    acc.uuid = cleanUuid;
                    acc.session = null;
                    store.accounts.add(0, acc);
                }

                if (store.active.isEmpty()) {
                    store.active = cleanUuid;
                }
                save();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static synchronized void save() {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(store));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static synchronized void removeAccount(String uuid) {
        if (uuid == null) return;
        
        String cleanTargetUuid = uuid.replace("-", "").toLowerCase();

        store.accounts.removeIf(a -> a.uuid != null && a.uuid.replace("-", "").equalsIgnoreCase(cleanTargetUuid));
        
        if (store.active != null && store.active.replace("-", "").equalsIgnoreCase(cleanTargetUuid)) {
            if (!store.accounts.isEmpty()) {
                store.active = store.accounts.get(0).uuid;
            } else {
                store.active = "";
            }
        }
        save();
    }

    public static synchronized void addOfflineAccount(String name) {
        Account acc = new Account();
        acc.name = name;
        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        acc.uuid = offlineUuid.toString().replace("-", "");
        acc.session = null;

        store.accounts.removeIf(a -> a.uuid.equals(acc.uuid));
        store.accounts.add(acc);
        if (store.active.isEmpty()) {
            store.active = acc.uuid;
        }
        save();
    }

    private static void removeOfflineByName(String name) {
        UUID offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
        String cleanUuid = offlineUuid.toString().replace("-", "");
        removeAccount(cleanUuid);
    }

    public static synchronized List<Account> accounts() {
        return new ArrayList<>(store.accounts);
    }

    public static synchronized String activeUuid() {
        if (!store.active.isEmpty()) return store.active;
        User current = Minecraft.getInstance().getUser();
        return current.getProfileId().toString().replace("-", "");
    }

    public static void addAccountAsync(Consumer<StepMsaDeviceCode.MsaDeviceCode> onCode,
                                       Consumer<String> status, Runnable done) {
        CompletableFuture.runAsync(() -> {
            try {
                StepFullJavaSession.FullJavaSession session =
                        LOGIN.getFromInput(HTTP, new StepMsaDeviceCode.MsaDeviceCodeCallback(onCode));
                Account acc = toAccount(session);
                synchronized (AccountManager.class) {
                    store.accounts.removeIf(a -> a.uuid.equals(acc.uuid));
                    store.accounts.add(acc);
                }
                apply(session, acc.uuid);
                status.accept("Logged in as " + acc.name);
            } catch (Exception e) {
                status.accept("Login failed: " + e.getMessage());
            } finally {
                done.run();
            }
        });
    }

    public static void switchAsync(Account acc, Consumer<String> status, Runnable done) {
        CompletableFuture.runAsync(() -> {
            try {
                status.accept("Switching to " + acc.name + "...");

                if (acc.session == null && initialLauncherUser != null 
                        && acc.uuid.equals(initialLauncherUser.getProfileId().toString().replace("-", ""))) {
                    Minecraft mc = Minecraft.getInstance();
                    mc.execute(() -> ((MinecraftAccessor) mc).zenithx$setUser(initialLauncherUser));
                    synchronized (AccountManager.class) {
                        store.active = acc.uuid;
                    }
                    save();
                    status.accept("Now playing as " + acc.name);
                } else if (acc.session == null && DevelopmentShared.isDev(acc.name)) {
                    status.accept("Cannot play as Developer in Offline mode!");
                    done.run();
                    return;
                } else if (acc.session == null) {
                    applyOffline(acc);
                    status.accept("Now playing as " + acc.name + " (Offline)");
                } else {
                    StepFullJavaSession.FullJavaSession session = LOGIN.fromJson(acc.session);
                    session = LOGIN.refresh(HTTP, session);
                    Account fresh = toAccount(session);
                    synchronized (AccountManager.class) {
                        store.accounts.replaceAll(a -> a.uuid.equals(fresh.uuid) ? fresh : a);
                    }
                    apply(session, fresh.uuid);
                    status.accept("Now playing as " + fresh.name);
                }
            } catch (Exception e) {
                status.accept("Switch failed: " + e.getMessage());
            } finally {
                done.run();
            }
        });
    }

    private static Account toAccount(StepFullJavaSession.FullJavaSession session) {
        StepMCProfile.MCProfile profile = session.getMcProfile();
        Account acc = new Account();
        acc.name = profile.getName();
        acc.uuid = profile.getId().toString().replace("-", "");
        acc.session = LOGIN.toJson(session);
        return acc;
    }

    private static void apply(StepFullJavaSession.FullJavaSession session, String uuid) {
        StepMCProfile.MCProfile profile = session.getMcProfile();
        User user = new User(
                profile.getName(),
                profile.getId(),
                profile.getMcToken().getAccessToken(),
                Optional.empty(),
                Optional.empty()
        );
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> ((MinecraftAccessor) mc).zenithx$setUser(user));
        synchronized (AccountManager.class) {
            store.active = uuid;
        }
        save();
    }

    private static void applyOffline(Account acc) {
        UUID uuidObj;
        try {
            if (acc.uuid.length() == 32) {
                String formatted = acc.uuid.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                        "$1-$2-$3-$4-$5"
                );
                uuidObj = UUID.fromString(formatted);
            } else {
                uuidObj = UUID.fromString(acc.uuid);
            }
        } catch (Exception e) {
            uuidObj = UUID.nameUUIDFromBytes(("OfflinePlayer:" + acc.name).getBytes(StandardCharsets.UTF_8));
        }

        User user = new User(
                acc.name,
                uuidObj,
                "",
                Optional.empty(),
                Optional.empty()
        );
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
        ((MinecraftAccessor) mc).zenithx$setUser(user);
            DevelopmentShared.checkAndCrashIfFakeDev();
        });
        synchronized (AccountManager.class) {
            store.active = acc.uuid;
        }
        save();
    }
}