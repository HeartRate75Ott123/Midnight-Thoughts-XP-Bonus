package com.plumejade.midnightthoughtsxpgift.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.netty.buffer.Unpooled;

import net.minecraft.network.FriendlyByteBuf;

/**
 * The packet carries the settlement table of the whole night, so its codec has to survive a round trip
 * with several players and with non ASCII names.
 */
class SyncXpRewardPacketTest {

    @Test
    @DisplayName("a multi player table survives encode/decode")
    void roundTrip() {
        SyncXpRewardPacket original = new SyncXpRewardPacket(List.of(
                new SyncXpRewardPacket.Entry("Plume", 2000, 30, 16, 12800, 10),
                new SyncXpRewardPacket.Entry("Friend", 1700, 0, 16, 10880, 0),
                new SyncXpRewardPacket.Entry("玩家三", -1, 5, 16, 0, 2)));

        SyncXpRewardPacket decoded = roundTrip(original);

        assertEquals(original.entries(), decoded.entries());
        assertEquals(3, decoded.entries().size());
        assertEquals(2046, decoded.entries().get(0).total());
        assertEquals(1716, decoded.entries().get(1).total());
        assertEquals(20, decoded.entries().get(2).total());
    }

    @Test
    @DisplayName("an empty table survives a round trip")
    void emptyTable() {
        assertTrue(roundTrip(new SyncXpRewardPacket(List.of())).entries().isEmpty());
    }

    private static SyncXpRewardPacket roundTrip(SyncXpRewardPacket packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        SyncXpRewardPacket.CODEC.encode(buffer, packet);
        return SyncXpRewardPacket.CODEC.decode(buffer);
    }
}
