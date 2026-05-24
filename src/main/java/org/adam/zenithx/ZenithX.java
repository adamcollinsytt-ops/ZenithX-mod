package org.adam.zenithx;

import net.fabricmc.api.ClientModInitializer;

public class ZenithX implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        System.out.println("ZenithX loaded");
    }
}