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
import com.plumejade.midnightthoughtsxpgift.server.NightSettlement;
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

    /** The activity settled during the current run of {@code showDailySummary}. */
    @Unique
    private static List<NightSettlement> midnightThoughtsXpGift$settledNights;

    @Inject(method = "showDailySummary", at = @At(value = "HEAD"))
    private static void midnightThoughtsXpGift$captureSleepingPlayers(
            MinecraftServer server,
            Set<UUID> sleepingPlayers,
            CallbackInfo callback) {
        midnightThoughtsXpGift$sleepingPlayers = new HashSet<>(sleepingPlayers);
        midnightThoughtsXpGift$settledNights = new ArrayList<>(sleepingPlayers.size());
    }

    /**
     * Settles the collected nights just before Midnight Thoughts builds and sends its own summary packets.
     *
     * <p>This point (the single {@code determineMvp} call) is reached right after the first loop, so all
     * deltas of the night are known, and it is <em>before</em> Midnight Thoughts' {@code DailySummaryPacket}
     * is written to the connection. That guarantees the client already holds the reward table when the
     * summary screen opens, instead of having to wait for a packet that arrives afterwards.</p>
     */
    @Inject(
            method = "showDailySummary",
            at = @At(
                    value = "INVOKE",
                    target = "Lmt/server/AchievementCalculator;determineMvp(Ljava/util/List;)Ljava/lang/String;"))
    private static void midnightThoughtsXpGift$settleCapturedPlayers(
            MinecraftServer server,
            Set<UUID> sleepingPlayers,
            CallbackInfo callback) {
        try {
            List<NightSettlement> nights = midnightThoughtsXpGift$settledNights;
            if (nights != null && !nights.isEmpty()) {
                XpRewardManager.settleAll(nights);
            }
        } catch (Throwable throwable) {
            MidnightThoughtsXpGift.LOGGER.error("Failed to settle the daily XP rewards", throwable);
        } finally {
            midnightThoughtsXpGift$sleepingPlayers = null;
            midnightThoughtsXpGift$settledNights = null;
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
            // The deltas Midnight Thoughts computed for this night. distanceWalked is in centimetres
            // (the unit of the vanilla *_one_cm statistics), mobsKilled is the mob_kills delta.
            NightSettlement night = new NightSettlement(player, delta.distanceWalked(), delta.mobsKilled());

            MidnightThoughtsXpGift.LOGGER.debug(
                    "{} slept through the night: {} cm travelled, {} mobs killed",
                    player.getGameProfile().getName(),
                    night.distanceCentimeters(),
                    night.mobKills());

            List<NightSettlement> nights = midnightThoughtsXpGift$settledNights;
            if (nights != null) {
                nights.add(night);
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