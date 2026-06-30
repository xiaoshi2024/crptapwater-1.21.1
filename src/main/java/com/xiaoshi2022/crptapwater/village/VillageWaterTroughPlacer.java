package com.xiaoshi2022.crptapwater.village;

import com.xiaoshi2022.crptapwater.CRPTapWater;
import com.xiaoshi2022.crptapwater.register.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VillageWaterTroughPlacer {

    private static final Set<BlockPos> PROCESSED_VILLAGES = ConcurrentHashMap.newKeySet();
    private static final int SEARCH_RADIUS = 32;

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (serverLevel.dimension() != Level.OVERWORLD) return;

        // 只处理村民
        if (!(entity instanceof Villager villager)) return;

        // 检查这个村民所在的村庄是否已经有水槽
        BlockPos center = villager.blockPosition();

        // 检查是否已经处理过
        boolean alreadyProcessed = PROCESSED_VILLAGES.stream()
                .anyMatch(pos -> pos.distSqr(center) < 10000);
        if (alreadyProcessed) return;

        // 检查村庄是否已经有水槽
        boolean hasTrough = checkVillageHasTrough(serverLevel, center);
        if (hasTrough) {
            PROCESSED_VILLAGES.add(center);
            return;
        }

        // 找到合适的位置放置水槽
        BlockPos troughPos = findTroughPosition(serverLevel, center);
        if (troughPos != null) {
            serverLevel.setBlock(troughPos,
                    BlockRegistry.WATER_TROUGH_BLOCK.get().defaultBlockState(),
                    3);
            PROCESSED_VILLAGES.add(center);
            CRPTapWater.LOGGER.info("在 {} 为村庄放置了水槽", troughPos);
        }
    }

    private boolean checkVillageHasTrough(ServerLevel level, BlockPos center) {
        for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (int y = -3; y <= 3; y++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.abs(x) < radius && Math.abs(z) < radius) continue;
                        BlockPos check = center.offset(x, y, z);
                        if (level.getBlockState(check).getBlock() == BlockRegistry.WATER_TROUGH_BLOCK.get()) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private BlockPos findTroughPosition(ServerLevel level, BlockPos center) {
        for (int radius = 0; radius <= SEARCH_RADIUS; radius++) {
            for (int y = -3; y <= 3; y++) {
                for (int x = -radius; x <= radius; x++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.abs(x) < radius && Math.abs(z) < radius) continue;

                        BlockPos check = center.offset(x, y, z);
                        if (isValidTroughPosition(level, check)) {
                            return check;
                        }
                    }
                }
            }
        }
        return null;
    }

    private boolean isValidTroughPosition(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).getBlock() == BlockRegistry.WATER_TROUGH_BLOCK.get()) {
            return false;
        }

        BlockPos below = pos.below();
        BlockState belowState = level.getBlockState(below);
        if (!belowState.isFaceSturdy(level, below, net.minecraft.core.Direction.UP)) {
            return false;
        }

        if (belowState.getBlock() == Blocks.WATER) return false;
        if (belowState.getBlock() == Blocks.LAVA) return false;
        if (belowState.getBlock() == Blocks.FARMLAND) return false;

        if (!level.getBlockState(pos).isAir()) return false;
        if (!level.getBlockState(pos.above()).isAir()) return false;
        if (!level.getBlockState(pos.above(2)).isAir()) return false;

        return true;
    }
}