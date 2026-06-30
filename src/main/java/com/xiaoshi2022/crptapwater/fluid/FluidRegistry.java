package com.xiaoshi2022.crptapwater.fluid;

import com.phagens.corpseorigin.register.Moditems;
import com.xiaoshi2022.crptapwater.CRPTapWater;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class FluidRegistry {

    // 使用NeoForgeRegistries.FLUID_TYPES (NeoForge专用注册表)
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, CRPTapWater.MODID);

    // 使用原版 Registries.FLUID
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, CRPTapWater.MODID);

    // 注册FluidType
    public static final DeferredHolder<FluidType, FluidType> CORPSE_WATER_TYPE =
            FLUID_TYPES.register("corpse_water", () -> new FluidType(
                    FluidType.Properties.create()
                            .descriptionId("fluid." + CRPTapWater.MODID + ".corpse_water")
                            .lightLevel(0)
                            .density(1000)
                            .temperature(300)
                            .viscosity(1000)
                            .canPushEntity(true)
                            .canSwim(true)
                            .canDrown(true)
                            .supportsBoating(true)
                            .fallDistanceModifier(0.5F)
                            .motionScale(0.014)
            ));

    // 注册Fluid - 使用DeferredHolder引用
    public static final DeferredHolder<Fluid, CorpseWaterFluid.Source> CORPSE_WATER;
    public static final DeferredHolder<Fluid, CorpseWaterFluid.Flowing> FLOWING_CORPSE_WATER;

    static {
        // 先创建空的DeferredHolder，稍后填充
        var sourceHolder = DeferredHolder.create(Registries.FLUID,
                ResourceLocation.fromNamespaceAndPath(CRPTapWater.MODID, "corpse_water"));
        var flowingHolder = DeferredHolder.create(Registries.FLUID,
                ResourceLocation.fromNamespaceAndPath(CRPTapWater.MODID, "flowing_corpse_water"));

        // 创建Properties
        BaseFlowingFluid.Properties properties = new BaseFlowingFluid.Properties(
                CORPSE_WATER_TYPE,
                sourceHolder,
                flowingHolder
        )
                .block(() -> com.xiaoshi2022.crptapwater.register.BlockRegistry.CORPSE_WATER_BLOCK.get())
                .bucket(() -> Moditems.BYWATER_BUCKET.get());

        // 注册Fluid
        CORPSE_WATER = FLUIDS.register("corpse_water",
                () -> new CorpseWaterFluid.Source(properties));
        FLOWING_CORPSE_WATER = FLUIDS.register("flowing_corpse_water",
                () -> new CorpseWaterFluid.Flowing(properties));
    }
}