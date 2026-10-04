package org.adam.zenithx.ui;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import org.adam.zenithx.HostClient;
import org.adam.zenithx.HostMain;

public class HostClientInit implements ClientModInitializer {

    private static boolean connected = false;

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!connected && client.getUser() != null) {
                connected = true;

                String username = client.getUser().getName();

                HostClient.lastLoginUsername = username;

                new Thread(() -> HostMain.connect(username)).start();
            }
        });
    }
}