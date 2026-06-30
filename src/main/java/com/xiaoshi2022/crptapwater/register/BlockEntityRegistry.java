package com.xiaoshi2022.crptapwater.register;

import com.xiaoshi2022.crptapwater.CRPTapWater;
import com.xiaoshi2022.crptapwater.pipe.PipeBlockEntity;
import com.xiaoshi2022.crptapwater.village.VillageWaterTroughBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class BlockEntityRegistry {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CRPTapWater.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PipeBlockEntity>> PIPE_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("pipe", () -> BlockEntityType.Builder.of(
                    PipeBlockEntity::new,
                    BlockRegistry.PIPE_BLOCK.get()
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<VillageWaterTroughBlockEntity>> WATER_TROUGH_BLOCK_ENTITY =
            BLOCK_ENTITIES.register("water_trough", () -> BlockEntityType.Builder.of(
                    VillageWaterTroughBlockEntity::new,
                    BlockRegistry.WATER_TROUGH_BLOCK.get()
            ).build(null));
}
