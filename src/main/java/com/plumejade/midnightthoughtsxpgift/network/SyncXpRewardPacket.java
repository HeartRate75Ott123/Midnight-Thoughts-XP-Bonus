package com.plumejade.midnightthoughtsxpgift.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import com.plumejade.midnightthoughtsxpgift.MidnightThoughtsXpGift;

/**
 * Tells the client how much experience it was awarded for the night it just slept through.
 *
 * <p>The client needs these numbers to render the {@code 经验奖励} entry on the Midnight Thoughts
 * morning summary panel. The packet is deliberately sent before Midnight Thoughts' own
 * {@code DailySummaryPacket} so that the values are already known by the time the summary screen is
 * opened.</p>
 *
 * @param travelExp experience earned by travelling
 * @param huntExp   experience earned by killing monsters
 * @param baseBonus the fixed bonus for having actually slept through the night
 * @param distanceBlocks how many blocks were travelled (display/information only)
 * @param mobKills  how many monsters were killed (display/information only)
 */
public record SyncXpRewardPacket(int travelExp, int huntExp, int baseBonus, int distanceBlocks, int mobKills)
        implements CustomPacketPayload {

    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(
            MidnightThoughtsXpGift.MOD_ID, "sync_xp_reward");
    public static final CustomPacketPayload.Type<SyncXpRewardPacket> TYPE = new CustomPacketPayload.Type<>(ID);

    public static final StreamCodec<FriendlyByteBuf, SyncXpRewardPacket> CODEC = StreamCodec.of(
            SyncXpRewardPacket::write,
            SyncXpRewardPacket::read);

    private static void write(FriendlyByteBuf buffer, SyncXpRewardPacket packet) {
        buffer.writeVarInt(packet.travelExp());
        buffer.writeVarInt(packet.huntExp());
        buffer.writeVarInt(packet.baseBonus());
        buffer.writeVarInt(packet.distanceBlocks());
        buffer.writeVarInt(packet.mobKills());
    }

    private static SyncXpRewardPacket read(FriendlyByteBuf buffer) {
        return new SyncXpRewardPacket(
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readVarInt());
    }

    /** The total amount of experience granted for this settlement. */
    public int total() {
        return this.travelExp + this.huntExp + this.baseBonus;
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
