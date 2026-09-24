package com.moremooncake.mooncake.registry;

import com.moremooncake.mooncake.MoreMooncake;
import com.moremooncake.mooncake.item.MooncakeItem;
import com.moremooncake.mooncake.item.WholeMooncakeItem;
import com.moremooncake.mooncake.mooncake.MooncakeFlavor;
import com.moremooncake.mooncake.mooncake.MooncakeState;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Registers all 40 mooncake slice items (5 flavors x 8 states), the grand
 * mooncake item and the mod's creative tab, entirely through Architectury
 * APIs in the common module.
 */
public final class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(MoreMooncake.MOD_ID, Registries.ITEM);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(MoreMooncake.MOD_ID, Registries.CREATIVE_MODE_TAB);

    private static final List<RegistrySupplier<Item>> ALL_ITEMS = new ArrayList<>();
    public static final RegistrySupplier<Item> MOONCAKE;

    static {
        for (MooncakeFlavor flavor : MooncakeFlavor.values()) {
            for (MooncakeState state : MooncakeState.values()) {
                ALL_ITEMS.add(registerItem(itemId(flavor, state), () -> new MooncakeItem(flavor, state)));
            }
        }

        MOONCAKE = ITEMS.register("mooncake", () -> new WholeMooncakeItem(ModBlocks.MOONCAKE_BLOCK.get(),
                new Item.Properties().stacksTo(1)));

        TABS.register("more_mooncake_tab", () -> CreativeTabRegistry.create(builder -> {
            builder.title(Component.translatable("itemGroup.more_mooncake"));
            builder.icon(() -> new ItemStack(ALL_ITEMS.get(0).get()));
            builder.displayItems((params, output) -> {
                for (RegistrySupplier<Item> item : ALL_ITEMS) {
                    output.accept(new ItemStack(item.get()));
                }
                // Grand mooncake examples: one pure five-kernel, one assorted.
                output.accept(WholeMooncakeItem.withSlices(
                        java.util.Collections.nCopies(8, "more_mooncake:wuren_mooncake")));
                output.accept(WholeMooncakeItem.withSlices(List.of(
                        "more_mooncake:wuren_mooncake",
                        "more_mooncake:dousha_mooncake",
                        "more_mooncake:suzi_mooncake",
                        "more_mooncake:hongzao_mooncake",
                        "more_mooncake:xianyadan_mooncake",
                        "more_mooncake:wuren_mooncake_rusted",
                        "more_mooncake:dousha_mooncake_weathered",
                        "more_mooncake:suzi_mooncake_oxidized")));
            });
        }));
    }

    private ModItems() {
    }

    /** Called from the platform entry points (after ModBlocks.register()). */
    public static void register() {
        TABS.register();
        ITEMS.register();
    }

    public static List<RegistrySupplier<Item>> getAllItems() {
        return ALL_ITEMS;
    }

    /** The slice item for a flavour x state pair (registration order: flavour, then state). */
    public static Item getItem(MooncakeFlavor flavor, MooncakeState state) {
        return ALL_ITEMS.get(flavor.ordinal() * MooncakeState.values().length + state.ordinal()).get();
    }

    /** A stack of the item registered under {@code "more_mooncake:<id>"}, or empty if unknown. */
    public static ItemStack byId(String id) {
        ResourceLocation loc = id.contains(":")
                ? ResourceLocation.parse(id)
                : ResourceLocation.fromNamespaceAndPath("more_mooncake", id);
        Item item = BuiltInRegistries.ITEM.get(loc);
        return item == null || item == Items.AIR ? ItemStack.EMPTY : new ItemStack(item);
    }

    public static String itemId(MooncakeFlavor flavor, MooncakeState state) {
        String suffix = state.getSuffix();
        return flavor.getRegistryName() + "_mooncake" + (suffix.isEmpty() ? "" : "_" + suffix);
    }

    private static RegistrySupplier<Item> registerItem(String name, Supplier<Item> supplier) {
        return ITEMS.register(name, supplier);
    }
}
