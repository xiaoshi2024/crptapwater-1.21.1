package com.xiaoshi2022.crptapwater.client.models.block;

import com.xiaoshi2022.crptapwater.village.VillageWaterTroughBlockEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;

import static com.xiaoshi2022.crptapwater.CRPTapWater.MODID;

public class WaterTroughGeoModel extends DefaultedBlockGeoModel<VillageWaterTroughBlockEntity> {

    private static final ResourceLocation TEXTURE_CLEAN =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/water_trough_clean.png");
    private static final ResourceLocation TEXTURE_POLLUTED =
            ResourceLocation.fromNamespaceAndPath(MODID, "textures/block/water_trough_polluted.png");

    public WaterTroughGeoModel() {
        super(ResourceLocation.fromNamespaceAndPath(MODID, "water_trough"));
    }

    @Override
    public ResourceLocation getTextureResource(VillageWaterTroughBlockEntity animatable) {
        if (animatable.isWaterPolluted()) {
            return TEXTURE_POLLUTED;
        }
        return TEXTURE_CLEAN;
    }

    @Override
    public RenderType getRenderType(VillageWaterTroughBlockEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutoutNoCull(texture);
    }
}
