package com.plumejade.midnightthoughtsxpgift.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.plumejade.midnightthoughtsxpgift.network.SyncXpRewardPacket;

/**
 * Client side cache of the rewards that were settled for the night.
 *
 * <p>The entries are keyed by <strong>player name</strong>, not by the local player: the Morning Thoughts
 * summary panel shows one row per player, and every row has to display the value that belongs to the
 * player of that row. Keying by the local player would make every row show the local player's numbers.</p>
 *
 * <p>Midnight Thoughts opens its morning summary screen from a client bound packet, so the values are
 * already present by the time the screen renders. The cache is dropped when the screen is closed.</p>
 */
public final class ClientXpRewardStore {

    private static final Map<String, SyncXpRewardPacket.Entry> REWARDS = new ConcurrentHashMap<>();

    private ClientXpRewardStore() {
    }

    /** Called from the payload handler (client only). Replaces the whole table of the previous night. */
    public static void handle(SyncXpRewardPacket payload) {
        Map<String, SyncXpRewardPacket.Entry> received = new ConcurrentHashMap<>(payload.entries().size() + 1);

        for (SyncXpRewardPacket.Entry entry : payload.entries()) {
            received.put(entry.playerName(), entry);
        }

        REWARDS.clear();
        REWARDS.putAll(received);
    }

    /**
     * The reward settled for the named player, or {@code null} when nothing was settled for them this
     * night (for example because they did not sleep).
     */
    public static SyncXpRewardPacket.Entry get(String playerName) {
        return playerName == null ? null : REWARDS.get(playerName);
    }

    /** Forgets all settled rewards, e.g. once the summary screen has been dismissed. */
    public static void clear() {
        REWARDS.clear();
    }
}
