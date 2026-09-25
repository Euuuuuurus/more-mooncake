package com.moremooncake.mooncake.item;

import com.moremooncake.mooncake.mooncake.MooncakeState;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Extension point shared by the base {@code MooncakeItem} and any addon slice item.
 * <p>
 * Everything that reads a mooncake slice - the grand-mooncake assembly recipe, the placed
 * block's eat behaviour, the block entity renderer (tint colour) and the scrape/rust recipe -
 * talks to this interface instead of the concrete {@code MooncakeItem}. That keeps the base mod
 * untouched for its 5 flavours while letting an addon register many more slices that still flow
 * through the same grand-mooncake assemble/split pipeline.
 */
public interface MooncakeFood {
    /** The copper-style oxidation/preservation state of this slice. */
    MooncakeState mooncakeState();

    /**
     * Every status effect this slice would apply when eaten (single flavour: one effect;
     * two-filling addon slices: the union of both flavours' effects). Already tier-scaled.
     */
    List<MobEffectInstance> effectsFor(MooncakeState state);

    /**
     * The base filling colour used to tint the wedge of a placed grand mooncake, as an
     * {r, g, b} triple (0-255). Tier and wax shading is applied by the renderer on top of it.
     */
    int[] fillingColor();

    /**
     * The slice of the same kind (flavour combination) but at the given target state,
     * used by the scrape recipe to convert wax/rust without losing the flavour.
     */
    ItemStack scrapedTo(MooncakeState target);
}