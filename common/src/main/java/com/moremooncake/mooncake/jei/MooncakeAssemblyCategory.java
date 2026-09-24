package com.moremooncake.mooncake.jei;

import com.moremooncake.mooncake.MoreMooncake;
import com.moremooncake.mooncake.jei.MooncakeJeiPlugin.AssemblyJeiRecipe;
import com.moremooncake.mooncake.registry.ModItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * Shows how to assemble a grand mooncake: 8 slices around an egg, mirroring the crafting
 * table layout. The ring is captured clockwise from the top-left cell, which is also the
 * eating order of the placed mooncake.
 */
public class MooncakeAssemblyCategory implements IRecipeCategory<AssemblyJeiRecipe> {
    /** Clockwise ring positions in the 3x3 grid (18px cells starting at 1,1). */
    private static final int[][] RING = {
            {1, 1}, {19, 1}, {37, 1}, {37, 19}, {37, 37}, {19, 37}, {1, 37}, {1, 19}
    };

    private final IDrawable icon;

    public MooncakeAssemblyCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(new ItemStack(ModItems.MOONCAKE.get()));
    }

    @Override
    public RecipeType<AssemblyJeiRecipe> getRecipeType() {
        return MooncakeJeiPlugin.ASSEMBLY;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei." + MoreMooncake.MOD_ID + ".assembly");
    }

    @Override
    public int getWidth() {
        return 116;
    }

    @Override
    public int getHeight() {
        return 54;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AssemblyJeiRecipe recipe, IFocusGroup focuses) {
        for (int k = 0; k < 8; k++) {
            builder.addSlot(RecipeIngredientRole.INPUT, RING[k][0], RING[k][1])
                    .addItemStack(recipe.ring().get(k));
        }
        builder.addSlot(RecipeIngredientRole.INPUT, 19, 19)
                .addItemStack(recipe.egg());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 88, 19)
                .addItemStack(recipe.output());
    }
}
