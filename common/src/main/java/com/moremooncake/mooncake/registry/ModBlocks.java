package com.moremooncake.mooncake.registry;

import com.moremooncake.mooncake.MoreMooncake;
import com.moremooncake.mooncake.block.MooncakeBlock;
import com.moremooncake.mooncake.block.MooncakeBlockEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Registers the grand mooncake block and its block entity type.
 */
public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(MoreMooncake.MOD_ID, Registries.BLOCK);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(MoreMooncake.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<Block> MOONCAKE_BLOCK = BLOCKS.register("mooncake_block",
            () -> new MooncakeBlock(BlockBehaviour.Properties.of()
                    .strength(0.5F)
                    .sound(SoundType.WOOL)
                    .noOcclusion()));

    public static final RegistrySupplier<BlockEntityType<MooncakeBlockEntity>> MOONCAKE_BE = BLOCK_ENTITIES.register(
            "mooncake_block_entity",
            () -> BlockEntityType.Builder.of(MooncakeBlockEntity::new, MOONCAKE_BLOCK.get()).build(null));

    private ModBlocks() {
    }

    public static void register() {
        BLOCKS.register();
        BLOCK_ENTITIES.register();
    }
}
