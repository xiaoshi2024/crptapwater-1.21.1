package com.xiaoshi2022.crptapwater.pipe;

import com.mojang.serialization.MapCodec;
import com.phagens.corpseorigin.register.Moditems;
import com.xiaoshi2022.crptapwater.fluid.FluidRegistry;
import com.xiaoshi2022.crptapwater.register.BlockEntityRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

public class PipeBlock extends BaseEntityBlock {

    // 添加 codec (1.21 要求)
    public static final MapCodec<PipeBlock> CODEC = simpleCodec(PipeBlock::new);

    public static final BooleanProperty NORTH = BlockStateProperties.NORTH;
    public static final BooleanProperty SOUTH = BlockStateProperties.SOUTH;
    public static final BooleanProperty EAST = BlockStateProperties.EAST;
    public static final BooleanProperty WEST = BlockStateProperties.WEST;
    public static final BooleanProperty UP = BlockStateProperties.UP;
    public static final BooleanProperty DOWN = BlockStateProperties.DOWN;

    private static final VoxelShape CENTER = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape SHAPE_NORTH = Block.box(5.0, 5.0, 0.0, 11.0, 11.0, 5.0);
    private static final VoxelShape SHAPE_SOUTH = Block.box(5.0, 5.0, 11.0, 11.0, 11.0, 16.0);
    private static final VoxelShape SHAPE_EAST = Block.box(11.0, 5.0, 5.0, 16.0, 11.0, 11.0);
    private static final VoxelShape SHAPE_WEST = Block.box(0.0, 5.0, 5.0, 5.0, 11.0, 11.0);
    private static final VoxelShape SHAPE_UP = Block.box(5.0, 11.0, 5.0, 11.0, 16.0, 11.0);
    private static final VoxelShape SHAPE_DOWN = Block.box(5.0, 0.0, 5.0, 11.0, 5.0, 11.0);

    public PipeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, false)
                .setValue(SOUTH, false)
                .setValue(EAST, false)
                .setValue(WEST, false)
                .setValue(UP, false)
                .setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    private boolean canConnectTo(BlockGetter level, BlockPos pos, Direction direction) {
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);
        Block neighborBlock = neighborState.getBlock();
        if (neighborBlock instanceof PipeBlock) {
            return true;
        }
        // 使用 Level 的 getCapability 方法替代 BlockEntity.getCapability
        if (level instanceof Level lvl) {
            var cap = lvl.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    neighborPos,
                    direction.getOpposite());
            if (cap != null) {
                return true;
            }
        }
        return false;
    }

    private BlockState updateConnections(BlockGetter level, BlockPos pos, BlockState state) {
        return state
                .setValue(NORTH, canConnectTo(level, pos, Direction.NORTH))
                .setValue(SOUTH, canConnectTo(level, pos, Direction.SOUTH))
                .setValue(EAST, canConnectTo(level, pos, Direction.EAST))
                .setValue(WEST, canConnectTo(level, pos, Direction.WEST))
                .setValue(UP, canConnectTo(level, pos, Direction.UP))
                .setValue(DOWN, canConnectTo(level, pos, Direction.DOWN));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return updateConnections(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return updateConnections(level, pos, state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape shape = CENTER;
        if (state.getValue(NORTH)) shape = Shapes.or(shape, SHAPE_NORTH);
        if (state.getValue(SOUTH)) shape = Shapes.or(shape, SHAPE_SOUTH);
        if (state.getValue(EAST)) shape = Shapes.or(shape, SHAPE_EAST);
        if (state.getValue(WEST)) shape = Shapes.or(shape, SHAPE_WEST);
        if (state.getValue(UP)) shape = Shapes.or(shape, SHAPE_UP);
        if (state.getValue(DOWN)) shape = Shapes.or(shape, SHAPE_DOWN);
        return shape;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, net.minecraft.world.phys.BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.CONSUME;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PipeBlockEntity pipe)) return ItemInteractionResult.FAIL;
        FluidTank tank = pipe.getFluidStorage();

        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() == Items.BUCKET && !tank.isEmpty()) {
            FluidStack drained = tank.drain(1000, IFluidHandler.FluidAction.SIMULATE);
            if (!drained.isEmpty() && drained.getAmount() >= 1000) {
                FluidStack actual = tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
                boolean isCorpse = actual.getFluid().isSame(FluidRegistry.CORPSE_WATER.get());
                ItemStack filledBucket = isCorpse
                        ? Moditems.BYWATER_BUCKET.get().getDefaultInstance()
                        : new ItemStack(Items.WATER_BUCKET);
                if (held.getCount() == 1) {
                    player.setItemInHand(hand, filledBucket);
                } else {
                    held.shrink(1);
                    if (!player.getInventory().add(filledBucket)) {
                        player.drop(filledBucket, false);
                    }
                }
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
                return ItemInteractionResult.SUCCESS;
            }
        }

        if (held.getItem() == Moditems.BYWATER_BUCKET.get() || held.getItem() == Items.WATER_BUCKET) {
            FluidStack toAdd = held.getItem() == Moditems.BYWATER_BUCKET.get()
                    ? new FluidStack(FluidRegistry.CORPSE_WATER.get(), 1000)
                    : new FluidStack(Fluids.WATER, 1000);
            int filled = tank.fill(toAdd, IFluidHandler.FluidAction.SIMULATE);
            if (filled >= 1000) {
                tank.fill(toAdd, IFluidHandler.FluidAction.EXECUTE);
                ItemStack emptyBucket = new ItemStack(Items.BUCKET);
                if (held.getCount() == 1) {
                    player.setItemInHand(hand, emptyBucket);
                } else {
                    held.shrink(1);
                    if (!player.getInventory().add(emptyBucket)) {
                        player.drop(emptyBucket, false);
                    }
                }
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0f, 1.0f);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, BlockEntityRegistry.PIPE_BLOCK_ENTITY.get(),
                PipeBlockEntity::serverTick);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PipeBlockEntity pipe) {
                pipe.updateConnections();
            }
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   BlockPos neighborPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, isMoving);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof PipeBlockEntity pipe) {
                pipe.updateConnections();
            }
        }
    }
}