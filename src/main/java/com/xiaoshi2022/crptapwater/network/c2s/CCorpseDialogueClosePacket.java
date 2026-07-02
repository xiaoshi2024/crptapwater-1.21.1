package com.xiaoshi2022.crptapwater.network.c2s;

import com.xiaoshi2022.crptapwater.CRPTapWater;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record CCorpseDialogueClosePacket(UUID corpseUUID) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CCorpseDialogueClosePacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(CRPTapWater.MODID, "c_corpse_dialogue_close"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CCorpseDialogueClosePacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CCorpseDialogueClosePacket::corpseUUID,
            CCorpseDialogueClosePacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final CCorpseDialogueClosePacket data, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
        });
    }
}
