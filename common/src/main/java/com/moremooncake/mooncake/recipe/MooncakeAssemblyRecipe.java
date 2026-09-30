package com.moremooncake.mooncake.recipe;

import com.moremooncake.mooncake.item.MooncakeFood;
import com.moremooncake.mooncake.item.WholeMooncakeItem;
import com.moremooncake.mooncake.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom crafting recipe: 8 mooncake slices arranged around an egg in the
 * middle (the egg binds them). The slice slots are captured CLOCKWISE from
 * the top-left cell, which defines the eating order of the placed grand mooncake:
 * (0,0) (1,0) (2,0) (2,1) (2,2) (1,2) (0,2) (0,1).
 */
public class MooncakeAssemblyRecipe implements CraftingRecipe {
    private static final int[][] CLOCKWISE_RING = {
            {0, 0}, {1, 0}, {2, 0}, {2, 1}, {2, 2}, {1, 2}, {0, 2}, {0, 1}
    };

    private final CraftingBookCategory category;

    public MooncakeAssemblyRecipe(CraftingBookCategory category) {
        this.category = category;
    }

    @Override
    public boolean matches(CraftingInput container, Level level) {
        if (container.width() != 3 || container.height() != 3) {
            return false;
        }
        if (!container.getItem(1, 1).is(Items.EGG)) {
            return false;
        }
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (i == 1 && j == 1) {
                    continue;
                }
                if (!(container.getItem(i, j).getItem() instanceof MooncakeFood)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public ItemStack assemble(CraftingInput container) {
        List<String> slices = new ArrayList<>();
        for (int[] pos : CLOCKWISE_RING) {
            ItemStack stack = container.getItem(pos[0], pos[1]);
            if (stack.getItem() instanceof MooncakeFood) {
                slices.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            }
        }
        return WholeMooncakeItem.withSlices(slices);
    }

    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public RecipeSerializer<? extends CraftingRecipe> getSerializer() {
        return ModRecipes.MOONCAKE_ASSEMBLY_SERIALIZER.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        // No standard ingredient grid: matching is fully dynamic (any 8 mooncakeFood around an egg).
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    /** Serializer codec/stream - kept in sync with the serializer registered in ModRecipes. */
    public static final MapCodec<MooncakeAssemblyRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(CraftingBookCategory.CODEC.fieldOf("category")
                            .orElse(CraftingBookCategory.MISC)
                            .forGetter(MooncakeAssemblyRecipe::category))
                    .apply(instance, MooncakeAssemblyRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MooncakeAssemblyRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    CraftingBookCategory.STREAM_CODEC, MooncakeAssemblyRecipe::category,
                    MooncakeAssemblyRecipe::new);
}
