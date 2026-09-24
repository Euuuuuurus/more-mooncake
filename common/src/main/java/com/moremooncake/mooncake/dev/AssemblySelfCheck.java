package com.moremooncake.mooncake.dev;

import com.moremooncake.mooncake.block.MooncakeBlock;
import com.moremooncake.mooncake.block.MooncakeGeometry;
import com.moremooncake.mooncake.item.WholeMooncakeItem;
import com.moremooncake.mooncake.mooncake.MooncakeFlavor;
import com.moremooncake.mooncake.mooncake.MooncakeState;
import com.moremooncake.mooncake.registry.ModItems;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.platform.Platform;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Development-only self check for the grand mooncake assembly recipe.
 * <p>
 * It runs when the game is started from a dev environment and asks the server's
 * recipe manager exactly what the crafting table would ask:
 * <ul>
 *   <li>8 slices clockwise around an egg in the middle must craft a grand mooncake,</li>
 *   <li>the resulting slice order must follow that clockwise order,</li>
 *   <li>the same 8 slices <b>without</b> the egg must not match.</li>
 * </ul>
 * A broken recipe then shows up in the log instead of silently doing nothing in game.
 */
public final class AssemblySelfCheck {
    private static final Logger LOGGER = LoggerFactory.getLogger("more_mooncake/selfcheck");
    /** ring positions in CraftingInput (x, y): top-left, then clockwise. */
    private static final int[][] RING = {
            {0, 0}, {1, 0}, {2, 0}, {2, 1}, {2, 2}, {1, 2}, {0, 2}, {0, 1}
    };

    private AssemblySelfCheck() {
    }

