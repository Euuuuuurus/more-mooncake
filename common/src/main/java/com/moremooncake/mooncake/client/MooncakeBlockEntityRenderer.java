package com.moremooncake.mooncake.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.moremooncake.mooncake.block.MooncakeBlock;
import com.moremooncake.mooncake.block.MooncakeBlockEntity;
import com.moremooncake.mooncake.block.MooncakeGeometry;
import com.moremooncake.mooncake.item.MooncakeFood;
import com.moremooncake.mooncake.mooncake.MooncakeState;
import com.moremooncake.mooncake.util.ItemLookup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Tints every remaining wedge of the placed grand mooncake with the filling colour of the slice
 * that sits in that slot, so the block visibly shows what it was crafted from - and you can watch
 * the 8 pieces disappear one by one while eating. Wedge 0 (top-left) is eaten first.
 * <p>
 * The tint is drawn as an opaque copy of the {@code mooncake_top} sheet over each wedge, sampling
 * exactly the uv rectangle the block model uses for that wedge. Because the tint is multiplicative
 * it keeps the baked crust texture and the cut lines, and the colour is pre-compensated by
 * {@link #tintFor} so the result lands on the intended filling colour instead of turning muddy.
 */
public class MooncakeBlockEntityRenderer implements BlockEntityRenderer<MooncakeBlockEntity, MooncakeBlockEntityRenderer.State> {
    /** Rough average colour of the top sheet, used to pre-compensate the multiplicative tint. */
    private static final int[] CRUST = {228, 170, 100};
    private static final int[][] TIER_COLORS = {
            {194, 112, 59},  // rusted
            {169, 166, 90},  // weathered
            {95, 175, 158}   // oxidized
    };

    public MooncakeBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MooncakeBlockEntity be, State state, float partialTick, Vec3 cameraPos,
                                   CrumblingOverlay crumblingOverlay) {
        state.slices = be.getSlices();
        state.bites = be.getBlockState().getValue(MooncakeBlock.BITES);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState cameraRenderState) {
        List<String> slices = state.slices;
        int bites = state.bites;
        if (slices.isEmpty() || bites >= 8) {
            return;
        }
        TextureAtlasSprite sprite = atlasSprite("block/mooncake_top");
        if (sprite == null) {
            return;
        }
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();
        int light = state.lightCoords;

        collector.submitCustomGeometry(pose, Sheets.cutoutBlockItemSheet(), (p, vc) -> {
            for (int k = bites; k < 8 && k < slices.size(); k++) {
                int[] c = tintFor(colorFor(slices.get(k)));
                for (double[] box : MooncakeGeometry.WEDGE_BOXES[k]) {
                    float y = (float) (box[4] / 16.0) + 0.002F;
                    float x0 = (float) (box[0] / 16.0);
                    float x1 = (float) (box[3] / 16.0);
                    float z0 = (float) (box[2] / 16.0);
                    float z1 = (float) (box[5] / 16.0);
                    // same uv rectangle the block model uses for the up face of this box
                    float su0 = u0 + (float) (box[0] / 16.0) * (u1 - u0);
                    float su1 = u0 + (float) (box[3] / 16.0) * (u1 - u0);
                    float sv0 = v0 + (float) (box[2] / 16.0) * (v1 - v0);
                    float sv1 = v0 + (float) (box[5] / 16.0) * (v1 - v0);
                    // top face, counter clockwise seen from above
                    vc.addVertex(p.pose(), x0, y, z0).setColor(c[0], c[1], c[2], 255).setUv(su0, sv0).setLight(light);
                    vc.addVertex(p.pose(), x0, y, z1).setColor(c[0], c[1], c[2], 255).setUv(su0, sv1).setLight(light);
                    vc.addVertex(p.pose(), x1, y, z1).setColor(c[0], c[1], c[2], 255).setUv(su1, sv1).setLight(light);
                    vc.addVertex(p.pose(), x1, y, z0).setColor(c[0], c[1], c[2], 255).setUv(su1, sv0).setLight(light);
                }
            }
        });
    }

    /** Looks up a sprite in the block texture atlas, or null if the atlas/sprite is missing. */
    private static TextureAtlasSprite atlasSprite(String path) {
        if (Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS) instanceof TextureAtlas atlas) {
            return atlas.getSprite(Identifier.fromNamespaceAndPath("more_mooncake", path));
        }
        return null;
    }

    /**
     * Vertex colour is multiplied with the texture, so a plain filling colour would come out dark
     * and muddy. Dividing by the crust colour first makes the product land on the wanted colour
     * while keeping the shading and the cut lines of the sheet.
     */
    private static int[] tintFor(int[] c) {
        int[] t = new int[3];
        double scale = 1.0;
        for (int i = 0; i < 3; i++) {
            t[i] = (int) Math.round(c[i] * 255.0 / CRUST[i]);
            if (t[i] > 255) {
                scale = Math.max(scale, t[i] / 255.0);
            }
        }
        if (scale > 1.0) {
            for (int i = 0; i < 3; i++) {
                t[i] = (int) Math.round(t[i] / scale);
            }
        }
        return t;
    }

    private static int[] colorFor(String sliceId) {
        Item item = ItemLookup.byId(sliceId);
        if (item instanceof MooncakeFood food) {
            MooncakeState state = food.mooncakeState();
            int[] c = food.fillingColor();
            if (state.getTier() > 0) {
                c = mix(c, TIER_COLORS[state.getTier() - 1], 0.25 * state.getTier());
            }
            if (state.isWaxed()) {
                c = mix(c, new int[]{255, 255, 255}, 0.15);
            }
            return c;
        }
        return new int[]{224, 164, 94};
    }

    private static int[] mix(int[] a, int[] b, double t) {
        return new int[]{(int) (a[0] + (b[0] - a[0]) * t), (int) (a[1] + (b[1] - a[1]) * t), (int) (a[2] + (b[2] - a[2]) * t)};
    }

    /** Render state: snaps the BE data once per frame for the render thread. */
    public static class State extends BlockEntityRenderState {
        public List<String> slices = List.of();
        public int bites;
    }
}