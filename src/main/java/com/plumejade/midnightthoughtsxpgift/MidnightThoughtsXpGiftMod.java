package com.plumejade.midnightthoughtsxpgift;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.plumejade.midnightthoughtsxpgift.data.XpRewardConfigReloadListener;
import com.plumejade.midnightthoughtsxpgift.network.SyncXpRewardPacket;
import com.plumejade.midnightthoughtsxpgift.server.DailyXpStats;
import com.plumejade.midnightthoughtsxpgift.server.XpRewardManager;

/**
 * Midnight Thoughts: XP Bonus.
 *
 * <p>An addon for <em>Midnight Thoughts</em> that pays out a small amount of experience for the day
 * that just ended: a reward for travelling, a reward for slaying monsters and a fixed bonus for
 * having actually slept through the night. The rewards are settled exactly when Midnight Thoughts
 * settles its own "since last sleep" statistics and are both shown on the morning summary panel and
 * written to the chat.</p>
 *
 * <p>All thresholds are data pack configurable through
 * {@code data/<namespace>/midnight_thoughts_xp_gift/*.json}.</p>
 */
@Mod(MidnightThoughtsXpGift.MOD_ID)
public class MidnightThoughtsXpGiftMod {

    public MidnightThoughtsXpGiftMod(IEventBus modEventBus) {
        modEventBus.addListener(this::onRegisterPayloads);
        NeoForge.EVENT_BUS.register(this);
    }

    /** Registers the server to client reward synchronisation. */
    private void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToClient(
                SyncXpRewardPacket.TYPE,
                SyncXpRewardPacket.CODEC,
                (payload, context) -> com.plumejade.midnightthoughtsxpgift.client.ClientPayloadHandler.handleXpReward(payload));
    }

    /** Makes the data pack configurable reward rules available. */
    @SubscribeEvent
    public void onAddReloadListener(AddReloadListenerEvent event) {
        event.addListener(XpRewardConfigReloadListener.get());
    }

    /** Keeps track of how far every player travelled. */
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DailyXpStats.get(player).tick(player);
        }
    }

    /** Pays out the rewards that were settled during the wake up handling of this tick. */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        XpRewardManager.payPending(event.getServer());
    }

    /** Drops the tracking state of players that left. */
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
        DailyXpStats.forget(event.getEntity().getUUID());
    }

    /** Drops all state when the server stops. */
    @SubscribeEvent
    public void onServerStopped(ServerStoppedEvent event) {
        DailyXpStats.clear();
        XpRewardManager.clear();
    }
}
