package com.xiaoshi2022.crptapwater.network.s2c;

import com.xiaoshi2022.crptapwater.CRPTapWater;
import com.xiaoshi2022.crptapwater.client.gui.CorpseDialogScreen;
import com.xiaoshi2022.crptapwater.network.CorpseNetwork;
import com.xiaoshi2022.crptapwater.network.c2s.CCorpseDialogueInitPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record SOpenCorpseDialogPacket(int entityId, UUID entityUUID) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SOpenCorpseDialogPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(CRPTapWater.MODID, "s_open_corpse_dialog"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SOpenCorpseDialogPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, SOpenCorpseDialogPacket::entityId,
            UUIDUtil.STREAM_CODEC, SOpenCorpseDialogPacket::entityUUID,
            SOpenCorpseDialogPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final SOpenCorpseDialogPacket data, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;
            Entity ent = mc.level.getEntity(data.entityId());
            if (!(ent instanceof Mob mob)) return;
            if (!mob.getUUID().equals(data.entityUUID())) return;

            CorpseDialogScreen screen = new CorpseDialogScreen(mob);
            mc.setScreen(screen);

            CorpseNetwork.sendToServer(new CCorpseDialogueInitPacket(data.entityUUID()));
        });
    }
}
