package com.xiaoshi2022.crptapwater.pipe;

import com.phagens.corpseorigin.api.watercompany.WaterCompanyAPI;
import com.xiaoshi2022.crptapwater.CRPTapWater;
import com.xiaoshi2022.crptapwater.register.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.EnumMap;
import java.util.Map;

public class PipeBlockEntity extends BlockEntity {

    private final FluidTank fluidStorage = new FluidTank(2000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                BlockState state = level.getBlockState(worldPosition);
                level.sendBlockUpdated(worldPosition, state, state, 3);
            }
        }
    };

    private int transferRate = 100;
    private final Map<Direction, Boolean> connections = new EnumMap<>(Direction.class);

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.PIPE_BLOCK_ENTITY.get(), pos, state);
        for (Direction dir : Direction.values()) {
            connections.put(dir, false);
        }
    }

    public FluidTank getFluidStorage() {
        return fluidStorage;
    }

    public void updateConnections() {
        if (level == null) return;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);
            boolean connected = false;
            if (neighborState.getBlock() instanceof PipeBlock) {
                connected = true;
            } else {
                BlockEntity neighborBe = level.getBlockEntity(neighborPos);
                if (neighborBe != null) {
                    var cap = level.getCapability(
                            Capabilities.FluidHandler.BLOCK,
                            neighborPos, dir.getOpposite());
                    connected = cap != null;
                }
            }
            connections.put(dir, connected);
        }
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PipeBlockEntity self) {
        self.tick();
    }

    public void tick() {
        if (level == null || level.isClientSide) return;

        if (!fluidStorage.isEmpty()) {
            pushFluidToNeighbors();
        } else {
            tryExtractFromSource();
        }
    }

    private void pushFluidToNeighbors() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        for (Direction dir : Direction.values()) {
            if (!connections.getOrDefault(dir, false)) continue;

            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            Direction opposite = dir.getOpposite();

            var neighborCap = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, opposite);
            if (neighborCap == null) continue;

            if (neighborBe instanceof PipeBlockEntity) {
                if (fluidStorage.getFluidAmount() <= ((PipeBlockEntity) neighborBe).fluidStorage.getFluidAmount()) {
                    continue;
                }
            }

            FluidStack toTransfer = fluidStorage.drain(transferRate, IFluidHandler.FluidAction.SIMULATE);
            if (!toTransfer.isEmpty()) {
                int filled = neighborCap.fill(toTransfer, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    fluidStorage.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                }
            }

            if (fluidStorage.isEmpty()) break;
        }
    }

    private void tryExtractFromSource() {
        if (!(level instanceof ServerLevel serverLevel)) return;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockState neighborState = level.getBlockState(neighborPos);

            if (!neighborState.getFluidState().isEmpty()) {
                FluidStack extracted = WaterCompanyAPI.getCorpseWaterHandler()
                        .extractFluidFromPosition(serverLevel, neighborPos, transferRate);
                if (!extracted.isEmpty()) {
                    fluidStorage.fill(extracted, IFluidHandler.FluidAction.EXECUTE);
                }
            }

            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe != null && !(neighborBe instanceof PipeBlockEntity)) {
                Direction opposite = dir.getOpposite();
                var neighborCap = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, opposite);
                if (neighborCap != null) {
                    FluidStack drained = neighborCap.drain(transferRate, IFluidHandler.FluidAction.SIMULATE);
                    if (!drained.isEmpty()) {
                        int filled = fluidStorage.fill(drained, IFluidHandler.FluidAction.SIMULATE);
                        if (filled > 0) {
                            FluidStack actual = neighborCap.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                            fluidStorage.fill(actual, IFluidHandler.FluidAction.EXECUTE);
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Fluid", fluidStorage.writeToNBT(provider, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        fluidStorage.readFromNBT(provider, tag.getCompound("Fluid"));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        return saveWithoutMetadata(provider);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        super.onDataPacket(net, pkt, provider);
        CompoundTag tag = pkt.getTag();
        if (tag != null && tag.contains("Fluid")) {
            fluidStorage.readFromNBT(provider, tag.getCompound("Fluid"));
        }
    }
}
