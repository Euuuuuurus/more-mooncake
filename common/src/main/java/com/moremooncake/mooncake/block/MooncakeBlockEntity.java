package com.moremooncake.mooncake.block;

import com.moremooncake.mooncake.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores the 8 slice slots of a placed grand mooncake.
 * Each slot holds the registry id of a slice item, in eating order.
 */
public class MooncakeBlockEntity extends BlockEntity {
    public static final String SLICES_KEY = "Slices";

    private List<String> slices = List.of();

    /** Server-side only: last game tick an eat happened, to stop double-eats in a single click. */
    public long lastEatGameTime = Long.MIN_VALUE / 2;

    public MooncakeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.MOONCAKE_BE.get(), pos, state);
    }

    public List<String> getSlices() {
        return slices;
    }

    public void setSlices(List<String> slices) {
        this.slices = new ArrayList<>(slices);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ValueOutput.TypedOutputList<String> list = output.list(SLICES_KEY, Codec.STRING);
        for (String s : slices) {
            list.add(s);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        List<String> loaded = new ArrayList<>();
        input.listOrEmpty(SLICES_KEY, Codec.STRING).forEach(loaded::add);
        slices = loaded;
    }
}