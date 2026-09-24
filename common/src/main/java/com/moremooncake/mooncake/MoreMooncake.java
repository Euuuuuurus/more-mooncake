package com.moremooncake.mooncake;

import com.moremooncake.mooncake.client.ModClient;
import com.moremooncake.mooncake.dev.AssemblySelfCheck;
import com.moremooncake.mooncake.registry.ModBlocks;
import com.moremooncake.mooncake.registry.ModItems;
import com.moremooncake.mooncake.registry.ModRecipes;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

/**
 * Common mod initializer, called from both the Fabric and NeoForge entry points.
 */
public final class MoreMooncake {
    public static final String MOD_ID = "more_mooncake";

    private MoreMooncake() {
    }

    public static void init() {
        ModBlocks.register();
        ModItems.register();
        ModRecipes.register();
        AssemblySelfCheck.register();
        if (Platform.getEnvironment() == Env.CLIENT) {
            ModClient.init();
        }
    }
}
