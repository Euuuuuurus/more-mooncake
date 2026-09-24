package com.moremooncake.mooncake.block;

import com.moremooncake.mooncake.item.MooncakeItem;
import com.moremooncake.mooncake.item.WholeMooncakeItem;
import com.moremooncake.mooncake.mooncake.MooncakeEffects;
import dev.architectury.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * The placed grand mooncake: a pie of 8 wedges ({@link MooncakeGeometry}).
 * <ul>
 *   <li>Bites 0..7: bite n has eaten wedges 0..n-1, the remaining ones are still there.</li>
 *   <li>Right-click: eat one wedge - applies that slice's effect, nothing is given back.</li>
 *   <li>Right-click with an axe (or mining it with an axe): cut it open and get the
 *       remaining slices back as items.</li>
 *   <li>Breaking it with anything else returns the (partially eaten) grand mooncake item.</li>
 * </ul>
 * The collision/outline shape is built from exactly the same wedge boxes as the block model
 * and the block entity renderer, so the shape always matches what you see.
 */
public class MooncakeBlock extends Block implements EntityBlock {
    private static final Logger LOGGER = LoggerFactory.getLogger("more_mooncake/eat");
    /** 8 bites: 0 = whole pie, 7 = one wedge left (vanilla BITES only goes 0-6). */
    public static final IntegerProperty BITES = IntegerProperty.create("bites", 0, 7);

    private static final VoxelShape[] SHAPES = new VoxelShape[8];

    static {
        for (int bites = 0; bites < 8; bites++) {
            VoxelShape shape = Shapes.empty();
            for (int k = bites; k < 8; k++) {
                for (double[] box : MooncakeGeometry.WEDGE_BOXES[k]) {
                    shape = Shapes.or(shape, Block.box(box[0], box[1], box[2], box[3], box[4], box[5]));
                }
            }
            SHAPES[bites] = shape.optimize();
        }
    }

    public MooncakeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BITES, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BITES);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(BITES)];
    }

    /** Exposed for the development self check. */
    public static VoxelShape shapeFor(int bites) {
        return SHAPES[bites];
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!(level.getBlockEntity(pos) instanceof MooncakeBlockEntity be)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        List<String> slices = be.getSlices();
        int bites = state.getValue(BITES);
        if (bites >= slices.size()) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (stack.is(ItemTags.AXES)) {
            if (!level.isClientSide()) {
                giveSlices(player, slices, bites);
                be.setSlices(List.of());
                be.setChanged();
                level.removeBlock(pos, false);
                stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!level.isClientSide()) {
            long now = level.getGameTime();
            // Same-click protection: a single right click must never eat two wedges, even if the
            // interaction is processed twice (prediction / packet re-send). 2 ticks is enough to
            // catch that while barely slowing down deliberate rapid clicking.
            if (now - be.lastEatGameTime < 2) {
                if (Platform.isDevelopmentEnvironment()) {
                    LOGGER.info("eat re-called {} ticks after the last one - blocked (bites={})",
                            now - be.lastEatGameTime, bites);
                }
                return ItemInteractionResult.sidedSuccess(false);
            }
            be.lastEatGameTime = now;
            // Eat one wedge: apply its effect. Nothing is handed back.
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(slices.get(bites)));
            if (item instanceof MooncakeItem mooncakeItem) {
                for (MobEffectInstance effect : MooncakeEffects.effectsFor(mooncakeItem.getFlavor(), mooncakeItem.getState())) {
                    player.addEffect(effect);
                }
            }
            if (bites + 1 >= 8) {
                // last wedge eaten: the pie is gone
                be.setSlices(List.of());
                be.setChanged();
                level.removeBlock(pos, false);
            } else {
                level.setBlock(pos, state.setValue(BITES, bites + 1), 3);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Hands the not yet eaten slices to the player (or drops them at their feet). */
    private static void giveSlices(Player player, List<String> slices, int from) {
        for (int k = from; k < slices.size(); k++) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(slices.get(k)));
            if (item == Items.AIR) {
                continue;
            }
            ItemStack slice = new ItemStack(item);
            if (!player.getInventory().add(slice)) {
                player.drop(slice, false);
            }
        }
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (!(params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof MooncakeBlockEntity be)) {
            return List.of();
        }
        List<String> slices = be.getSlices();
        int bites = state.getValue(BITES);
        if (bites >= slices.size()) {
            return List.of();
        }
        // An axe dismantles the pie into slices, anything else picks up the rest as an item.
        ItemStack tool = params.getParameter(LootContextParams.TOOL);
        if (tool != null && tool.is(ItemTags.AXES)) {
            List<ItemStack> drops = new ArrayList<>();
            for (int k = bites; k < slices.size(); k++) {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(slices.get(k)));
                if (item != Items.AIR) {
                    drops.add(new ItemStack(item));
                }
            }
            return drops;
        }
        return List.of(WholeMooncakeItem.withSlices(slices.subList(bites, slices.size())));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MooncakeBlockEntity(pos, state);
    }
}
