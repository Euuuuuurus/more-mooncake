package com.moremooncake.mooncake.jei;

import com.moremooncake.mooncake.MoreMooncake;
import com.moremooncake.mooncake.item.WholeMooncakeItem;
import com.moremooncake.mooncake.mooncake.MooncakeFlavor;
import com.moremooncake.mooncake.mooncake.MooncakeState;
import com.moremooncake.mooncake.recipe.MooncakeScrapeRecipe;
import com.moremooncake.mooncake.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI integration for the two custom recipes the recipe book cannot show:
 * <ul>
 *   <li>{@code assembly} - 8 slices around an egg -> a grand mooncake
 *       (any mix works; the clockwise ring order defines the eating order);</li>
 *   <li>{@code scrape} - one slice + one axe -> the slice scraped of wax/rust,
 *       the axe only loses 1 durability.</li>
 * </ul>
 * <p>
 * The plugin lives in the <b>common</b> module and is packaged into both the fabric and the
 * neoforge jar. JEI scans {@code @JeiPlugin} classes only when JEI itself is installed, so
 * without JEI these classes are never touched and the mod stays a soft dependency.
 */
@JeiPlugin
public class MooncakeJeiPlugin implements IModPlugin {
    public static final RecipeType<AssemblyJeiRecipe> ASSEMBLY =
            RecipeType.create(MoreMooncake.MOD_ID, "assembly", AssemblyJeiRecipe.class);
    public static final RecipeType<ScrapeJeiRecipe> SCRAPE =
            RecipeType.create(MoreMooncake.MOD_ID, "scrape", ScrapeJeiRecipe.class);

    /** One example of the assembly recipe: 8 different slices around an egg. */
    public record AssemblyJeiRecipe(List<ItemStack> ring, ItemStack egg, ItemStack output) {
    }

    /** One example of the scrape recipe: slice + axe -> scraped slice. */
    public record ScrapeJeiRecipe(ItemStack input, ItemStack axe, ItemStack output) {
    }

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(MoreMooncake.MOD_ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new MooncakeAssemblyCategory(registration.getJeiHelpers().getGuiHelper()),
                new MooncakeScrapeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ASSEMBLY, List.of(buildAssemblyExample()));
        registration.addRecipes(SCRAPE, buildScrapeExamples());
    }

    /** 8 different slices around an egg, output is the matching assorted grand mooncake. */
    private static AssemblyJeiRecipe buildAssemblyExample() {
        List<String> slices = List.of(
                "more_mooncake:wuren_mooncake",
                "more_mooncake:dousha_mooncake",
                "more_mooncake:suzi_mooncake",
                "more_mooncake:hongzao_mooncake",
                "more_mooncake:xianyadan_mooncake",
                "more_mooncake:wuren_mooncake_rusted",
                "more_mooncake:dousha_mooncake_weathered",
                "more_mooncake:suzi_mooncake_oxidized");
        List<ItemStack> ring = new ArrayList<>();
        for (String id : slices) {
            ring.add(ModItems.byId(id));
        }
        return new AssemblyJeiRecipe(ring, new ItemStack(Items.EGG), WholeMooncakeItem.withSlices(slices));
    }

    /** A handful of representative scrapes covering wax removal and rust removal. */
    private static List<ScrapeJeiRecipe> buildScrapeExamples() {
        List<ScrapeJeiRecipe> out = new ArrayList<>();
        MooncakeFlavor[] flavors = MooncakeFlavor.values();
        MooncakeState[][] states = {
                {MooncakeState.WAXED, MooncakeState.NORMAL},
                {MooncakeState.WAXED_RUSTED, MooncakeState.RUSTED},
                {MooncakeState.OXIDIZED, MooncakeState.WEATHERED},
                {MooncakeState.WEATHERED, MooncakeState.RUSTED},
                {MooncakeState.RUSTED, MooncakeState.NORMAL},
        };
        for (int i = 0; i < states.length; i++) {
            ItemStack input = new ItemStack(ModItems.getItem(flavors[i % flavors.length], states[i][0]));
            ItemStack output = MooncakeScrapeRecipe.scraped(input);
            out.add(new ScrapeJeiRecipe(input, new ItemStack(Items.STONE_AXE), output));
        }
        return out;
    }
}
