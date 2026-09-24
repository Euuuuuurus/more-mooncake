package com.moremooncake.mooncake.client.neoforge;

import com.moremooncake.mooncake.block.MooncakeBlockEntity;
import com.moremooncake.mooncake.client.MooncakeBlockEntityRenderer;
import com.moremooncake.mooncake.registry.ModBlocks;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * NeoForge implementation of the client-only setup.
 */
public final class ModClientImpl {
    private ModClientImpl() {
    }

    public static void registerBlockEntityRenderer() {
        BlockEntityType<MooncakeBlockEntity> type = ModBlocks.MOONCAKE_BE.get();
        BlockEntityRendererProvider<MooncakeBlockEntity> provider = MooncakeBlockEntityRenderer::new;
        BlockEntityRenderers.register(type, provider);
    }
}
