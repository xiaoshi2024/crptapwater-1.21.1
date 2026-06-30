package com.xiaoshi2022.crptapwater.village;

import com.phagens.corpseorigin.api.watercompany.WaterCompanyAPI;
import com.xiaoshi2022.crptapwater.register.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;
import java.util.Optional;

public class VillagerThirstGoal extends Goal {

    private final Villager villager;
    private final ServerLevel level;
    private BlockPos targetTrough;
    private int drinkCooldown = 0;
    private int pathRetryCooldown = 0;
    private static final int SEARCH_RADIUS = 16;
    private static final int SEARCH_HEIGHT = 4;
    private static final int DRINK_AMOUNT = 100;
    private static final double DRINK_DISTANCE_SQR = 2.0;

    public VillagerThirstGoal(Villager villager) {
        this.villager = villager;
        this.level = (ServerLevel) villager.level();
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (drinkCooldown > 0) {
            drinkCooldown--;
            return false;
        }

        if (villager.isBaby()) {
            return false;
        }

        if (level.getRandom().nextInt(1000) != 0) {
            return false;
        }

        targetTrough = findNearbyTrough();
        return targetTrough != null;
    }

    @Override
    public boolean canContinueToUse() {
        if (targetTrough == null) {
            return false;
        }
        if (pathRetryCooldown > 0) {
            pathRetryCooldown--;
        }
        double distSqr = villager.distanceToSqr(Vec3.atCenterOf(targetTrough));
        if (distSqr <= DRINK_DISTANCE_SQR) {
            return true;
        }
        if (villager.getNavigation().isDone() && pathRetryCooldown <= 0) {
            pathRetryCooldown = 20;
            if (!startMovingToTrough()) {
                return false;
            }
        }
        return !villager.getNavigation().isStuck();
    }

    @Override
    public void start() {
        if (targetTrough != null) {
            startMovingToTrough();
        }
    }

    // 在 tick() 方法中修改
    @Override
    public void tick() {
        if (targetTrough == null) return;

        if (villager.distanceToSqr(Vec3.atCenterOf(targetTrough)) <= DRINK_DISTANCE_SQR) {
            Vec3 center = Vec3.atCenterOf(targetTrough);
            // 使用 setLookAt(double, double, double) 方法
            villager.getLookControl().setLookAt(
                    center.x,
                    center.y,
                    center.z,
                    30.0F,
                    30.0F
            );
            drinkFromTrough();
        }
    }

    @Override
    public void stop() {
        targetTrough = null;
        villager.getNavigation().stop();
    }

    private boolean startMovingToTrough() {
        if (targetTrough == null) return false;
        return villager.getNavigation().moveTo(
                targetTrough.getX() + 0.5,
                targetTrough.getY(),
                targetTrough.getZ() + 0.5,
                0.8
        );
    }

    private BlockPos findNearbyTrough() {
        BlockPos villagerPos = villager.blockPosition();
        Optional<BlockPos> result = BlockPos.findClosestMatch(
                villagerPos,
                SEARCH_RADIUS,
                SEARCH_HEIGHT,
                pos -> isWaterTroughWithWater(pos)
        );
        return result.orElse(null);
    }

    private boolean isWaterTroughWithWater(BlockPos pos) {
        if (!level.getBlockState(pos).is(BlockRegistry.WATER_TROUGH_BLOCK.get())) {
            return false;
        }
        var be = level.getBlockEntity(pos);
        if (be instanceof VillageWaterTroughBlockEntity trough) {
            return trough.getFluidStorage().getFluidAmount() >= DRINK_AMOUNT;
        }
        return false;
    }

    private void drinkFromTrough() {
        if (targetTrough == null) return;
        var be = level.getBlockEntity(targetTrough);
        if (be instanceof VillageWaterTroughBlockEntity trough) {
            boolean wasPolluted = trough.isWaterPolluted();
            if (trough.consumeWaterForVillager(DRINK_AMOUNT)) {
                trough.triggerDispenseAnimation(wasPolluted);
                if (wasPolluted) {
                    WaterCompanyAPI.getInfectionTrigger().triggerInfection(villager, level);
                    // CRPTapWater.LOGGER.info(
                    //         "村民 {} 在 {} 喝下了污染的尸水，即将变异！",
                    //         villager.getName().getString(), targetTrough);
                }
                drinkCooldown = 1200;
                targetTrough = null;
            } else {
                drinkCooldown = 100;
                targetTrough = null;
            }
        } else {
            drinkCooldown = 100;
            targetTrough = null;
        }
    }
}
