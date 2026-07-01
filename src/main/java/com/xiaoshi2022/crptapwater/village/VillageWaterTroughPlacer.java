package com.xiaoshi2022.crptapwater.village;

import com.xiaoshi2022.crptapwater.CRPTapWater;
import com.xiaoshi2022.crptapwater.register.BlockRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class VillageWaterTroughPlacer {

    private static final Set<BlockPos> PROCESSED_VILLAGES = ConcurrentHashMap.newKeySet();
    private static final int SEARCH_RADIUS = 32;
    private static final String SAVE_FILE = "water_trough_processed.nbt";
    private static boolean isLoaded = false;
    private static int tickCounter = 0;
    private static final int DELAY_TICKS = 100;

    // ========== 持久化存储 ==========

    private Path getSavePath(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve(SAVE_FILE);
    }

    private void loadProcessedVillages(ServerLevel level) {
        Path savePath = getSavePath(level);
        File saveFile = savePath.toFile();

        // 如果文件不存在，直接返回，不加载任何数据
        if (!saveFile.exists()) {
            CRPTapWater.LOGGER.info("未找到已处理村庄数据文件，将创建新文件");
            return;
        }

        try {
            CompoundTag tag = NbtIo.read(savePath);
            // 检查 tag 是否为 null
            if (tag == null) {
                CRPTapWater.LOGGER.warn("已处理村庄数据文件为空");
                return;
            }

            ListTag list = tag.getList("ProcessedVillages", 10);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag posTag = (CompoundTag) list.get(i);
                Optional<BlockPos> posOpt = NbtUtils.readBlockPos(posTag, "Pos");
                posOpt.ifPresent(PROCESSED_VILLAGES::add);
            }
            CRPTapWater.LOGGER.info("加载了 {} 个已处理的村庄", PROCESSED_VILLAGES.size());
        } catch (IOException e) {
            CRPTapWater.LOGGER.error("加载已处理村庄数据失败: {}", e.getMessage());
        }
    }

    private void saveProcessedVillages(ServerLevel level) {
        Path savePath = getSavePath(level);

        CompoundTag tag = new CompoundTag();
        ListTag list = new ListTag();
        for (BlockPos pos : PROCESSED_VILLAGES) {
            // 修复：writeBlockPos 返回 Tag，需要转换为 CompoundTag
            Tag posTag = NbtUtils.writeBlockPos(pos);
            if (posTag instanceof CompoundTag) {
                list.add((CompoundTag) posTag);
            }
        }
        tag.put("ProcessedVillages", list);

        try {
            NbtIo.write(tag, savePath);
        } catch (IOException e) {
            CRPTapWater.LOGGER.error("保存已处理村庄数据失败: {}", e.getMessage());
        }
    }

    // ========== 事件监听 ==========

    @SubscribeEvent
    public void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (serverLevel.dimension() != Level.OVERWORLD) return;

        if (!isLoaded) {
            loadProcessedVillages(serverLevel);
            isLoaded = true;
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        if (!isLoaded) return;

        tickCounter++;
        if (tickCounter < DELAY_TICKS) return;

        if (tickCounter == DELAY_TICKS) {
            ServerLevel level = event.getServer().overworld();
            if (level != null) {
                try {
                    checkForNewVillages(level);
                } catch (Exception e) {
                    CRPTapWater.LOGGER.error("检查新村庄时发生错误: {}", e.getMessage());
                }
            }
        }
    }

    @SubscribeEvent
    public void onEntityJoinLevel(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (serverLevel.dimension() != Level.OVERWORLD) return;
        if (!(entity instanceof Villager)) return;

        BlockPos center = entity.blockPosition();
        boolean alreadyProcessed = PROCESSED_VILLAGES.stream()
                .anyMatch(pos -> pos.distSqr(center) < 10000);
        if (!alreadyProcessed) {
            try {
                checkForNewVillages(serverLevel);
            } catch (Exception e) {
                CRPTapWater.LOGGER.error("检查新村庄时发生错误: {}", e.getMessage());
            }
        }
    }

    // ========== 核心逻辑 ==========

    private void checkForNewVillages(ServerLevel level) {
        AABB searchBox = new AABB(
                -1000000, -1000000, -1000000,
                1000000, 1000000, 1000000
        );
        var villagers = level.getEntitiesOfClass(Villager.class, searchBox, entity -> true);

        if (villagers.isEmpty()) {
            CRPTapWater.LOGGER.debug("未找到村民");
            return;
        }

        Set<BlockPos> villageCenters = findVillageCenters(villagers);
        int placed = 0;

        for (BlockPos center : villageCenters) {
            boolean alreadyProcessed = PROCESSED_VILLAGES.stream()
                    .anyMatch(pos -> pos.distSqr(center) < 10000);
            if (alreadyProcessed) {
                CRPTapWater.LOGGER.debug("村庄 {} 已处理过，跳过", center);
                continue;
            }

            boolean hasTrough = checkVillageHasTrough(level, center);
            if (hasTrough) {
                PROCESSED_VILLAGES.add(center);
                saveProcessedVillages(level);
                CRPTapWater.LOGGER.debug("村庄 {} 已有水槽，标记为已处理", center);
                continue;
            }

            BlockPos troughPos = findTroughPosition(level, center);
            if (troughPos != null) {
                level.setBlock(troughPos,
                        BlockRegistry.WATER_TROUGH_BLOCK.get().defaultBlockState(),
                        3);
                PROCESSED_VILLAGES.add(center);
                saveProcessedVillages(level);
                placed++;
                CRPTapWater.LOGGER.info("在 {} 为村庄放置了水槽", troughPos);
            }
        }

        if (placed > 0) {
            CRPTapWater.LOGGER.info("成功在新村庄中放置了 {} 个水槽", placed);
        }
    }

    private Set<BlockPos> findVillageCenters(java.util.List<Villager> villagers) {
        Set<BlockPos> centers = new HashSet<>();

        for (Villager villager : villagers) {
            BlockPos pos = villager.blockPosition();
            boolean found = false;
            for (BlockPos center : centers) {
                if (center.distSqr(pos) < 10000) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                centers.add(pos);
            }
        }

        return centers;
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