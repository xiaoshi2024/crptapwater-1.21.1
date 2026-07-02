package com.xiaoshi2022.crptapwater.network.c2s;

import com.xiaoshi2022.crptapwater.CRPTapWater;
import com.xiaoshi2022.crptapwater.dialogue.CorpseBrotherHelper;
import com.xiaoshi2022.crptapwater.dialogue.CorpseDialogues;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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

public record CCorpseDialogueAnswerPacket(UUID corpseUUID, String questionId, String answerId) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<CCorpseDialogueAnswerPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(CRPTapWater.MODID, "c_corpse_dialogue_answer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CCorpseDialogueAnswerPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CCorpseDialogueAnswerPacket::corpseUUID,
            ByteBufCodecs.STRING_UTF8, CCorpseDialogueAnswerPacket::questionId,
            ByteBufCodecs.STRING_UTF8, CCorpseDialogueAnswerPacket::answerId,
            CCorpseDialogueAnswerPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final CCorpseDialogueAnswerPacket data, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer sp)) return;
            ServerLevel level = sp.serverLevel();
            Entity ent = level.getEntity(data.corpseUUID());
            if (!(ent instanceof Mob mob)) return;
            if (!CorpseBrotherHelper.canDialogue(mob)) return;
            if (mob.distanceTo(sp) > 8.0) return;

            CorpseDialogues.handleAnswer(mob, sp, data.questionId(), data.answerId());
        });
    }
}
