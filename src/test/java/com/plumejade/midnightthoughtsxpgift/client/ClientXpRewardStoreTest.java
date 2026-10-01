package com.plumejade.midnightthoughtsxpgift.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.plumejade.midnightthoughtsxpgift.network.SyncXpRewardPacket;

/**
 * Regression tests for the multi player bug: the morning summary panel shows one row per player, so the
 * reward of a row must be looked up by that row's player name. Keying by the local player made every row
 * display the local player's numbers.
 *
 * <p>{@link ClientXpRewardStore} deliberately has no dependency on any client class, so it can be tested
 * without starting the game.</p>
 */
class ClientXpRewardStoreTest {

    @AfterEach
    void tearDown() {
        ClientXpRewardStore.clear();
    }

    @Test
    @DisplayName("every player sees their own value, not the local player's value")
    void eachNameHasItsOwnValue() {
        ClientXpRewardStore.handle(new SyncXpRewardPacket(List.of(
                new SyncXpRewardPacket.Entry("Plume", 2000, 0, 16, 12800, 0),
                new SyncXpRewardPacket.Entry("Friend", 1700, 0, 16, 10880, 0))));

        SyncXpRewardPacket.Entry plume = ClientXpRewardStore.get("Plume");
        SyncXpRewardPacket.Entry friend = ClientXpRewardStore.get("Friend");

        assertNotNull(plume);
        assertNotNull(friend);
        assertEquals(2016, plume.total());
        assertEquals(1716, friend.total());
    }

    @Test
    @DisplayName("a player that did not sleep has no entry")
    void unknownPlayer() {
        ClientXpRewardStore.handle(new SyncXpRewardPacket(List.of(
                new SyncXpRewardPacket.Entry("Plume", 640, 30, 16, 4096, 10))));

        assertNull(ClientXpRewardStore.get("SomeoneElse"));
        assertNull(ClientXpRewardStore.get(null));
    }

    @Test
    @DisplayName("a new night replaces the previous table instead of merging into it")
    void replacesPreviousTable() {
        ClientXpRewardStore.handle(new SyncXpRewardPacket(List.of(
                new SyncXpRewardPacket.Entry("Plume", 640, 0, 16, 4096, 0),
                new SyncXpRewardPacket.Entry("Friend", 320, 0, 16, 2048, 0))));

        // The next night only Plume slept.
        ClientXpRewardStore.handle(new SyncXpRewardPacket(List.of(
                new SyncXpRewardPacket.Entry("Plume", 1280, 0, 16, 8192, 0))));

        assertEquals(1296, ClientXpRewardStore.get("Plume").total());
        assertNull(ClientXpRewardStore.get("Friend"), "stale entries must be dropped");
    }

    @Test
    @DisplayName("closing the summary panel drops the table")
    void clear() {
        ClientXpRewardStore.handle(new SyncXpRewardPacket(List.of(
                new SyncXpRewardPacket.Entry("Plume", 640, 0, 16, 4096, 0))));

        ClientXpRewardStore.clear();

        assertNull(ClientXpRewardStore.get("Plume"));
    }
}
