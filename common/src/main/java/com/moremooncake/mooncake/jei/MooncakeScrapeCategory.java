package com.moremooncake.mooncake.jei;

import com.moremooncake.mooncake.MoreMooncake;
import com.moremooncake.mooncake.jei.MooncakeJeiPlugin.ScrapeJeiRecipe;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Shows the scrape recipe: one slice + one axe. The axe is NOT consumed - it only loses
 * 1 durability, exactly like scraping a copper block. The result depends on the slice:
 * waxed -> unwaxed (same tier), oxidized -> weathered -> rusted -> fresh.
 */
public class MooncakeScrapeCategory implements IRecipeCategory<ScrapeJeiRecipe> {
    private final IDrawable icon;

    public MooncakeScrapeCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemStack(new ItemStack(Items.STONE_AXE));
    }

    @Override
    public RecipeType<ScrapeJeiRecipe> getRecipeType() {
        return MooncakeJeiPlugin.SCRAPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei." + MoreMooncake.MOD_ID + ".scrape");
    }

    @Override
    public int getWidth() {
        return 96;
    }

    @Override
    public int getHeight() {
        return 34;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ScrapeJeiRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 1, 1)
                .addItemStack(recipe.input());
        builder.addSlot(RecipeIngredientRole.INPUT, 19, 1)
                .addItemStack(recipe.axe());
        builder.addSlot(RecipeIngredientRole.OUTPUT, 60, 1)
                .addItemStack(recipe.output());
    }
}
