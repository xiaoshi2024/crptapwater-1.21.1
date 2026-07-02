package com.xiaoshi2022.crptapwater.network.s2c;

import com.xiaoshi2022.crptapwater.CRPTapWater;
import com.xiaoshi2022.crptapwater.client.gui.CorpseDialogScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record SCorpseDialogueQuestionPacket(int entityId, Component question, boolean showToast) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SCorpseDialogueQuestionPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(CRPTapWater.MODID, "s_corpse_dialogue_question"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SCorpseDialogueQuestionPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SCorpseDialogueQuestionPacket::entityId,
            ComponentSerialization.STREAM_CODEC, SCorpseDialogueQuestionPacket::question,
            ByteBufCodecs.BOOL, SCorpseDialogueQuestionPacket::showToast,
            SCorpseDialogueQuestionPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SCorpseDialogueQuestionPacket data, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            Screen screen = mc.screen;
            if (screen instanceof CorpseDialogScreen dialogScreen && dialogScreen.matchesEntity(data.entityId())) {
                dialogScreen.setLastPhrase(data.question());
            }
        });
    }
}
