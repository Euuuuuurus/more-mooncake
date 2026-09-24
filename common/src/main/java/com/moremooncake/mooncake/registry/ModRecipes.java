package com.moremooncake.mooncake.registry;

import com.moremooncake.mooncake.MoreMooncake;
import com.moremooncake.mooncake.recipe.MooncakeAssemblyRecipe;
import com.moremooncake.mooncake.recipe.MooncakeScrapeRecipe;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

/**
 * Registers the custom recipe serializers:
 * <ul>
 *   <li>{@code mooncake_assembly} - 8 slices around an egg into a grand mooncake;</li>
 *   <li>{@code mooncake_scrape} - 1 slice + 1 axe, the axe keeps 1 durability.</li>
 * </ul>
 * Only the serializers are custom; both recipes deliberately report
 * {@link RecipeType#CRAFTING} as their type so the vanilla crafting table can find them.
 */
public final class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(MoreMooncake.MOD_ID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeSerializer<MooncakeAssemblyRecipe>> MOONCAKE_ASSEMBLY_SERIALIZER =
            SERIALIZERS.register("mooncake_assembly",
                    () -> new SimpleCraftingRecipeSerializer<>(MooncakeAssemblyRecipe::new));

    public static final RegistrySupplier<RecipeSerializer<MooncakeScrapeRecipe>> MOONCAKE_SCRAPE_SERIALIZER =
            SERIALIZERS.register("mooncake_scrape",
                    () -> new SimpleCraftingRecipeSerializer<>(MooncakeScrapeRecipe::new));

    private ModRecipes() {
    }

    public static void register() {
        SERIALIZERS.register();
    }
}
