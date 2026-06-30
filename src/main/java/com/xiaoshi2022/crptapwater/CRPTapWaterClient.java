package com.xiaoshi2022.crptapwater;

import com.xiaoshi2022.crptapwater.client.renderer.block.WaterTroughBlockRenderer;
import com.xiaoshi2022.crptapwater.register.BlockEntityRegistry;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = CRPTapWater.MODID, value = Dist.CLIENT)
public class CRPTapWaterClient {

    @SubscribeEvent
    public static void registerRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                BlockEntityRegistry.WATER_TROUGH_BLOCK_ENTITY.get(),
                WaterTroughBlockRenderer::new
        );
        CRPTapWater.LOGGER.info("已注册水槽 Geo 渲染器: WaterTroughBlockRenderer");
    }
}
