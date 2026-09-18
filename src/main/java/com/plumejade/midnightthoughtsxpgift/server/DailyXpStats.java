package com.plumejade.midnightthoughtsxpgift.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.world.phys.Vec3;

import com.plumejade.midnightthoughtsxpgift.data.XpRewardConfig;

/**
 * Tracks the per player numbers the daily rewards are paid for.
 *
 * <p>The travelled distance is accumulated from the player's actual movement (which covers every
 * way of moving: walking, sprinting, swimming, flying, riding, being pushed around, ...), while the
 * amount of slain monsters is read straight from the vanilla statistics, exactly like Midnight
 * Thoughts itself does. Both counters are reset once the night is settled, so a settlement always
 * covers "everything that happened since the previous sleep".</p>
 */
public final class DailyXpStats {

    private static final Map<UUID, DailyXpStats> STATS = new ConcurrentHashMap<>();

    /**
     * The vanilla statistic counting slain monsters (it only counts hostile monsters, exactly like
     * the "Monster Hunter" advancement).
     */
    private static final Stat<ResourceLocation> MOB_KILLS = Stats.CUSTOM.get(Stats.MOB_KILLS);

    private long distanceBlocks;
    private int lastMobKills;
    private boolean initialized;
    private Vec3 lastPosition;

    private DailyXpStats() {
    }

    /** The tracking state of a player, created on demand. */
    public static DailyXpStats get(ServerPlayer player) {
        return STATS.computeIfAbsent(player.getUUID(), uuid -> new DailyXpStats());
    }

    /**
     * Called once per server tick and player. Makes sure the baseline is captured for players that
     * just joined and accumulates their movement into {@link #distanceBlocks()}.
     */
    public void tick(ServerPlayer player) {
        if (!this.initialized) {
            this.initialized = true;
            this.lastMobKills = mobKillsOf(player);
            this.lastPosition = player.position();
            return;
        }

        Vec3 position = player.position();
        Vec3 previous = this.lastPosition;

        if (previous != null) {
            double dx = position.x - previous.x;
            double dz = position.z - previous.z;
            double dy = position.y - previous.y;

            // Ignore absurd jumps (teleports, dimension changes, respawns) instead of paying out for them.
            if (dx * dx + dy * dy + dz * dz < 1.0E8D) {
                this.distanceBlocks += Math.round(Math.sqrt(dx * dx + dz * dz));
            }
        }

        this.lastPosition = position;
    }

    /** The accumulated travelled distance in blocks. */
    public long distanceBlocks() {
        return this.distanceBlocks;
    }

    /**
     * The number of monsters killed since the previous settlement, read from the vanilla statistics.
     */
    public int mobKills(ServerPlayer player) {
        return Math.max(0, mobKillsOf(player) - this.lastMobKills);
    }

    /**
     * Computes the experience this player earned and resets the counters for the next night.
     */
    public XpRewardSettlement settle(ServerPlayer player, XpRewardConfig config) {
        int distance = (int) Math.min(Integer.MAX_VALUE, this.distanceBlocks);
        int kills = this.mobKills(player);
        int totalMobKills = mobKillsOf(player);

        this.distanceBlocks = 0L;
        this.lastMobKills = totalMobKills;
        this.lastPosition = null;
        this.initialized = false;

        return new XpRewardSettlement(
                config.travelExp(distance),
                config.huntExp(kills),
                config.baseBonus(),
                distance,
                kills);
    }

    private static int mobKillsOf(ServerPlayer player) {
        return player.getStats().getValue(MOB_KILLS);
    }

    /** Drops the tracking state of a player that left the server. */
    public static void forget(UUID playerId) {
        STATS.remove(playerId);
    }

    /** Drops all tracking state, e.g. when the server stops. */
    public static void clear() {
        STATS.clear();
    }

    /**
     * The raw result of a settlement. {@link #travelExp()}, {@link #huntExp()} and
     * {@link #baseBonus()} are the values the player is actually paid.
     */
    public record XpRewardSettlement(int travelExp, int huntExp, int baseBonus, int distanceBlocks, int mobKills) {

        /** The total experience of this settlement. */
        public int totalExp() {
            return this.travelExp + this.huntExp + this.baseBonus;
        }
    }
}
