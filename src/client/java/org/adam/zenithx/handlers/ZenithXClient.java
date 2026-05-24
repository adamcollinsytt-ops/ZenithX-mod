package org.adam.zenithx.handlers;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import org.adam.zenithx.HostClient;
import org.adam.zenithx.ui.NoConnectionWarningScreen;

public class ZenithXClient implements ClientModInitializer {

    public static volatile boolean connectionChecked = false;

    @Override
    public void onInitializeClient() {
        ZenithRPC.init();
        Settings.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            Settings.tick();

            if (!connectionChecked && client.screen instanceof TitleScreen) {
                synchronized (ZenithXClient.class) {
                    if (!connectionChecked) {
                        connectionChecked = true;
                        Thread connectThread = new Thread(() -> {

                            boolean connected = HostClient.connectToAnyServer();

                            if (!connected) {

                                client.execute(() -> {

                                    if (!(client.screen instanceof NoConnectionWarningScreen)) {

                                        client.setScreen(new NoConnectionWarningScreen(
                                                () -> {},
                                                () -> HostClient.tryReconnectNow()
                                        ));
                                    }
                                });
                            }

                        }, "zenithx-connect");
                        connectThread.setDaemon(true);
                        connectThread.start();
                    }
                }
            }
        });
    }
}