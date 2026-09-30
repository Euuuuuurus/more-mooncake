package com.moremooncake.mooncake.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Small helper for resolving item ids stored as strings (slice lists live in NBT as ids).
 * <p>
 * In 1.21.2+ {@code Registry#get(ResourceLocation)} returns an {@link java.util.Optional} of a
 * {@code Holder.Reference}, so this wraps the lookup and falls back to {@link Items#AIR} for
 * unknown ids - which keeps the call sites readable and the behaviour unchanged.
 */
public final class ItemLookup {
    private ItemLookup() {
    }

    /** Resolves an item id such as {@code more_mooncake:wuren_mooncake}, or AIR if unknown. */
    public static Item byId(String id) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(Items.AIR);
    }

    /** Resolves an item id, or {@code fallback} if the id is unknown/absent. */
    public static Item byId(String id, Item fallback) {
        return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(id)).orElse(fallback);
    }
}
