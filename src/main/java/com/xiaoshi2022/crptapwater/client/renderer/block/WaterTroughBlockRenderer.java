package com.xiaoshi2022.crptapwater.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.xiaoshi2022.crptapwater.client.models.block.WaterTroughGeoModel;
import com.xiaoshi2022.crptapwater.village.VillageWaterTroughBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public class WaterTroughBlockRenderer extends GeoBlockRenderer<VillageWaterTroughBlockEntity> {

    public WaterTroughBlockRenderer(BlockEntityRendererProvider.Context context) {
        super(new WaterTroughGeoModel());
    }

    @Override
    public void preRender(PoseStack poseStack, VillageWaterTroughBlockEntity animatable,
                          BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          int colour) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, colour);

        boolean hasWater = !animatable.getFluidStorage().isEmpty();
        boolean polluted = animatable.isWaterPolluted();

        GeoBone hotWaterDrop = model.getBone("hot_water").orElse(null);
        if (hotWaterDrop != null) {
            if (!hasWater) {
                hotWaterDrop.setHidden(true);
            } else {
                hotWaterDrop.setHidden(false);
                if (polluted) {
                    hotWaterDrop.setScaleX(1.2F);
                    hotWaterDrop.setScaleY(1.2F);
                    hotWaterDrop.setScaleZ(1.2F);
                }
            }
        }

        GeoBone coldWaterDrop = model.getBone("cold water").orElse(null);
        if (coldWaterDrop != null) {
            if (!hasWater) {
                coldWaterDrop.setHidden(true);
            } else {
                coldWaterDrop.setHidden(false);
                if (polluted) {
                    coldWaterDrop.setScaleX(1.2F);
                    coldWaterDrop.setScaleY(1.2F);
                    coldWaterDrop.setScaleZ(1.2F);
                }
            }
        }

        GeoBone tankBone = model.getBone("water tank").orElse(null);
        if (tankBone != null && hasWater) {
            float ratio = animatable.getWaterFillRatio();
            float scale = 0.2F + ratio * 0.8F;
            tankBone.setScaleY(0.5F + scale * 0.5F);
        } else if (tankBone != null) {
            tankBone.setScaleY(0.2F);
        }
    }

    @Override
    public AABB getRenderBoundingBox(VillageWaterTroughBlockEntity blockEntity) {
        BlockPos pos = blockEntity.getBlockPos();
        return new AABB(
                pos.getX() - 0.3, pos.getY(), pos.getZ() - 0.3,
                pos.getX() + 1.3, pos.getY() + 2.1, pos.getZ() + 1.3
        );
    }
}
