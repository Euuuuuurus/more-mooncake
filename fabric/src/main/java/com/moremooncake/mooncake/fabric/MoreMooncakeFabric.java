package com.moremooncake.mooncake.fabric;

import com.moremooncake.mooncake.MoreMooncake;
import net.fabricmc.api.ModInitializer;

public class MoreMooncakeFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MoreMooncake.init();
    }
}
