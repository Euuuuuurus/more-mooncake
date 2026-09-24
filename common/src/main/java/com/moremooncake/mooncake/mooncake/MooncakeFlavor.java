package com.moremooncake.mooncake.mooncake;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

/**
 * The five mooncake flavors.
 * Each flavor has a base nutrition, saturation and a signature status effect.
 */
public enum MooncakeFlavor {
    WUREN("wuren", 6, 0.7F, MobEffects.DAMAGE_BOOST),
    DOUSHA("dousha", 7, 0.8F, MobEffects.REGENERATION),
    SUZI("suzi", 6, 0.7F, MobEffects.JUMP),
    HONGZAO("hongzao", 6, 0.7F, MobEffects.ABSORPTION),
    XIANYADAN("xianyadan", 7, 0.8F, MobEffects.DAMAGE_RESISTANCE);

    private final String registryName;
    private final int nutrition;
    private final float saturation;
    private final Holder<MobEffect> effect;

    MooncakeFlavor(String registryName, int nutrition, float saturation, Holder<MobEffect> effect) {
        this.registryName = registryName;
        this.nutrition = nutrition;
        this.saturation = saturation;
        this.effect = effect;
    }

    public String getRegistryName() {
        return registryName;
    }

    public int getNutrition() {
        return nutrition;
    }

    public float getSaturation() {
        return saturation;
    }

    public Holder<MobEffect> getEffect() {
        return effect;
    }
}
