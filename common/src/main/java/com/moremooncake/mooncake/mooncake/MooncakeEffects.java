package com.moremooncake.mooncake.mooncake;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared effect logic for both single slices and the grand (assembled) mooncake:
 * <ul>
 *   <li>main effect level = tier + 1 (I..IV), duration = 15s + 5s per tier</li>
 *   <li>waxed variants: main effect duration doubled (wax preserves freshness)</li>
 *   <li>oxidized (unwaxed) variants also apply a short Nausea - they have gone bad</li>
 * </ul>
 */
public final class MooncakeEffects {
    private MooncakeEffects() {
    }

    public static List<MobEffectInstance> effectsFor(MooncakeFlavor flavor, MooncakeState state) {
        int level = state.getTier() + 1;
        int durationTicks = 20 * (15 + 5 * state.getTier());
        if (state.isWaxed()) {
            durationTicks *= 2;
        }

        List<MobEffectInstance> out = new ArrayList<>();
        out.add(new MobEffectInstance(flavor.getEffect(), durationTicks, level - 1, false, true, true));
        if (state.isOxidized() && !state.isWaxed()) {
            out.add(new MobEffectInstance(MobEffects.CONFUSION, 20 * 4, 0, false, true, true));
        }
        return out;
    }
}
