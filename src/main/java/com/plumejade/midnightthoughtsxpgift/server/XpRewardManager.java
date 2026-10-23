package com.plumejade.midnightthoughtsxpgift.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.plumejade.midnightthoughtsxpgift.MidnightThoughtsXpGift;
import com.plumejade.midnightthoughtsxpgift.data.XpRewardConfig;
import com.plumejade.midnightthoughtsxpgift.data.XpRewardConfigReloadListener;
import com.plumejade.midnightthoughtsxpgift.network.SyncXpRewardPacket;
import com.plumejade.midnightthoughtsxpgift.network.XpGiftNetwork;

/**
 * Settles and pays out the daily experience rewards.
 *
 * <p>{@link #settleAll(List)} is called right when Midnight Thoughts settles the night, using the
 * statistics Midnight Thoughts itself computed for the night (see {@link NightSettlement}). Because those
 * statistics are measured against a baseline that Midnight Thoughts persists to the world storage, the
 * activity of a player that disconnected and reconnected before sleeping is <strong>not lost</strong>.</p>
 *
 * <p>The payout itself is deferred to the next server tick via {@link #payPending(MinecraftServer)} so that
 * the reward packet reaches the client before Midnight Thoughts opens its morning summary screen, and so
 * that the "you gained experience" chat message is not immediately overwritten by the wake up animation.</p>
 */
public final class XpRewardManager {

    private static final int COLOR_TITLE = 0x55FF55; // ChatFormatting.GREEN, section sign a
    private static final int COLOR_REWARD = 0xAAAAAA; // ChatFormatting.GRAY, section sign 7
    private static final int COLOR_TOTAL = 0x55FFFF; // ChatFormatting.AQUA, section sign b
    private static final String SEPARATOR = "——————————";

    /** The settlement that still has to be granted to each player, keyed by player id. */
    private static final Map<UUID, SyncXpRewardPacket.Entry> PENDING = new ConcurrentHashMap<>();

    private XpRewardManager() {
    }

    /**
     * Settles the night for every player that slept through it.
     *
     * <p>All settlements are computed before anything is sent, because the morning summary panel shows
     * one row per player and every row has to display the value of <em>that</em> player, regardless of
     * who is looking at the screen. The complete table is therefore synchronised to each of the players
     * that slept.</p>
     *
     * @param nights the settled activity of each player (may be empty)
     */
    public static void settleAll(List<NightSettlement> nights) {
        if (nights.isEmpty()) {
            return;
        }

        XpRewardConfig config = XpRewardConfigReloadListener.getConfig();
        List<SyncXpRewardPacket.Entry> entries = new ArrayList<>(nights.size());
        List<ServerPlayer> players = new ArrayList<>(nights.size());

        for (NightSettlement night : nights) {
            ServerPlayer player = night.player();
            SyncXpRewardPacket.Entry entry = settlementOf(night, config);

            entries.add(entry);
            players.add(player);
            PENDING.put(player.getUUID(), entry);

            MidnightThoughtsXpGift.LOGGER.debug(
                    "Settled {} exp for {} (travel {}, hunt {}, bonus {})",
                    entry.total(),
                    entry.playerName(),
                    entry.travelExp(),
                    entry.huntExp(),
                    entry.baseBonus());
        }

        SyncXpRewardPacket payload = new SyncXpRewardPacket(List.copyOf(entries));

        for (ServerPlayer player : players) {
            XpGiftNetwork.sendReward(player, payload);
        }
    }

    /**
     * Computes the reward entry of one player from the activity Midnight Thoughts reported for the night.
     */
    static SyncXpRewardPacket.Entry settlementOf(NightSettlement night, XpRewardConfig config) {
        return settlementOf(
                night.player().getGameProfile().getName(),
                night.distanceCentimeters(),
                night.mobKills(),
                config);
    }

    /**
     * The pure reward computation: vanilla centimetres and mob kills in, one reward entry out.
     *
     * <p>Kept free of game objects so it can be unit tested directly.</p>
     */
    static SyncXpRewardPacket.Entry settlementOf(
            String playerName,
            int distanceCentimeters,
            int mobKills,
            XpRewardConfig config) {
        int blocks = NightSettlement.blocksOf(distanceCentimeters);
        int kills = Math.max(0, mobKills);

        return new SyncXpRewardPacket.Entry(
                playerName,
                config.travelExp(blocks),
                config.huntExp(kills),
                config.baseBonus(),
                blocks,
                kills);
    }

    /**
     * Grants the settled experience and sends the summary message to the chat, once per settlement.
     */
    public static void payPending(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SyncXpRewardPacket.Entry entry = PENDING.remove(player.getUUID());
            if (entry == null) {
                continue;
            }

            if (entry.total() > 0) {
                player.giveExperiencePoints(entry.total());
            }

            player.sendSystemMessage(buildSummaryMessage(entry));
        }
    }

    /** Forgets a settlement that could not be paid out (e.g. the player left), on server stop. */
    public static void clear() {
        PENDING.clear();
    }

    private static Component buildSummaryMessage(SyncXpRewardPacket.Entry entry) {
        return Component.empty()
                .append(colored(Component.translatable("midnight_thoughts_xp_gift.message.title"), COLOR_TITLE))
                .append(Component.literal("\n"))
                .append(colored(rewardLine("midnight_thoughts_xp_gift.message.travel", entry.travelExp()), COLOR_REWARD))
                .append(Component.literal("\n"))
                .append(colored(rewardLine("midnight_thoughts_xp_gift.message.hunt", entry.huntExp()), COLOR_REWARD))
                .append(Component.literal("\n"))
                .append(colored(rewardLine("midnight_thoughts_xp_gift.message.bonus", entry.baseBonus()), COLOR_REWARD))
                .append(Component.literal("\n"))
                .append(colored(Component.literal(SEPARATOR), COLOR_TOTAL))
                .append(Component.literal("\n"))
                .append(colored(Component.translatable("midnight_thoughts_xp_gift.message.total", entry.total()), COLOR_TOTAL));
    }

    private static Component rewardLine(String translationKey, int amount) {
        return Component.translatable(translationKey, amount);
    }

    private static Component colored(Component component, int rgb) {
        return component.copy().withStyle(style -> style.withColor(rgb).withItalic(false));
    }
}
