package com.moremooncake.mooncake.item;

import com.moremooncake.mooncake.MoreMooncake;
import com.moremooncake.mooncake.block.MooncakeBlockEntity;
import com.moremooncake.mooncake.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * A grand mooncake: a BlockItem for the pie-shaped {@link com.moremooncake.mooncake.block.MooncakeBlock}
 * that stores the 8 slice slots in its NBT ({@link #SLICES_KEY}).
 * <p>
 * It can only be placed (right-click a block). It can <b>not</b> be split by hand any more:
 * to get slices back you cut the placed mooncake open with an axe.
 */
public class WholeMooncakeItem extends BlockItem {
    public static final String SLICES_KEY = "Slices";

    public WholeMooncakeItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult result = super.useOn(context);
        if (result.consumesAction() && !context.getLevel().isClientSide()) {
            Level level = context.getLevel();
            BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
            MooncakeBlockEntity be = null;
            if (level.getBlockEntity(pos) instanceof MooncakeBlockEntity found) {
                be = found;
            } else if (level.getBlockEntity(context.getClickedPos()) instanceof MooncakeBlockEntity found) {
                be = found;
            }
            if (be != null) {
                be.setSlices(getSlices(context.getItemInHand()));
                be.setChanged();
            }
        }
        return result;
    }

    /**
     * The name shows the full slice list in eating order, e.g.
     * "大月饼：五仁月饼块 → 豆沙月饼块 → ..." so you can see the sequence at a glance.
     */
    @Override
    public Component getName(ItemStack stack) {
        List<String> slices = getSlices(stack);
        if (slices.isEmpty()) {
            return super.getName(stack);
        }
        StringJoiner joiner = new StringJoiner(" → ");
        for (String id : slices) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
            joiner.add(item != Items.AIR ? item.getName(ItemStack.EMPTY).getString() : id);
        }
        return Component.translatable("item." + MoreMooncake.MOD_ID + ".mooncake_full", joiner.toString());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.more_mooncake.grand_place").withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.more_mooncake.grand_cut").withStyle(ChatFormatting.GRAY));
    }

    public static List<String> getSlices(ItemStack stack) {
        List<String> out = new ArrayList<>();
        CompoundTag tag = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(SLICES_KEY, Tag.TAG_LIST)) {
            ListTag list = tag.getList(SLICES_KEY, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                out.add(list.getString(i));
            }
        }
        return out;
    }

    public static ItemStack withSlices(List<String> slices) {
        ItemStack stack = new ItemStack(ModItems.MOONCAKE.get());
        ListTag list = new ListTag();
        for (String s : slices) {
            list.add(StringTag.valueOf(s));
        }
        CompoundTag tag = new CompoundTag();
        tag.put(SLICES_KEY, list);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return stack;
    }
}
