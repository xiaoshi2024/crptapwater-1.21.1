package com.xiaoshi2022.crptapwater.api;

import com.phagens.corpseorigin.api.watercompany.ICorpseWaterHandler;
import com.phagens.corpseorigin.api.watercompany.WaterCompanyAPI;
import com.phagens.corpseorigin.data.InfectionData;
import com.phagens.corpseorigin.register.Moditems;
import com.xiaoshi2022.crptapwater.fluid.FluidRegistry;
import com.xiaoshi2022.crptapwater.register.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public class CustomCorpseWaterHandler implements ICorpseWaterHandler {

    @Override
    public Fluid getCorpseWaterFluid() {
        return FluidRegistry.CORPSE_WATER.get();
    }

    @Override
    public boolean isCorpseWater(Fluid fluid) {
        return fluid == FluidRegistry.CORPSE_WATER.get() ||
               fluid == FluidRegistry.FLOWING_CORPSE_WATER.get();
    }

    @Override
    public ItemStack createCorpseWaterBottle() {
        return new ItemStack(Moditems.BYWATER_BOTTLE.get());
    }

    @Override
    public ItemStack createCorpseWaterBucket() {
        return new ItemStack(Moditems.BYWATER_BUCKET.get());
    }

    @Override
    public boolean canBeConvertedToCorpseWater(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getFluidState().is(Fluids.WATER);
    }

    @Override
    public void convertWaterToCorpseWater(ServerLevel level, BlockPos pos) {
        WaterCompanyAPI.getPollutionProvider().markWaterAsPolluted(level, pos);
        if (canBeConvertedToCorpseWater(level, pos)) {
            level.setBlock(pos, BlockRegistry.CORPSE_WATER_BLOCK.get().defaultBlockState(), 3);
        }
    }

    @Override
    public void convertCorpseWaterToCleanWater(ServerLevel level, BlockPos pos) {
        WaterCompanyAPI.getPollutionProvider().markWaterAsClean(level, pos);
        Fluid fluid = level.getBlockState(pos).getFluidState().getType();
        if (isCorpseWater(fluid)) {
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
        }
    }

    @Override
    public int getCorpseWaterEnergy(ServerLevel level, BlockPos pos) {
        return WaterCompanyAPI.getPollutionProvider().getPollutionLevel(level, pos);
    }

    @Override
    public void setCorpseWaterEnergy(ServerLevel level, BlockPos pos, int energy) {
        InfectionData.get(level).setWaterEnergy(pos, energy);
    }

    @Override
    public int getMaxCorpseWaterEnergy() {
        return 15;
    }

    @Override
    public FluidStack extractFluidFromPosition(ServerLevel level, BlockPos pos, int amount) {
        if (isPositionCorpseWaterSource(level, pos)) {
            return new FluidStack(FluidRegistry.CORPSE_WATER.get(), amount);
        }
        return new FluidStack(Fluids.WATER, amount);
    }

    @Override
    public boolean isPositionCorpseWaterSource(ServerLevel level, BlockPos pos) {
        if (WaterCompanyAPI.getPollutionProvider().isWaterPolluted(level, pos)) {
            return true;
        }
        Fluid fluid = level.getBlockState(pos).getFluidState().getType();
        return isCorpseWater(fluid);
    }
}
