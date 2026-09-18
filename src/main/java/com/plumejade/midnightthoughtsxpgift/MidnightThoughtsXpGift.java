package com.plumejade.midnightthoughtsxpgift;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

/**
 * Shared constants for Midnight Thoughts: XP Bonus.
 */
public final class MidnightThoughtsXpGift {
    /** The mod id. Must match the {@code @Mod} annotation and {@code neoforge.mods.toml}. */
    public static final String MOD_ID = "midnight_thoughts_xp_gift";

    /** The id of the linked mod. */
    public static final String MIDNIGHT_THOUGHTS_MOD_ID = "midnightthoughts";

    public static final Logger LOGGER = LogUtils.getLogger();

    private MidnightThoughtsXpGift() {
    }
}
