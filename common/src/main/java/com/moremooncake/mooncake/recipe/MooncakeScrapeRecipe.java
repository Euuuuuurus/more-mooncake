package com.moremooncake.mooncake.recipe;

import com.moremooncake.mooncake.item.MooncakeFood;
import com.moremooncake.mooncake.mooncake.MooncakeFlavor;
import com.moremooncake.mooncake.mooncake.MooncakeState;
import com.moremooncake.mooncake.registry.ModItems;
import com.moremooncake.mooncake.registry.ModRecipes;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Custom tool recipe: one mooncake slice + one axe. The axe is <b>not</b> consumed -
 * it loses 1 durability and comes back in the leftover grid, exactly like scraping a
 * copper block. What the scrape does depends on the slice state:
 * <ul>
 *   <li>waxed_* -&gt; the same tier without wax (scrape the wax off)</li>
 *   <li>oxidized -&gt; weathered, weathered -&gt; rusted, rusted -&gt; fresh (scrape the patina off)</li>
 *   <li>a fresh slice has nothing to scrape and never matches</li>
 * </ul>
 */
public class MooncakeScrapeRecipe implements CraftingRecipe {
    private final CraftingBookCategory category;

    public MooncakeScrapeRecipe(CraftingBookCategory category) {
        this.category = category;
    }

    @Override
    public boolean matches(CraftingInput container, Level level) {
        if (container.width() > 3 || container.height() > 3) {
            return false;
        }
        boolean slice = false;
        boolean axe = false;
        for (int i = 0; i < container.size(); i++) {
            ItemStack s = container.getItem(i);
            if (s.isEmpty()) {
                continue;
            }
            if (s.getItem() instanceof MooncakeFood food) {
                MooncakeState state = food.mooncakeState();
                if (!state.isWaxed() && state.getTier() == 0) {
                    return false; // fresh slice: nothing to scrape
                }
                if (slice) {
                    return false;
                }
                slice = true;
            } else if (s.is(ItemTags.AXES)) {
                if (axe) {
                    return false;
                }
                axe = true;
            } else {
                return false;
            }
        }
        return slice && axe;
    }

    @Override
    public ItemStack assemble(CraftingInput container, HolderLookup.Provider access) {
        for (int i = 0; i < container.size(); i++) {
            ItemStack s = container.getItem(i);
            if (s.getItem() instanceof MooncakeFood) {
                return scraped(s);
            }
        }
        return ItemStack.EMPTY;
    }

    /** waxed_* -> unwaxed same tier; oxidized/weathered/rusted -> one tier fresher. */
    public static ItemStack scraped(ItemStack slice) {
        if (!(slice.getItem() instanceof MooncakeFood food)) {
            return slice.copy();
        }
        MooncakeState state = food.mooncakeState();
        MooncakeState target = null;
        for (MooncakeState v : MooncakeState.values()) {
            if (state.isWaxed()) {
                // waxed tier N -> the unwaxed state of the same tier
                if (!v.isWaxed() && v.getTier() == state.getTier()) {
                    target = v;
                    break;
                }
            } else if (!v.isWaxed() && v.getTier() == Math.max(0, state.getTier() - 1)) {
                target = v;
                break;
            }
        }
        return target == null ? slice.copy() : food.scrapedTo(target);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput container) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(container.size(), ItemStack.EMPTY);
        for (int i = 0; i < container.size(); i++) {
            ItemStack s = container.getItem(i);
            if (s.is(ItemTags.AXES)) {
                ItemStack axe = s.copy();
                int dmg = axe.getDamageValue() + 1;
                if (dmg >= axe.getMaxDamage()) {
                    remaining.set(i, ItemStack.EMPTY); // the axe breaks from heavy use
                } else {
                    axe.setDamageValue(dmg);
                    remaining.set(i, axe);
                }
            }
        }
        return remaining;
    }

    public boolean canCraftInDimensions(int width, int height) {
        return width >= 1 && height >= 1;
    }

    public ItemStack getResultItem(HolderLookup.Provider access) {
        return new ItemStack(ModItems.getItem(MooncakeFlavor.WUREN, MooncakeState.WAXED));
    }

    @Override
    public CraftingBookCategory category() {
        return category;
    }

    @Override
    public RecipeSerializer<MooncakeScrapeRecipe> getSerializer() {
        return ModRecipes.MOONCAKE_SCRAPE_SERIALIZER.get();
    }

    @Override
    public PlacementInfo placementInfo() {
        // No standard ingredient grid: matching uses a dynamic slice + axe check.
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    /** Serializer codec/stream - kept in sync with the serializer registered in ModRecipes. */
    public static final MapCodec<MooncakeScrapeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(CraftingBookCategory.CODEC.fieldOf("category")
                            .orElse(CraftingBookCategory.MISC)
                            .forGetter(MooncakeScrapeRecipe::category))
                    .apply(instance, MooncakeScrapeRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MooncakeScrapeRecipe> STREAM_CODEC =
            StreamCodec.composite(
                    CraftingBookCategory.STREAM_CODEC, MooncakeScrapeRecipe::category,
                    MooncakeScrapeRecipe::new);

    public static final class Serializer implements RecipeSerializer<MooncakeScrapeRecipe> {
        @Override
        public MapCodec<MooncakeScrapeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, MooncakeScrapeRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
