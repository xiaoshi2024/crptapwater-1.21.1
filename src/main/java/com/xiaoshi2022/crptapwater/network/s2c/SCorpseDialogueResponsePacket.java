package com.xiaoshi2022.crptapwater.network.s2c;

import com.xiaoshi2022.crptapwater.CRPTapWater;
import com.xiaoshi2022.crptapwater.client.gui.CorpseDialogScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public record SCorpseDialogueResponsePacket(int entityId, String question, List<String> answers) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SCorpseDialogueResponsePacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(CRPTapWater.MODID, "s_corpse_dialogue_response"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SCorpseDialogueResponsePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SCorpseDialogueResponsePacket::entityId,
            ByteBufCodecs.STRING_UTF8, SCorpseDialogueResponsePacket::question,
            ByteBufCodecs.collection(ArrayList::new, ByteBufCodecs.STRING_UTF8), SCorpseDialogueResponsePacket::answers,
            SCorpseDialogueResponsePacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SCorpseDialogueResponsePacket data, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            Screen screen = mc.screen;
            if (screen instanceof CorpseDialogScreen dialogScreen && dialogScreen.matchesEntity(data.entityId())) {
                dialogScreen.setDialogue(data.question(), data.answers());
            }
        });
    }
}
