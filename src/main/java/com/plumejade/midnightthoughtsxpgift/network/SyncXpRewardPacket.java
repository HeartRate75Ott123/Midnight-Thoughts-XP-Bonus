package com.plumejade.midnightthoughtsxpgift.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.plumejade.midnightthoughtsxpgift.MidnightThoughtsXpGift;
import com.plumejade.midnightthoughtsxpgift.server.DailyXpStats;

/**
 * Tells a client how much experience <em>every</em> player that slept through the night was awarded.
 *
 * <p>The Morning Thoughts summary panel lists one row per player, so the reward shown next to a row has
 * to belong to the player of that row — not to the client that is looking at the screen. The packet
 * therefore carries the full settlement table of the night and the client looks the values up by player
 * name.</p>
 *
 * <p>The packet is deliberately sent before Midnight Thoughts' own {@code DailySummaryPacket} so that
 * the values are already known by the time the summary screen is opened.</p>
 */
public record SyncXpRewardPacket(List<Entry> entries) implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            MidnightThoughtsXpGift.MOD_ID, "sync_xp_reward");
    public static final CustomPacketPayload.Type<SyncXpRewardPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    /** Upper bound used while decoding, to guard against a malformed packet. */
    private static final int MAX_ENTRIES = 1024;

    public static final StreamCodec<FriendlyByteBuf, SyncXpRewardPacket> CODEC = StreamCodec.of(
            SyncXpRewardPacket::write,
            SyncXpRewardPacket::read);

    /** A settlement that was granted to one player, for one night. */
    public record Entry(String playerName, int travelExp, int huntExp, int baseBonus, int distanceBlocks, int mobKills) {

        public static Entry of(String playerName, DailyXpStats.XpRewardSettlement settlement) {
            return new Entry(
                    playerName,
                    settlement.travelExp(),
                    settlement.huntExp(),
                    settlement.baseBonus(),
                    settlement.distanceBlocks(),
                    settlement.mobKills());
        }

        /** The total amount of experience granted to this player. */
        public int total() {
            return this.travelExp + this.huntExp + this.baseBonus;
        }
    }

    private static void write(FriendlyByteBuf buffer, SyncXpRewardPacket packet) {
        buffer.writeVarInt(packet.entries().size());

        for (Entry entry : packet.entries()) {
            buffer.writeUtf(entry.playerName());
            buffer.writeVarInt(entry.travelExp());
            buffer.writeVarInt(entry.huntExp());
            buffer.writeVarInt(entry.baseBonus());
            buffer.writeVarInt(entry.distanceBlocks());
            buffer.writeVarInt(entry.mobKills());
        }
    }

    private static SyncXpRewardPacket read(FriendlyByteBuf buffer) {
        int size = Math.min(buffer.readVarInt(), MAX_ENTRIES);
        List<Entry> entries = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            entries.add(new Entry(
                    buffer.readUtf(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt(),
                    buffer.readVarInt()));
        }

        return new SyncXpRewardPacket(List.copyOf(entries));
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
