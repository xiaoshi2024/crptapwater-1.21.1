package com.xiaoshi2022.crptapwater.network.c2s;

import com.xiaoshi2022.crptapwater.CRPTapWater;
import com.xiaoshi2022.crptapwater.dialogue.CorpseBrotherHelper;
import com.xiaoshi2022.crptapwater.dialogue.DialogFlow;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record CCorpseDialogueInitPacket(UUID corpseUUID) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CCorpseDialogueInitPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(CRPTapWater.MODID, "c_corpse_dialogue_init"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CCorpseDialogueInitPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CCorpseDialogueInitPacket::corpseUUID,
            CCorpseDialogueInitPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final CCorpseDialogueInitPacket data, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            ServerLevel level = sp.serverLevel();
            Entity ent = level.getEntity(data.corpseUUID());
            if (!(ent instanceof Mob mob)) return;
            if (!CorpseBrotherHelper.canDialogue(mob)) return;
            if (mob.distanceTo(sp) > 6.0) return;

            DialogFlow.continueDialogue(mob, sp, "root");
        });
    }
}
