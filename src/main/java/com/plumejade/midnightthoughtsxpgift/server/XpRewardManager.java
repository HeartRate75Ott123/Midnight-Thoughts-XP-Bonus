package com.plumejade.midnightthoughtsxpgift.server;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import com.plumejade.midnightthoughtsxpgift.MidnightThoughtsXpGift;
import com.plumejade.midnightthoughtsxpgift.data.XpRewardConfigReloadListener;
import com.plumejade.midnightthoughtsxpgift.network.SyncXpRewardPacket;
import com.plumejade.midnightthoughtsxpgift.network.XpGiftNetwork;

/**
 * Settles and pays out the daily experience rewards.
 *
 * <p>{@link #settle(ServerPlayer)} is called right when Midnight Thoughts settles the night (before
 * it resets its own daily counters). The payout itself is deferred to the next server tick via
 * {@link #payPending(MinecraftServer)} so that the reward packet reaches the client before Midnight
 * Thoughts opens its morning summary screen, and so that the "you gained experience" chat message
 * is not immediately overwritten by the wake up animation.</p>
 */
public final class XpRewardManager {

    private static final int COLOR_TITLE = 0x55FF55; // ChatFormatting.GREEN, section sign a
    private static final int COLOR_REWARD = 0xAAAAAA; // ChatFormatting.GRAY, section sign 7
    private static final int COLOR_TOTAL = 0x55FFFF; // ChatFormatting.AQUA, section sign b
    private static final String SEPARATOR = "——————————";

    private static final Map<UUID, SyncXpRewardPacket> PENDING = new ConcurrentHashMap<>();

    private XpRewardManager() {
    }

    /**
     * Computes the reward for a player whose night is being settled, stores it for the pending
     * payout and sends it to the client so the summary panel can display it.
     */
    public static void settle(ServerPlayer player) {
        DailyXpStats stats = DailyXpStats.get(player);
        DailyXpStats.XpRewardSettlement settlement = stats.settle(player, XpRewardConfigReloadListener.getConfig());

        SyncXpRewardPacket payload = new SyncXpRewardPacket(
                settlement.travelExp(),
                settlement.huntExp(),
                settlement.baseBonus(),
                settlement.distanceBlocks(),
                settlement.mobKills());

        PENDING.put(player.getUUID(), payload);
        XpGiftNetwork.sendReward(player, payload);

        MidnightThoughtsXpGift.LOGGER.debug(
                "Settled {} exp for {} (travel {}, hunt {}, bonus {})",
                payload.total(),
                player.getGameProfile().getName(),
                payload.travelExp(),
                payload.huntExp(),
                payload.baseBonus());
    }

    /**
     * Grants the settled experience and sends the summary message to the chat, once per settlement.
     */
    public static void payPending(MinecraftServer server) {
        if (PENDING.isEmpty()) {
            return;
        }

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            SyncXpRewardPacket payload = PENDING.remove(player.getUUID());
            if (payload == null) {
                continue;
            }

            if (payload.total() > 0) {
                player.giveExperiencePoints(payload.total());
            }

            player.sendSystemMessage(buildSummaryMessage(payload));
        }
    }

    /** Forgets a settlement that could not be paid out (e.g. the player left), on server stop. */
    public static void clear() {
        PENDING.clear();
    }

    private static Component buildSummaryMessage(SyncXpRewardPacket payload) {
        return Component.empty()
                .append(colored(Component.translatable("midnight_thoughts_xp_gift.message.title"), COLOR_TITLE))
                .append(Component.literal("\n"))
                .append(colored(rewardLine("midnight_thoughts_xp_gift.message.travel", payload.travelExp()), COLOR_REWARD))
                .append(Component.literal("\n"))
                .append(colored(rewardLine("midnight_thoughts_xp_gift.message.hunt", payload.huntExp()), COLOR_REWARD))
                .append(Component.literal("\n"))
                .append(colored(rewardLine("midnight_thoughts_xp_gift.message.bonus", payload.baseBonus()), COLOR_REWARD))
                .append(Component.literal("\n"))
                .append(colored(Component.literal(SEPARATOR), COLOR_TOTAL))
                .append(Component.literal("\n"))
                .append(colored(Component.translatable("midnight_thoughts_xp_gift.message.total", payload.total()), COLOR_TOTAL));
    }

    private static Component rewardLine(String translationKey, int amount) {
        return Component.translatable(translationKey, amount);
    }

    private static Component colored(Component component, int rgb) {
        return component.copy().withStyle(style -> style.withColor(rgb).withItalic(false));
    }
}
