package com.plumejade.midnightthoughtsxpgift.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Small helper around NeoForge's {@link PacketDistributor} for the addon's payload.
 *
 * <p>Client bound handling lives in {@code ClientPayloadHandler} so that no client only class is
 * ever resolved on a dedicated server.</p>
 */
public final class XpGiftNetwork {

    private XpGiftNetwork() {
    }

    /**
     * Sends the settled reward of one player to that player's client.
     */
    public static void sendReward(ServerPlayer player, SyncXpRewardPacket payload) {
        PacketDistributor.sendToPlayer(player, payload);
    }
}
