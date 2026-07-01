package com.xiaoshi2022.crptapwater.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xiaoshi2022.crptapwater.fluid.FluidRegistry;
import com.xiaoshi2022.crptapwater.pipe.PipeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.joml.Matrix4f;

public class PipeBlockEntityRenderer implements BlockEntityRenderer<PipeBlockEntity> {

    private static final float INNER_MIN = 6.0F / 16.0F;
    private static final float INNER_MAX = 10.0F / 16.0F;

    public PipeBlockEntityRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public void render(PipeBlockEntity pipe, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        FluidStack fluid = pipe.getFluidStorage().getFluid();
        int amount = pipe.getFluidStorage().getFluidAmount();
        if (amount <= 0 || fluid.isEmpty()) return;

        int capacity = pipe.getFluidStorage().getCapacity();
        float fillRatio = Math.min(1.0F, (float) amount / (float) capacity);
        float maxY = INNER_MIN + (INNER_MAX - INNER_MIN) * fillRatio;

        Fluid f = fluid.getFluid();
        ResourceLocation stillTexture;
        int tintColor;
        if (f.isSame(FluidRegistry.CORPSE_WATER.get())) {
            stillTexture = ResourceLocation.withDefaultNamespace("block/water_still");
            tintColor = 0xCC2A0040;
        } else if (f.isSame(Fluids.WATER) || f.isSame(Fluids.FLOWING_WATER)) {
            stillTexture = ResourceLocation.withDefaultNamespace("block/water_still");
            tintColor = 0xE63F76E4;
        } else {
            stillTexture = ResourceLocation.withDefaultNamespace("block/water_still");
            tintColor = 0xFFFFFFFF;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(stillTexture);

        if (sprite == null) return;

        VertexConsumer consumer = bufferSource.getBuffer(RenderType.translucent());
        Matrix4f pose = poseStack.last().pose();

        renderFluidCube(pose, consumer, sprite, INNER_MIN, INNER_MIN, INNER_MIN,
                INNER_MAX, maxY, INNER_MAX, tintColor, packedLight, packedOverlay);
    }

    private void renderFluidCube(Matrix4f pose, VertexConsumer consumer, TextureAtlasSprite sprite,
                                 float minX, float minY, float minZ,
                                 float maxX, float maxY, float maxZ,
                                 int color, int light, int overlay) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        // up (+Y)
        vertex(pose, consumer, minX, maxY, minZ, r, g, b, a, u0, v0, light, overlay, Direction.UP);
        vertex(pose, consumer, minX, maxY, maxZ, r, g, b, a, u0, v1, light, overlay, Direction.UP);
        vertex(pose, consumer, maxX, maxY, maxZ, r, g, b, a, u1, v1, light, overlay, Direction.UP);
        vertex(pose, consumer, maxX, maxY, minZ, r, g, b, a, u1, v0, light, overlay, Direction.UP);

        // down (-Y)
        vertex(pose, consumer, minX, minY, maxZ, r, g, b, a, u0, v0, light, overlay, Direction.DOWN);
        vertex(pose, consumer, minX, minY, minZ, r, g, b, a, u0, v1, light, overlay, Direction.DOWN);
        vertex(pose, consumer, maxX, minY, minZ, r, g, b, a, u1, v1, light, overlay, Direction.DOWN);
        vertex(pose, consumer, maxX, minY, maxZ, r, g, b, a, u1, v0, light, overlay, Direction.DOWN);

        // north (-Z)
        vertex(pose, consumer, maxX, minY, minZ, r, g, b, a, u0, v0, light, overlay, Direction.NORTH);
        vertex(pose, consumer, minX, minY, minZ, r, g, b, a, u1, v0, light, overlay, Direction.NORTH);
        vertex(pose, consumer, minX, maxY, minZ, r, g, b, a, u1, v1, light, overlay, Direction.NORTH);
        vertex(pose, consumer, maxX, maxY, minZ, r, g, b, a, u0, v1, light, overlay, Direction.NORTH);

        // south (+Z)
        vertex(pose, consumer, minX, minY, maxZ, r, g, b, a, u0, v0, light, overlay, Direction.SOUTH);
        vertex(pose, consumer, maxX, minY, maxZ, r, g, b, a, u1, v0, light, overlay, Direction.SOUTH);
        vertex(pose, consumer, maxX, maxY, maxZ, r, g, b, a, u1, v1, light, overlay, Direction.SOUTH);
        vertex(pose, consumer, minX, maxY, maxZ, r, g, b, a, u0, v1, light, overlay, Direction.SOUTH);

        // west (-X)
        vertex(pose, consumer, minX, minY, minZ, r, g, b, a, u0, v0, light, overlay, Direction.WEST);
        vertex(pose, consumer, minX, minY, maxZ, r, g, b, a, u1, v0, light, overlay, Direction.WEST);
        vertex(pose, consumer, minX, maxY, maxZ, r, g, b, a, u1, v1, light, overlay, Direction.WEST);
        vertex(pose, consumer, minX, maxY, minZ, r, g, b, a, u0, v1, light, overlay, Direction.WEST);

        // east (+X)
        vertex(pose, consumer, maxX, minY, maxZ, r, g, b, a, u0, v0, light, overlay, Direction.EAST);
        vertex(pose, consumer, maxX, minY, minZ, r, g, b, a, u1, v0, light, overlay, Direction.EAST);
        vertex(pose, consumer, maxX, maxY, minZ, r, g, b, a, u1, v1, light, overlay, Direction.EAST);
        vertex(pose, consumer, maxX, maxY, maxZ, r, g, b, a, u0, v1, light, overlay, Direction.EAST);
    }

    private void vertex(Matrix4f pose, VertexConsumer consumer,
                        float x, float y, float z,
                        int r, int g, int b, int a,
                        float u, float v, int light, int overlay, Direction normal) {
        consumer.addVertex(pose, x, y, z)
                .setColor(r, g, b, a)
                .setUv(u, v)
                .setOverlay(overlay)
                .setLight(light)
                .setNormal(normal.getStepX(), normal.getStepY(), normal.getStepZ());
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
