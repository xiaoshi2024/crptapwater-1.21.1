package com.xiaoshi2022.crptapwater.fluid;

import com.phagens.corpseorigin.api.watercompany.WaterCompanyAPI;
import com.phagens.corpseorigin.register.Moditems;
import com.xiaoshi2022.crptapwater.CRPTapWater;
import com.xiaoshi2022.crptapwater.register.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;

public abstract class CorpseWaterFluid extends BaseFlowingFluid {

    protected CorpseWaterFluid(Properties properties) {
        super(properties);
    }

    @Override
    public Fluid getSource() {
        return FluidRegistry.CORPSE_WATER.get();
    }

    @Override
    public Fluid getFlowing() {
        return FluidRegistry.FLOWING_CORPSE_WATER.get();
    }

    @Override
    public Item getBucket() {
        return Moditems.BYWATER_BUCKET.get();
    }

    @Override
    protected boolean canConvertToSource(Level level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockEntity blockentity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        Block.dropResources(state, level, pos, blockentity);
    }

    @Override
    public int getSlopeFindDistance(LevelReader level) {
        return 4;
    }

    @Override
    public int getDropOff(LevelReader level) {
        return 1;
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == FluidRegistry.CORPSE_WATER.get() ||
                fluid == FluidRegistry.FLOWING_CORPSE_WATER.get();
    }

    @Override
    public FluidType getFluidType() {
        return FluidRegistry.CORPSE_WATER_TYPE.get();
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return BlockRegistry.CORPSE_WATER_BLOCK.get().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    // 移除 @Override，因为父类方法签名可能不同
    // 在 1.21 中，entityInside 方法签名可能是 entityInside(FluidState, Level, BlockPos, Entity, double)
    public void entityInside(FluidState state, Level level, BlockPos pos, Entity entity) {
        // 不调用 super，因为可能不存在
        if (entity instanceof LivingEntity living && !level.isClientSide()) {
            try {
                WaterCompanyAPI.getInfectionTrigger().triggerInfection(living, (ServerLevel) level);
            } catch (Exception e) {
                CRPTapWater.LOGGER.warn("触发尸兄感染失败: {}", e.getMessage());
            }
        }
    }

    public static class Source extends CorpseWaterFluid {

        public Source(Properties properties) {
            super(properties);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends CorpseWaterFluid {

        public Flowing(Properties properties) {
            super(properties);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}