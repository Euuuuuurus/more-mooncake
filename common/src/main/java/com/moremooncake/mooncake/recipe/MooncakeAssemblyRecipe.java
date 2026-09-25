package com.moremooncake.mooncake.recipe;

import com.moremooncake.mooncake.item.MooncakeFood;
import com.moremooncake.mooncake.item.WholeMooncakeItem;
import com.moremooncake.mooncake.registry.ModItems;
import com.moremooncake.mooncake.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
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
    public ItemStack assemble(CraftingInput container, HolderLookup.Provider access) {
        List<String> slices = new ArrayList<>();
        for (int[] pos : CLOCKWISE_RING) {
            ItemStack stack = container.getItem(pos[0], pos[1]);
            if (stack.getItem() instanceof MooncakeFood) {
                slices.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            }
        }
        return WholeMooncakeItem.withSlices(slices);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider access) {
        return new ItemStack(ModItems.MOONCAKE.get());
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.MOONCAKE_ASSEMBLY_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        // MUST be the vanilla crafting type, otherwise the crafting table (which
        // only queries RecipeType.CRAFTING) would never find this recipe.
        return RecipeType.CRAFTING;
    }
}
