package com.moremooncake.mooncake.mooncake;

/**
 * Copper-style oxidation states of a mooncake: normal, rusted (exposed),
 * weathered, oxidized, plus the waxed (preserved) variant of each.
 * <p>
 * {@code tier} is the oxidation stage 0..3, {@code waxed} marks the preserved
 * (waxed) variant which keeps the same tier but has doubled effect duration
 * and cannot suffer oxidation side effects.
 */
public enum MooncakeState {
    NORMAL("", 0, false),
    RUSTED("rusted", 1, false),
    WEATHERED("weathered", 2, false),
    OXIDIZED("oxidized", 3, false),
    WAXED("waxed", 0, true),
    WAXED_RUSTED("waxed_rusted", 1, true),
    WAXED_WEATHERED("waxed_weathered", 2, true),
    WAXED_OXIDIZED("waxed_oxidized", 3, true);

    private final String suffix;
    private final int tier;
    private final boolean waxed;

    MooncakeState(String suffix, int tier, boolean waxed) {
        this.suffix = suffix;
        this.tier = tier;
        this.waxed = waxed;
    }

    /** Registry-name suffix, may be empty for the fresh mooncake. */
    public String getSuffix() {
        return suffix;
    }

    /** Oxidation tier 0..3 (fresh / rusted / weathered / oxidized). */
    public int getTier() {
        return tier;
    }

    public boolean isWaxed() {
        return waxed;
    }

    /** Fully oxidized (tier 3) and unwaxed mooncakes have gone bad. */
    public boolean isOxidized() {
        return tier >= 3;
    }
}
