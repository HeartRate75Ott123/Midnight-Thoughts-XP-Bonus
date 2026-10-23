package com.plumejade.midnightthoughtsxpgift.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.plumejade.midnightthoughtsxpgift.data.XpRewardConfig;
import com.plumejade.midnightthoughtsxpgift.network.SyncXpRewardPacket;

/**
 * Regression tests for the "quit and rejoin, then sleep" bug.
 *
 * <p>The rewards are now computed from Midnight Thoughts' own deltas, which are measured against a
 * baseline persisted to the world storage. The previous implementation accumulated distance in a static
 * in-memory map and threw that entry away on logout, so everything gathered before the disconnect was
 * lost and only the fixed base bonus was paid.</p>
 *
 * <p>These tests pin down that the conversion from the vanilla centimetre unit to blocks is exact and
 * that a night's activity really ends up in the payout.</p>
 */
class XpRewardManagerSettlementTest {

    private static final XpRewardConfig CONFIG = XpRewardConfig.DEFAULT;

    @Test
    @DisplayName("vanilla centimetres are converted to whole blocks")
    void centimetersToBlocks() {
        assertEquals(0, NightSettlement.blocksOf(0));
        assertEquals(0, NightSettlement.blocksOf(99), "a partial block is not a block");
        assertEquals(1, NightSettlement.blocksOf(100));
        assertEquals(63, NightSettlement.blocksOf(6399));
        assertEquals(64, NightSettlement.blocksOf(6400));
        assertEquals(0, NightSettlement.blocksOf(-500), "negative distances are clamped to zero");
    }

    @Test
    @DisplayName("activity gathered before a reconnect is still paid out (the reported bug)")
    void activitySurvivesReconnect() {
        // The player travelled 128 blocks (the panel showed it), no kills. The old implementation paid
        // only the base bonus because the distance had been forgotten on logout.
        SyncXpRewardPacket.Entry entry = XpRewardManager.settlementOf("Plume", 12_800, 0, CONFIG);

        assertEquals(128, entry.distanceBlocks());
        assertEquals(20, entry.travelExp(), "128 blocks must pay 2 * 10 exp");
        assertEquals(0, entry.huntExp());
        assertEquals(16, entry.baseBonus());
        assertEquals(36, entry.total(), "the payout must not degrade to the base bonus only");
    }

    @Test
    @DisplayName("the hunt reward is paid from the mob kill delta")
    void huntReward() {
        SyncXpRewardPacket.Entry entry = XpRewardManager.settlementOf("Plume", 0, 25, CONFIG);

        assertEquals(25, entry.mobKills());
        assertEquals(60, entry.huntExp(), "25 kills are 2 whole groups of 10");
        assertEquals(76, entry.total());
    }

    @Test
    @DisplayName("distance and kills add up on top of the base bonus")
    void combined() {
        // 12800 cm = 128 blocks -> 20 exp, 30 kills -> 3 groups -> 90 exp, plus the 16 base bonus.
        SyncXpRewardPacket.Entry entry = XpRewardManager.settlementOf("Plume", 12_800, 30, CONFIG);

        assertEquals(20, entry.travelExp());
        assertEquals(90, entry.huntExp());
        assertEquals(126, entry.total());
    }

    @Test
    @DisplayName("a night without any activity still pays the base bonus")
    void quietNight() {
        SyncXpRewardPacket.Entry entry = XpRewardManager.settlementOf("Plume", 0, 0, CONFIG);

        assertEquals(16, entry.total());
        assertEquals(0, entry.distanceBlocks());
    }

    @Test
    @DisplayName("negative input is clamped instead of producing a negative payout")
    void negativeInput() {
        SyncXpRewardPacket.Entry entry = XpRewardManager.settlementOf("Plume", -1_000, -5, CONFIG);

        assertEquals(0, entry.distanceBlocks());
        assertEquals(0, entry.mobKills());
        assertEquals(16, entry.total());
    }
}
