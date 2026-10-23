package com.plumejade.midnightthoughtsxpgift.server;

import net.minecraft.server.level.ServerPlayer;

/**
 * One player's unsettled activity of the night, as reported by Midnight Thoughts.
 *
 * <p>The numbers come straight from Midnight Thoughts' own daily statistics
 * ({@code DailyPlayerStats.DailyDelta}): {@code distanceCentimeters} is the sum of all vanilla
 * {@code *_one_cm} distance statistics and {@code mobKills} is the vanilla
 * {@code minecraft.mob_kills} delta. Both are measured against a baseline that Midnight Thoughts
 * persists to the world storage, so the activity is <strong>not lost when a player disconnects and
 * reconnects before sleeping</strong>.</p>
 *
 * @param player              the player whose night is settled
 * @param distanceCentimeters the distance travelled since the previous sleep, in centimetres
 *                            (vanilla statistics unit, 100 cm = 1 block)
 * @param mobKills            the number of mobs killed since the previous sleep
 */
public record NightSettlement(ServerPlayer player, int distanceCentimeters, int mobKills) {

    public NightSettlement {
        distanceCentimeters = Math.max(0, distanceCentimeters);
        mobKills = Math.max(0, mobKills);
    }

    /**
     * Converts the vanilla distance statistics unit (centimetres) into whole blocks.
     *
     * @param distanceCentimeters a distance in centimetres, as reported by the vanilla {@code *_one_cm}
     *                            statistics; negative input is treated as zero
     * @return the distance in whole blocks (100 cm = 1 block), truncated
     */
    public static int blocksOf(int distanceCentimeters) {
        return Math.max(0, distanceCentimeters) / 100;
    }

    /** The travelled distance in whole blocks, which is what the reward thresholds are expressed in. */
    public int distanceBlocks() {
        return blocksOf(this.distanceCentimeters);
    }
}
