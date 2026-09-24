package com.moremooncake.mooncake.client;

import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.injectables.annotations.ExpectPlatform;

/**
 * Client-only setup (registered from common, executed only on the client).
 * The block entity renderer registration is platform-specific, so it goes
 * through an {@link ExpectPlatform} method implemented in each loader module.
 */
public final class ModClient {
    private ModClient() {
    }

    public static void init() {
        ClientLifecycleEvent.CLIENT_SETUP.register(client -> ModClient.registerBlockEntityRenderer());
    }

    @ExpectPlatform
    private static void registerBlockEntityRenderer() {
        throw new AssertionError("platform implementation missing");
    }
}