    public static void register() {
        if (!Platform.isDevelopmentEnvironment()) {
            return;
        }
        checkPieGeometry();
        LifecycleEvent.SERVER_STARTED.register(server -> {
            Level level = server.overworld();
            checkSurvivalRecipes(server, level);

            // 8 different slices clockwise, egg in the middle.
            List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
            grid.set(4, new ItemStack(Items.EGG));
            List<String> expected = new ArrayList<>();
            for (int k = 0; k < RING.length; k++) {
                Item item = ModItems.getAllItems().get(k).get();
                grid.set(RING[k][0] + RING[k][1] * 3, new ItemStack(item));
                expected.add(BuiltInRegistries.ITEM.getKey(item).toString());
            }
            CraftingInput input = CraftingInput.of(3, 3, grid);

            server.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                    .ifPresentOrElse(holder -> {
                        ItemStack result = holder.value().assemble(input, server.registryAccess());
                        List<String> actual = WholeMooncakeItem.getSlices(result);
                        LOGGER.info("assembly lookup OK ({}), slice order {}",
                                holder.id(), expected.equals(actual) ? "CLOCKWISE OK" : "MISMATCH");
                        if (!expected.equals(actual)) {
                            LOGGER.error("expected {} but got {}", expected, actual);
                        }
                    }, () -> LOGGER.error("assembly lookup FAILED: crafting table finds no result "
                            + "for 8 slices around an egg"));

            // Negative control: same ring, no egg in the middle.
            List<ItemStack> withoutEgg = new ArrayList<>(grid);
            withoutEgg.set(4, ItemStack.EMPTY);
            boolean matched = server.getRecipeManager()
                    .getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, withoutEgg), level)
                    .isPresent();
            LOGGER.info("without the egg matched={} (expected false)", matched);
        });
    }

    /**
     * Survival-loop checks: the slice baking recipe, the wax/scrape/deoxidize chain and the pure
     * grand-mooncake recipes must all exist and load without errors (a broken ingredient like the
     * old undefined "c:axes" tag would trip the recipe manager's error flag).
     */
    private static void checkSurvivalRecipes(MinecraftServer server, Level level) {
        var manager = server.getRecipeManager();

        // 1) Every key the survival loop depends on must exist.
        String[] keys = {
                "more_mooncake:slice_wuren", "more_mooncake:slice_dousha", "more_mooncake:slice_suzi",
                "more_mooncake:slice_hongzao", "more_mooncake:slice_xianyadan",
                "more_mooncake:wax_wuren_mooncake", "more_mooncake:oxidize_wuren_mooncake",
                "more_mooncake:mooncake_scrape",
                "more_mooncake:grand_wuren_mooncake", "more_mooncake:grand_wuren_mooncake_oxidized",
                "more_mooncake:mooncake_assembly"
        };
        int missing = 0;
        for (String key : keys) {
            if (manager.byKey(ResourceLocation.parse(key)).isEmpty()) {
                missing++;
                LOGGER.error("recipe {} is missing", key);
            }
        }
        LOGGER.info("survival recipes: {}/{} keys present", keys.length - missing, keys.length);

        // 2) The pure grand recipe must actually match: 8 identical slices + egg
        //    must craft a grand mooncake carrying 8 copies of that slice.
        Item slice = ModItems.getAllItems().get(0).get(); // wuren, fresh
        List<ItemStack> grid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
        grid.set(4, new ItemStack(Items.EGG));
        for (int[] pos : RING) {
            grid.set(pos[0] + pos[1] * 3, new ItemStack(slice));
        }
        manager.getRecipeFor(RecipeType.CRAFTING, CraftingInput.of(3, 3, grid), level)
                .ifPresentOrElse(holder -> {
                    ItemStack result = holder.value().assemble(CraftingInput.of(3, 3, grid), server.registryAccess());
                    List<String> slices = WholeMooncakeItem.getSlices(result);
                    boolean pure = slices.size() == 8 && slices.stream().allMatch(id -> id.equals(BuiltInRegistries.ITEM.getKey(slice).toString()));
                    LOGGER.info("pure grand: matched {}, slices pure={} count={}", holder.id(), pure, slices.size());
                    if (!pure) {
                        LOGGER.error("expected 8 identical wuren slices but got {}", slices);
                    }
                }, () -> LOGGER.error("pure grand lookup FAILED: 8 identical slices around an egg "
                        + "match no recipe"));

        // 3) Scraping must NOT eat the axe: waxed wuren + stone axe -> fresh wuren,
        //    and the axe comes back with exactly 1 damage.
        ItemStack waxed = new ItemStack(ModItems.getItem(MooncakeFlavor.WUREN, MooncakeState.WAXED));
        ItemStack stoneAxe = new ItemStack(Items.STONE_AXE);
        List<ItemStack> scrapeGrid = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
        scrapeGrid.set(0, waxed);
        scrapeGrid.set(1, stoneAxe);
        CraftingInput scrapeInput = CraftingInput.of(3, 3, scrapeGrid);
        manager.getRecipeFor(RecipeType.CRAFTING, scrapeInput, level)
                .ifPresentOrElse(holder -> {
                    ItemStack scraped = holder.value().assemble(scrapeInput, server.registryAccess());
                    boolean freshWuren = scraped.is(ModItems.getItem(MooncakeFlavor.WUREN, MooncakeState.NORMAL));
                    boolean axeKept = false;
                    int axeDamage = -1;
                    for (ItemStack s : holder.value().getRemainingItems(scrapeInput)) {
                        if (s.is(Items.STONE_AXE)) {
                            axeKept = true;
                            axeDamage = s.getDamageValue();
                        }
                    }
                    LOGGER.info("scrape: waxed wuren + axe matched {}, result fresh={}, axe kept={} damage={}",
                            holder.id(), freshWuren, axeKept, axeDamage);
                    if (!freshWuren || !axeKept || axeDamage != 1) {
                        LOGGER.error("scrape behaviour broken: fresh={} axeKept={} axeDamage={}",
                                freshWuren, axeKept, axeDamage);
                    }
                }, () -> LOGGER.error("scrape lookup FAILED: slice + axe matches no recipe"));

        // 4) Any recipe that failed to parse (unknown item id or tag) sets the error flag.
        if (manager.hadErrorsLoading()) {
            LOGGER.error("some recipes failed to load - check for unknown item ids or tags");
        } else {
            LOGGER.info("recipe loading: no errors (axe tags OK)");
        }
    }

    /**
     * The block models, the collision shapes and the renderer all have to describe the same pie.
     * This compares the element count of every generated bite model with the geometry table and
     * reports the shape of the first and the last state.
     */
    private static void checkPieGeometry() {
        for (int bites = 0; bites < 8; bites++) {
            int expected = 0;
            for (int k = bites; k < 8; k++) {
                expected += MooncakeGeometry.WEDGE_BOXES[k].length;
            }
            int found = countModelElements("mooncake_bite" + bites + ".json");
            if (found != expected) {
                LOGGER.error("model mooncake_bite{}.json has {} elements but the geometry table says {}"
                        + " - model, collision shape and renderer are out of sync", bites, found, expected);
            }
        }
        LOGGER.info("pie geometry: {} boxes per wedge, shape whole={} one wedge left={}",
                MooncakeGeometry.WEDGE_BOXES[0].length,
                MooncakeBlock.shapeFor(0).bounds(), MooncakeBlock.shapeFor(7).bounds());
    }

    private static int countModelElements(String name) {
        try (InputStream in = AssemblySelfCheck.class
                .getResourceAsStream("/assets/more_mooncake/models/block/" + name)) {
            if (in == null) {
                return -1;
            }
            String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            return json.split("\"from\":", -1).length - 1;
        } catch (IOException e) {
            return -2;
        }
    }
}
