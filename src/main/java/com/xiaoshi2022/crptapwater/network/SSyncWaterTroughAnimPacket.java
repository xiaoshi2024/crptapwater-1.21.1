package com.xiaoshi2022.crptapwater.network;

import com.xiaoshi2022.crptapwater.CRPTapWater;
import com.xiaoshi2022.crptapwater.village.VillageWaterTroughBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SSyncWaterTroughAnimPacket(BlockPos pos, String animName) implements CustomPacketPayload {

    public static final Type<SSyncWaterTroughAnimPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(CRPTapWater.MODID, "sync_water_trough_anim"));

    public static final StreamCodec<FriendlyByteBuf, SSyncWaterTroughAnimPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,  // 使用 BlockPos 自带的编解码器
            SSyncWaterTroughAnimPacket::pos,
            ByteBufCodecs.STRING_UTF8,
            SSyncWaterTroughAnimPacket::animName,
            SSyncWaterTroughAnimPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static int ticksForAnim(String animName) {
        return switch (animName) {
            case "cold" -> VillageWaterTroughBlockEntity.USE_COLD_TICKS;
            case "hot"  -> VillageWaterTroughBlockEntity.USE_HOT_TICKS;
            case "set"  -> VillageWaterTroughBlockEntity.SET_TICKS;
            case "pipe" -> VillageWaterTroughBlockEntity.PIPE_TICKS;
            default -> 0;
        };
    }

    public static void handle(SSyncWaterTroughAnimPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            Level level = context.player().level();
            if (level == null || !level.isLoaded(packet.pos())) return;
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (be instanceof VillageWaterTroughBlockEntity trough) {
                int ticks = ticksForAnim(packet.animName());
                trough.setAnimFromPacket(packet.animName(), ticks);
            }
        });
    }
}
