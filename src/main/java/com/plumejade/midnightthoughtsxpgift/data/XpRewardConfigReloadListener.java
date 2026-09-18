package com.plumejade.midnightthoughtsxpgift.data;

import java.util.Map;
import java.util.function.Function;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import com.plumejade.midnightthoughtsxpgift.MidnightThoughtsXpGift;

/**
 * Loads {@link XpRewardConfig} from data packs.
 *
 * <p>Every JSON file found in {@code data/<namespace>/midnight_thoughts_xp_gift/} is decoded with
 * {@link XpRewardConfig#CODEC}. The values of the file shipped by this mod are applied first, so a
 * data pack with a higher priority simply overrides them.</p>
 */
public class XpRewardConfigReloadListener extends SimpleJsonResourceReloadListener {
    /** The data pack directory (relative to {@code data/<namespace>/}) holding reward configs. */
    public static final String DIRECTORY = MidnightThoughtsXpGift.MOD_ID;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final Function<String, IllegalArgumentException> ERROR_FACTORY = IllegalArgumentException::new;

    private static volatile XpRewardConfig config = XpRewardConfig.DEFAULT;
    private static volatile XpRewardConfigReloadListener instance;

    public XpRewardConfigReloadListener() {
        super(GSON, DIRECTORY);
    }

    /** The listener instance registered on the NeoForge event bus. */
    public static XpRewardConfigReloadListener get() {
        XpRewardConfigReloadListener local = instance;
        if (local == null) {
            local = new XpRewardConfigReloadListener();
            instance = local;
        }
        return local;
    }

    /** The currently active reward rules (never {@code null}). */
    public static XpRewardConfig getConfig() {
        return config;
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager, ProfilerFiller profiler) {
        XpRewardConfig loaded = XpRewardConfig.DEFAULT;

        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                loaded = XpRewardConfig.CODEC
                        .parse(JsonOps.INSTANCE, entry.getValue())
                        .getOrThrow(ERROR_FACTORY);
            } catch (Exception exception) {
                MidnightThoughtsXpGift.LOGGER.error("Could not parse XP reward config {}", entry.getKey(), exception);
            }
        }

        config = loaded;
        MidnightThoughtsXpGift.LOGGER.info(
                "XP reward config ready: every {} block(s) -> {} exp, every {} kill(s) -> {} exp, base bonus {}",
                loaded.blocksPerDistanceExp(),
                loaded.distanceExp(),
                loaded.killsPerHuntExp(),
                loaded.huntExp(),
                loaded.baseBonus());
    }
}
