package com.gridless.client;

import net.fabricmc.api.ClientModInitializer;

public class GridlessModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        com.gridless.network.GridlessClientNetwork.registerS2C();
    }
}
