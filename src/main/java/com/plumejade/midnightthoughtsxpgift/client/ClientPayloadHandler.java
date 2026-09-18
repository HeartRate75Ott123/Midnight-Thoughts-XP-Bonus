package com.plumejade.midnightthoughtsxpgift.client;

import com.plumejade.midnightthoughtsxpgift.network.SyncXpRewardPacket;

/**
 * Client only payload handling. This class is only ever touched from a payload handler that only
 * runs on the physical client, so no client only class of this mod is resolved on a dedicated
 * server.
 */
public final class ClientPayloadHandler {

    private ClientPayloadHandler() {
    }

    public static void handleXpReward(SyncXpRewardPacket payload) {
        ClientXpRewardStore.handle(payload);
    }
}
