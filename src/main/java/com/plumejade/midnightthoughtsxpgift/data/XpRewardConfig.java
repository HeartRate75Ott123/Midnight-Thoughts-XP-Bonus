package com.plumejade.midnightthoughtsxpgift.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * The (data pack configurable) reward rules used when a player's sleep is settled.
 *
 * <p>The values are read from {@code data/<namespace>/midnight_thoughts_xp_gift/*.json},
 * so any data pack can override the thresholds without touching the mod jar. Every file found in
 * that directory is decoded with {@link #CODEC}; files that are not present simply keep the
 * defaults below.</p>
 *
 * <p>Default file shipped with the mod:</p>
 * <pre>
 * {
 *   "blocks_per_distance_exp": 64,
 *   "distance_exp": 10,
 *   "kills_per_hunt_exp": 10,
 *   "hunt_exp": 30,
 *   "base_bonus": 16
 * }
 * </pre>
 */
public record XpRewardConfig(
        int blocksPerDistanceExp,
        int distanceExp,
        int killsPerHuntExp,
        int huntExp,
        int baseBonus) {

    /** What the game falls back to when no data pack provides a config file. */
    public static final XpRewardConfig DEFAULT = new XpRewardConfig(64, 10, 10, 30, 16);

    public static final Codec<XpRewardConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            positiveField("blocks_per_distance_exp", 64).forGetter(XpRewardConfig::blocksPerDistanceExp),
            nonNegativeField("distance_exp", 10).forGetter(XpRewardConfig::distanceExp),
            positiveField("kills_per_hunt_exp", 10).forGetter(XpRewardConfig::killsPerHuntExp),
            nonNegativeField("hunt_exp", 30).forGetter(XpRewardConfig::huntExp),
            nonNegativeField("base_bonus", 16).forGetter(XpRewardConfig::baseBonus)
    ).apply(instance, XpRewardConfig::new));

    private static MapCodec<Integer> positiveField(String name, int defaultValue) {
        return Codec.intRange(1, Integer.MAX_VALUE).optionalFieldOf(name, defaultValue);
    }

    private static MapCodec<Integer> nonNegativeField(String name, int defaultValue) {
        return Codec.intRange(0, Integer.MAX_VALUE).optionalFieldOf(name, defaultValue);
    }

    /**
     * Experience awarded for travelling {@code distanceBlocks} blocks.
     */
    public int travelExp(int distanceBlocks) {
        if (distanceBlocks <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, (long) (distanceBlocks / this.blocksPerDistanceExp) * this.distanceExp);
    }

    /**
     * Experience awarded for killing {@code mobKills} monsters.
     */
    public int huntExp(int mobKills) {
        if (mobKills <= 0) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, (long) (mobKills / this.killsPerHuntExp) * this.huntExp);
    }
}
