package com.plumejade.midnightthoughtsxpgift.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Locks in the reward rules that are documented in the README and shipped in the default data pack file.
 */
class XpRewardConfigTest {

    private static final XpRewardConfig CONFIG = XpRewardConfig.DEFAULT;

    @Test
    @DisplayName("the shipped defaults are the documented ones")
    void defaults() {
        assertEquals(64, CONFIG.blocksPerDistanceExp());
        assertEquals(10, CONFIG.distanceExp());
        assertEquals(10, CONFIG.killsPerHuntExp());
        assertEquals(30, CONFIG.huntExp());
        assertEquals(16, CONFIG.baseBonus());
    }

    @Test
    @DisplayName("a travel reward is paid per whole group of 64 blocks")
    void travelReward() {
        assertEquals(0, CONFIG.travelExp(0));
        assertEquals(0, CONFIG.travelExp(63));
        assertEquals(10, CONFIG.travelExp(64));
        assertEquals(10, CONFIG.travelExp(127), "a partial group of blocks must not pay out");
        assertEquals(20, CONFIG.travelExp(128));
        assertEquals(310, CONFIG.travelExp(2000));
    }

    @Test
    @DisplayName("a hunt reward is paid per whole group of 10 kills")
    void huntReward() {
        assertEquals(0, CONFIG.huntExp(0));
        assertEquals(0, CONFIG.huntExp(9));
        assertEquals(30, CONFIG.huntExp(10));
        assertEquals(30, CONFIG.huntExp(19));
        assertEquals(60, CONFIG.huntExp(20));
    }

    @Test
    @DisplayName("negative input never pays out")
    void negativeInput() {
        assertEquals(0, CONFIG.travelExp(-64));
        assertEquals(0, CONFIG.huntExp(-10));
    }

    @Test
    @DisplayName("an absurd distance stays exact instead of overflowing")
    void largeDistanceStaysExact() {
        // DailyXpStats clamps the travelled distance to Integer.MAX_VALUE, which is 33_554_431 whole
        // groups of 64 blocks, so the reward can never overflow with the shipped multipliers.
        assertEquals(335_544_310, CONFIG.travelExp(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("a heavily configured reward saturates instead of overflowing")
    void saturates() {
        // A data pack is free to pick much larger multipliers than the defaults, so the multiplication
        // itself has to clamp. 33_554_431 groups * 1000 exp would be ~3.36e10 and must not wrap around.
        XpRewardConfig generous = new XpRewardConfig(64, 1000, 1, 1000, 0);

        assertEquals(Integer.MAX_VALUE, generous.travelExp(Integer.MAX_VALUE));
        assertEquals(Integer.MAX_VALUE, generous.huntExp(Integer.MAX_VALUE));
    }

    @Test
    @DisplayName("the two separate examples from the bug report")
    void reportedScenario() {
        // 2000 exp vs 1700 exp, i.e. two players that travelled a different distance.
        int first = CONFIG.travelExp(64 * 200) + CONFIG.huntExp(0) + CONFIG.baseBonus();
        int second = CONFIG.travelExp(64 * 170) + CONFIG.huntExp(0) + CONFIG.baseBonus();

        assertEquals(2016, first);
        assertEquals(1716, second);
    }
}
