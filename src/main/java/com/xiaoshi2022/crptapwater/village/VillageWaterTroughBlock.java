package com.xiaoshi2022.crptapwater.village;

import com.mojang.serialization.MapCodec;
import com.phagens.corpseorigin.register.Moditems;
import com.xiaoshi2022.crptapwater.register.BlockEntityRegistry;
import com.xiaoshi2022.crptapwater.register.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class VillageWaterTroughBlock extends BaseEntityBlock {

    // codec() 方法实现
    public static final MapCodec<VillageWaterTroughBlock> CODEC = simpleCodec(VillageWaterTroughBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(3.5, 0.0, 3.5, 12.5, 10.25, 12.5),
            Block.box(3.5, 10.0, 3.5, 12.5, 21.5, 12.5),
            Block.box(4.15, 18.17, 4.15, 11.85, 29.67, 11.85)
    );


    public VillageWaterTroughBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof VillageWaterTroughBlockEntity trough) {
                trough.triggerSetAnimation();
            }
        }
    }

    // useWithoutItem - 右键点击（空手）处理
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VillageWaterTroughBlockEntity trough) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                    "水槽水量: " + trough.getFluidStorage().getFluidAmount() + "mB / " +
                            trough.getFluidStorage().getCapacity() + "mB"
            ));
            if (!trough.getFluidStorage().isEmpty()) {
                String fluidName = trough.getFluidStorage().getFluid().getFluidType()
                        .getDescription().getString();
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "当前液体: " + fluidName
                ));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    // 使用 ItemInteractionResult 作为返回类型
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof VillageWaterTroughBlockEntity trough) {
            ItemStack held = player.getItemInHand(hand);

            if (held.getItem() == ItemRegistry.PIPE_BLOCK_ITEM.get()) {
                trough.triggerPipeAnimation();
                return ItemInteractionResult.SUCCESS;
            }

            if (held.getItem() == Items.GLASS_BOTTLE) {
                if (!trough.getFluidStorage().isEmpty() && trough.getFluidStorage().getFluidAmount() >= 250) {
                    var drained = trough.getFluidStorage().drain(250,
                            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                    if (drained.getAmount() >= 250) {
                        trough.getFluidStorage().drain(250,
                                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                        boolean isCorpse = com.phagens.corpseorigin.api.watercompany.WaterCompanyAPI
                                .getCorpseWaterHandler().isCorpseWater(drained.getFluid());
                        ItemStack bottleItem;
                        if (isCorpse) {
                            bottleItem = new ItemStack(Moditems.BYWATER_BOTTLE.get());
                        } else {
                            bottleItem = PotionContents.createItemStack(Items.POTION, Potions.WATER);
                        }
                        if (held.getCount() == 1) {
                            player.setItemInHand(hand, bottleItem);
                        } else {
                            held.shrink(1);
                            if (!player.getInventory().add(bottleItem)) {
                                player.drop(bottleItem, false);
                            }
                        }
                        trough.triggerDispenseAnimation(isCorpse);
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOTTLE_FILL,
                                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                        return ItemInteractionResult.SUCCESS;
                    }
                }
            }

            if (held.getItem() == ItemRegistry.LONGSHI_MINERAL_WATER_EMPTY.get()) {
                if (!trough.getFluidStorage().isEmpty() && trough.getFluidStorage().getFluidAmount() >= 250) {
                    var drained = trough.getFluidStorage().drain(250,
                            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                    if (drained.getAmount() >= 250) {
                        trough.getFluidStorage().drain(250,
                                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                        boolean isCorpse = com.phagens.corpseorigin.api.watercompany.WaterCompanyAPI
                                .getCorpseWaterHandler().isCorpseWater(drained.getFluid());
                        ItemStack filled = new ItemStack(ItemRegistry.LONGSHI_MINERAL_WATER.get());
                        if (held.getCount() == 1) {
                            player.setItemInHand(hand, filled);
                        } else {
                            held.shrink(1);
                            if (!player.getInventory().add(filled)) {
                                player.drop(filled, false);
                            }
                        }
                        trough.triggerDispenseAnimation(isCorpse);
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BOTTLE_FILL,
                                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                        return ItemInteractionResult.SUCCESS;
                    }
                }
            }

            // 用桶取水
            if (held.getItem() == Items.BUCKET) {
                if (!trough.getFluidStorage().isEmpty() && trough.getFluidStorage().getFluidAmount() >= 1000) {
                    var drained = trough.getFluidStorage().drain(1000,
                            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                    if (drained.getAmount() >= 1000) {
                        trough.getFluidStorage().drain(1000,
                                net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                        ItemStack bucketItem;
                        if (com.phagens.corpseorigin.api.watercompany.WaterCompanyAPI.getCorpseWaterHandler()
                                .isCorpseWater(drained.getFluid())) {
                            bucketItem = new ItemStack(Moditems.BYWATER_BUCKET.get());
                        } else {
                            bucketItem = new ItemStack(Items.WATER_BUCKET);
                        }
                        if (held.getCount() == 1) {
                            player.setItemInHand(hand, bucketItem);
                        } else {
                            held.shrink(1);
                            if (!player.getInventory().add(bucketItem)) {
                                player.drop(bucketItem, false);
                            }
                        }
                        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_FILL,
                                net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                        // 使用 SUCCESS（在服务端等同于 CONSUME）
                        return ItemInteractionResult.SUCCESS;
                    }
                }
            }

            // 倒入水
            if (held.getItem() == Items.WATER_BUCKET) {
                int filled = trough.getFluidStorage().fill(
                        new net.neoforged.neoforge.fluids.FluidStack(
                                net.minecraft.world.level.material.Fluids.WATER, 1000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    if (held.getCount() == 1) {
                        player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                    } else {
                        held.shrink(1);
                        if (!player.getInventory().add(new ItemStack(Items.BUCKET))) {
                            player.drop(new ItemStack(Items.BUCKET), false);
                        }
                    }
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY,
                            net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    return ItemInteractionResult.SUCCESS;
                }
            }

            // 倒入尸水
            if (held.getItem() == Moditems.BYWATER_BUCKET.get()) {
                int filled = trough.getFluidStorage().fill(
                        new net.neoforged.neoforge.fluids.FluidStack(
                                com.xiaoshi2022.crptapwater.fluid.FluidRegistry.CORPSE_WATER.get(), 1000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    if (held.getCount() == 1) {
                        player.setItemInHand(hand, new ItemStack(Items.BUCKET));
                    } else {
                        held.shrink(1);
                        if (!player.getInventory().add(new ItemStack(Items.BUCKET))) {
                            player.drop(new ItemStack(Items.BUCKET), false);
                        }
                    }
                    level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY,
                            net.minecraft.sounds.SoundSource.BLOCKS, 1.0F, 1.0F);
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VillageWaterTroughBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return createTickerHelper(type, BlockEntityRegistry.WATER_TROUGH_BLOCK_ENTITY.get(),
                    VillageWaterTroughBlockEntity::clientTick);
        }
        return createTickerHelper(type, BlockEntityRegistry.WATER_TROUGH_BLOCK_ENTITY.get(),
                VillageWaterTroughBlockEntity::serverTick);
    }
}