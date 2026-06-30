package com.xiaoshi2022.crptapwater.register;

import com.xiaoshi2022.crptapwater.CRPTapWater;
import com.xiaoshi2022.crptapwater.fluid.FluidRegistry;
import com.xiaoshi2022.crptapwater.pipe.PipeBlock;
import com.xiaoshi2022.crptapwater.village.VillageWaterTroughBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockRegistry {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(CRPTapWater.MODID);

    public static final DeferredBlock<LiquidBlock> CORPSE_WATER_BLOCK =
            BLOCKS.registerBlock("corpse_water",
                    properties -> new LiquidBlock(
                            FluidRegistry.CORPSE_WATER.get(),
                            properties.noCollission()
                                    .strength(100.0F)
                                    .noLootTable()
                                    .liquid()
                    ),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED)
                            .noCollission()
                            .strength(100.0F)
                            .noLootTable()
                            .liquid());

    public static final DeferredBlock<PipeBlock> PIPE_BLOCK =
            BLOCKS.registerBlock("pipe",
                    properties -> new PipeBlock(properties),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.METAL)
                            .strength(2.0F, 6.0F)
                            .sound(SoundType.METAL)
                            .requiresCorrectToolForDrops()
            );

    public static final DeferredBlock<VillageWaterTroughBlock> WATER_TROUGH_BLOCK =
            BLOCKS.registerBlock("water_trough",
                    properties -> new VillageWaterTroughBlock(properties),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.WOOD)
                            .strength(1.5F, 6.0F)
                            .sound(SoundType.WOOD)
                            .requiresCorrectToolForDrops()
            );
}
