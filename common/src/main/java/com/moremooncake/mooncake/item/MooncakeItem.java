package com.moremooncake.mooncake.item;

import com.moremooncake.mooncake.mooncake.MooncakeEffects;
import com.moremooncake.mooncake.mooncake.MooncakeFlavor;
import com.moremooncake.mooncake.mooncake.MooncakeState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A mooncake slice item (1/8 of a grand mooncake). Food properties are baked
 * per flavor + state:
 * <ul>
 *   <li>nutrition is low (1) - it is only one slice</li>
 *   <li>main effect level = tier + 1 (I..IV), duration = 15s + 5s per tier</li>
 *   <li>waxed variants: main effect duration doubled (wax preserves freshness)</li>
 *   <li>oxidized (unwaxed) variants also apply a short Nausea - they have gone bad</li>
 * </ul>
 * Eight slices (any combination) can be crafted into a grand mooncake.
 */
public class MooncakeItem extends Item implements MooncakeFood {
    private final MooncakeFlavor flavor;
    private final MooncakeState state;

    public MooncakeItem(MooncakeFlavor flavor, MooncakeState state) {
        super(createProperties(flavor, state));
        this.flavor = flavor;
        this.state = state;
    }

    private static Item.Properties createProperties(MooncakeFlavor flavor, MooncakeState state) {
        FoodProperties.Builder food = new FoodProperties.Builder()
                .nutrition(1)
                .saturationModifier(0.15F)
                .alwaysEdible();
        for (MobEffectInstance effect : MooncakeEffects.effectsFor(flavor, state)) {
            food.effect(effect, 1.0F);
        }
        return new Item.Properties().food(food.build());
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.more_mooncake.festival").withStyle(ChatFormatting.GOLD));
        tooltipComponents.add(Component.translatable("tooltip.more_mooncake.slice_hint").withStyle(ChatFormatting.DARK_GRAY));
        if (state.isWaxed()) {
            tooltipComponents.add(Component.translatable("tooltip.more_mooncake.waxed").withStyle(ChatFormatting.GRAY));
        }
        if (state.isOxidized() && !state.isWaxed()) {
            tooltipComponents.add(Component.translatable("tooltip.more_mooncake.oxidized_side_effect").withStyle(ChatFormatting.DARK_RED));
        }
    }

    public MooncakeFlavor getFlavor() {
        return flavor;
    }

    public MooncakeState getState() {
        return state;
    }

    // ---------------- MooncakeFood ----------------

    @Override
    public MooncakeState mooncakeState() {
        return state;
    }

    @Override
    public List<MobEffectInstance> effectsFor(MooncakeState ignore) {
        return MooncakeEffects.effectsFor(flavor, state);
    }

    /** Rough average filling colour per base flavour, matching the block entity renderer. */
    private static final int[][] FLAVOR_COLORS = {
            {240, 230, 206}, // wuren - seed cream
            {122, 42, 42},   // dousha - red bean
            {74, 46, 99},    // suzi - perilla purple
            {176, 58, 58},   // hongzao - red date
            {240, 162, 58}   // xianyadan - yolk orange
    };

    @Override
    public int[] fillingColor() {
        return FLAVOR_COLORS[flavor.ordinal()].clone();
    }

    @Override
    public ItemStack scrapedTo(MooncakeState target) {
        return new ItemStack(com.moremooncake.mooncake.registry.ModItems.getItem(flavor, target));
    }
}
