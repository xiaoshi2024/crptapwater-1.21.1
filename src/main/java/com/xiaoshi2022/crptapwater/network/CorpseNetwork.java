package com.xiaoshi2022.crptapwater.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class CorpseNetwork {

    public static <T extends CustomPacketPayload> void sendToPlayer(T payload, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, payload);
    }

    public static <T extends CustomPacketPayload> void sendToServer(T payload) {
        PacketDistributor.sendToServer(payload);
    }
}
