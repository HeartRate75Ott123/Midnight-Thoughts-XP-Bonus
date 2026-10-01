package com.plumejade.midnightthoughtsxpgift.mixin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import com.plumejade.midnightthoughtsxpgift.MidnightThoughtsXpGift;
import com.plumejade.midnightthoughtsxpgift.server.XpRewardManager;

import mt.server.DailyPlayerStats;
import mt.server.DailyStatsManager;

/**
 * Hooks Midnight Thoughts so the daily experience reward is settled at exactly the same moment the
 * mod settles its own "since last sleep" statistics.
 *
 * <p>{@code DailyStatsManager.showDailySummary(MinecraftServer, Set)} is invoked by Midnight
 * Thoughts once per morning, for the set of players that actually slept through the night. It walks
 * that set twice: once to compute the per player deltas and once to build the packet that is sent to
 * the clients. The {@link Redirect} below hooks the first loop, while the night is still unsettled
 * and before Midnight Thoughts resets its own counters, which makes it the perfect place to settle
 * this addon's rewards.</p>
 */
@Pseudo
@Mixin(targets = "mt.server.DailyStatsManager", remap = false)
public abstract class DailyStatsManagerMixin {

    /**
     * The players whose night is currently being settled. Set at the head of
     * {@code showDailySummary} and cleared again when it returns.
     */
    @Unique
    private static Set<UUID> midnightThoughtsXpGift$sleepingPlayers;

    /** The players settled during the current run of {@code showDailySummary}. */
    @Unique
    private static List<ServerPlayer> midnightThoughtsXpGift$settledPlayers;

    @Inject(method = "showDailySummary", at = @At(value = "HEAD"))
    private static void midnightThoughtsXpGift$captureSleepingPlayers(
            MinecraftServer server,
            Set<UUID> sleepingPlayers,
            CallbackInfo callback) {
        midnightThoughtsXpGift$sleepingPlayers = new HashSet<>(sleepingPlayers);
        midnightThoughtsXpGift$settledPlayers = new ArrayList<>(sleepingPlayers.size());
    }

    @Inject(method = "showDailySummary", at = @At(value = "RETURN"))
    private static void midnightThoughtsXpGift$settleCapturedPlayers(
            MinecraftServer server,
            Set<UUID> sleepingPlayers,
            CallbackInfo callback) {
        try {
            List<ServerPlayer> settled = midnightThoughtsXpGift$settledPlayers;
            if (settled != null && !settled.isEmpty()) {
                XpRewardManager.settleAll(settled);
            }
        } catch (Throwable throwable) {
            MidnightThoughtsXpGift.LOGGER.error("Failed to settle the daily XP rewards", throwable);
        } finally {
            midnightThoughtsXpGift$sleepingPlayers = null;
            midnightThoughtsXpGift$settledPlayers = null;
        }
    }

    @Redirect(
            method = "showDailySummary",
            at = @At(
                    value = "INVOKE",
                    target = "Lmt/server/DailyStatsManager;getOrCreateStats(Ljava/util/UUID;)Lmt/server/DailyPlayerStats;",
                    ordinal = 0))
    private static DailyPlayerStats midnightThoughtsXpGift$collectSleptPlayer(UUID playerId) {
        DailyPlayerStats stats = DailyStatsManager.getOrCreateStats(playerId);
        Set<UUID> sleeping = midnightThoughtsXpGift$sleepingPlayers;

        if (sleeping == null || !sleeping.contains(playerId)) {
            return stats;
        }

        try {
            ServerPlayer player = midnightThoughtsXpGift$findPlayer(playerId);
            if (player == null) {
                return stats;
            }

            DailyPlayerStats.DailyDelta delta = stats.calculateDelta(player);
            MidnightThoughtsXpGift.LOGGER.debug(
                    "{} slept through the night: {} blocks travelled, {} monsters killed",
                    player.getGameProfile().getName(),
                    delta.distanceWalked(),
                    delta.mobsKilled());

            List<ServerPlayer> settled = midnightThoughtsXpGift$settledPlayers;
            if (settled != null) {
                settled.add(player);
            }
        } catch (Throwable throwable) {
            MidnightThoughtsXpGift.LOGGER.error("Failed to collect the night of {}", playerId, throwable);
        }

        return stats;
    }

    @Unique
    private static ServerPlayer midnightThoughtsXpGift$findPlayer(UUID playerId) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? null : server.getPlayerList().getPlayer(playerId);
    }
}