package com.moremooncake.mooncake.block;

import com.moremooncake.mooncake.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (String s : slices) {
            list.add(StringTag.valueOf(s));
        }
        tag.put(SLICES_KEY, list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        slices = new ArrayList<>();
        if (tag.contains(SLICES_KEY, Tag.TAG_LIST)) {
            ListTag list = tag.getList(SLICES_KEY, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                slices.add(list.getString(i));
            }
        }
    }
}
