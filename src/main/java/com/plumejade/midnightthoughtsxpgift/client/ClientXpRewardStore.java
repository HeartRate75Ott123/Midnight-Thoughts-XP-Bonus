package com.plumejade.midnightthoughtsxpgift.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.plumejade.midnightthoughtsxpgift.network.SyncXpRewardPacket;

/**
 * Client side cache of the reward that was settled for the local player.
 *
 * <p>Midnight Thoughts opens its morning summary screen from a client bound packet, so the reward
 * values are already present by the time the screen renders. The entry is cleared when the summary
 * screen is closed.</p>
 */
public final class ClientXpRewardStore {

    private static final Map<UUID, SyncXpRewardPacket> REWARDS = new ConcurrentHashMap<>();

    private ClientXpRewardStore() {
    }

    /** Called from the payload handler (client only). */
    public static void handle(SyncXpRewardPacket payload) {
        UUID localPlayer = net.minecraft.client.Minecraft.getInstance().player == null
                ? null
                : net.minecraft.client.Minecraft.getInstance().player.getUUID();
        if (localPlayer != null) {
            REWARDS.put(localPlayer, payload);
        }
    }

    /** The reward settled for the given player, or {@code null} when nothing was settled. */
    public static SyncXpRewardPacket get(UUID playerId) {
        return playerId == null ? null : REWARDS.get(playerId);
    }

    /** Forgets the stored reward, e.g. once the summary screen has been dismissed. */
    public static void clear(UUID playerId) {
        if (playerId != null) {
            REWARDS.remove(playerId);
        }
    }

    public static void clearAll() {
        REWARDS.clear();
    }
}
