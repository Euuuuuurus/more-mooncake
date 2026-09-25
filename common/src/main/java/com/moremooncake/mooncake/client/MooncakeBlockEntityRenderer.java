package com.moremooncake.mooncake.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.moremooncake.mooncake.block.MooncakeBlock;
import com.moremooncake.mooncake.block.MooncakeBlockEntity;
import com.moremooncake.mooncake.block.MooncakeGeometry;
import com.moremooncake.mooncake.item.MooncakeFood;
import com.moremooncake.mooncake.mooncake.MooncakeState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

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
public class MooncakeBlockEntityRenderer implements BlockEntityRenderer<MooncakeBlockEntity> {
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
    public void render(MooncakeBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffer,
                       int light, int overlay) {
        List<String> slices = be.getSlices();
        if (slices.isEmpty()) {
            return;
        }
        int bites = be.getBlockState().getValue(MooncakeBlock.BITES);
        if (bites >= 8) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(ResourceLocation.fromNamespaceAndPath("more_mooncake", "block/mooncake_top"));
        float u0 = sprite.getU0(), u1 = sprite.getU1();
        float v0 = sprite.getV0(), v1 = sprite.getV1();

        VertexConsumer vc = buffer.getBuffer(RenderType.cutout());
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
                vc.addVertex(pose.last().pose(), x0, y, z0).setColor(c[0], c[1], c[2], 255).setUv(su0, sv0).setLight(light).setOverlay(overlay);
                vc.addVertex(pose.last().pose(), x0, y, z1).setColor(c[0], c[1], c[2], 255).setUv(su0, sv1).setLight(light).setOverlay(overlay);
                vc.addVertex(pose.last().pose(), x1, y, z1).setColor(c[0], c[1], c[2], 255).setUv(su1, sv1).setLight(light).setOverlay(overlay);
                vc.addVertex(pose.last().pose(), x1, y, z0).setColor(c[0], c[1], c[2], 255).setUv(su1, sv0).setLight(light).setOverlay(overlay);
            }
        }
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

    private int[] colorFor(String sliceId) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(sliceId));
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
}
