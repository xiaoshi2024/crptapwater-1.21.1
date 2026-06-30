package com.xiaoshi2022.crptapwater.village;

import com.phagens.corpseorigin.api.watercompany.WaterCompanyAPI;
import com.xiaoshi2022.crptapwater.network.SSyncWaterTroughAnimPacket;
import com.xiaoshi2022.crptapwater.register.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.*;
import software.bernie.geckolib.util.GeckoLibUtil;

public class VillageWaterTroughBlockEntity extends BlockEntity implements GeoBlockEntity {

    protected static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation USE_COLD = RawAnimation.begin().thenPlay("use_cold").thenLoop("idle");
    protected static final RawAnimation USE_HOT = RawAnimation.begin().thenPlay("use_hot").thenLoop("idle");
    protected static final RawAnimation SET_ANIM = RawAnimation.begin().thenPlay("set").thenLoop("idle");
    protected static final RawAnimation PIPE_ANIM = RawAnimation.begin().thenPlay("pipe").thenLoop("idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private final FluidTank fluidStorage = new FluidTank(4000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private long lastPollutionCheckTick = 0;
    private int lastFluidAmount = 0;
    private String queuedActionAnim = null;
    private int animTickLeft = 0;
    private RawAnimation lastPlayedAnim = IDLE;
    public static final int USE_COLD_TICKS = 38;
    public static final int USE_HOT_TICKS = 48;
    public static final int SET_TICKS = 52;
    public static final int PIPE_TICKS = 58;

    public VillageWaterTroughBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.WATER_TROUGH_BLOCK_ENTITY.get(), pos, state);
    }

    public FluidTank getFluidStorage() {
        return fluidStorage;
    }

    public float getWaterFillRatio() {
        int amount = fluidStorage.getFluidAmount();
        return Math.max(0, Math.min(1, (float) amount / fluidStorage.getCapacity()));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, VillageWaterTroughBlockEntity self) {
        self.tick(level, pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, VillageWaterTroughBlockEntity self) {
        self.tick(level, pos, state);
    }

    public void tick(Level level, BlockPos pos, BlockState state) {
        if (animTickLeft > 0) {
            animTickLeft--;
            if (animTickLeft <= 0 && queuedActionAnim != null) {
                queuedActionAnim = null;
                if (!level.isClientSide) setChanged();
            }
        }

        if (level.isClientSide) return;

        if (fluidStorage.isEmpty()) {
            refillFromRainOrPipe(level, pos);
        }

        long currentTick = level.getGameTime();
        if (currentTick - lastPollutionCheckTick > 200) {
            lastPollutionCheckTick = currentTick;
            checkWaterQuality(level, pos);
        }

        lastFluidAmount = fluidStorage.getFluidAmount();
    }

    private void refillFromRainOrPipe(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        boolean refilled = false;
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = pos.relative(dir);
            var cap = level.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, dir.getOpposite());
            if (cap != null) {
                FluidStack drained = cap.drain(100, IFluidHandler.FluidAction.EXECUTE);
                if (!drained.isEmpty()) {
                    fluidStorage.fill(drained, IFluidHandler.FluidAction.EXECUTE);
                    refilled = true;
                    break;
                }
            }
        }

        if (!refilled && level.isRainingAt(pos.above())) {
            fluidStorage.fill(new FluidStack(Fluids.WATER, 5), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void checkWaterQuality(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (!fluidStorage.isEmpty()) {
            FluidStack current = fluidStorage.getFluid();
            if (!WaterCompanyAPI.getCorpseWaterHandler().isCorpseWater(current.getFluid())) {
                if (WaterCompanyAPI.getPollutionProvider().isWaterPolluted(serverLevel, pos)) {
                    int amount = current.getAmount();
                    fluidStorage.drain(amount, IFluidHandler.FluidAction.EXECUTE);
                    fluidStorage.fill(new FluidStack(
                            WaterCompanyAPI.getCorpseWaterHandler().getCorpseWaterFluid(), amount),
                            IFluidHandler.FluidAction.EXECUTE);
                    com.xiaoshi2022.crptapwater.CRPTapWater.LOGGER.info(
                            "水槽在 {} 被污染！水质检测发现尸水污染，已自动转化为尸水", pos);
                }
            }
        }
    }

    public boolean consumeWaterForVillager(int amount) {
        if (fluidStorage.getFluidAmount() >= amount) {
            FluidStack drained = fluidStorage.drain(amount, IFluidHandler.FluidAction.EXECUTE);
            return !drained.isEmpty();
        }
        return false;
    }

    public boolean isWaterPolluted() {
        if (fluidStorage.isEmpty()) return false;
        return WaterCompanyAPI.getCorpseWaterHandler()
                .isCorpseWater(fluidStorage.getFluid().getFluid());
    }

    public void triggerDispenseAnimation(boolean isCorpse) {
        triggerAnim(isCorpse ? "cold" : "hot", isCorpse ? USE_COLD_TICKS : USE_HOT_TICKS);
    }

    public void triggerSetAnimation() {
        triggerAnim("set", SET_TICKS);
    }

    public void triggerPipeAnimation() {
        triggerAnim("pipe", PIPE_TICKS);
    }

    private void triggerAnim(String animName, int ticks) {
        this.queuedActionAnim = animName;
        this.animTickLeft = ticks;
        this.lastPlayedAnim = IDLE; // 强制下次 waterAnimController 重新 set
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = level.getBlockState(worldPosition);
            level.sendBlockUpdated(worldPosition, state, state, 3);
            PacketDistributor.sendToPlayersTrackingChunk(
                    (ServerLevel) level,
                    level.getChunkAt(worldPosition).getPos(),
                    new SSyncWaterTroughAnimPacket(worldPosition, animName)
            );
        }
    }

    public void setAnimFromPacket(String animName, int ticks) {
        this.queuedActionAnim = animName;
        this.animTickLeft = ticks;
        this.lastPlayedAnim = IDLE; // 强制重新 set
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "dispenserController", 1, this::waterAnimController));
    }

    protected <E extends VillageWaterTroughBlockEntity> PlayState waterAnimController(final AnimationState<E> state) {
        RawAnimation target = queuedActionAnim != null
                ? switch (queuedActionAnim) {
                    case "cold" -> USE_COLD;
                    case "hot"  -> USE_HOT;
                    case "set"  -> SET_ANIM;
                    case "pipe" -> PIPE_ANIM;
                    default -> IDLE;
                }
                : IDLE;
        if (target != lastPlayedAnim) {
            lastPlayedAnim = target;
            return state.setAndContinue(target);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider provider) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, provider);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("Fluid", fluidStorage.writeToNBT(provider, new CompoundTag()));
        tag.putLong("LastPollutionCheck", lastPollutionCheckTick);
        tag.putInt("AnimTickLeft", animTickLeft);
        if (queuedActionAnim != null) {
            tag.putString("QueuedAnim", queuedActionAnim);
        }
    }



    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        fluidStorage.readFromNBT(provider, tag.getCompound("Fluid"));
        lastPollutionCheckTick = tag.getLong("LastPollutionCheck");
        lastFluidAmount = fluidStorage.getFluidAmount();
        animTickLeft = tag.contains("AnimTickLeft") ? tag.getInt("AnimTickLeft") : 0;
        queuedActionAnim = tag.contains("QueuedAnim") ? tag.getString("QueuedAnim") : null;
    }
}
