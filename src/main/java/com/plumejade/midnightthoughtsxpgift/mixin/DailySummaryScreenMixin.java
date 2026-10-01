package com.plumejade.midnightthoughtsxpgift.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.plumejade.midnightthoughtsxpgift.client.ClientPayloadHandler;

/**
 * Drops the cached experience rewards of the night as soon as Midnight Thoughts closes its morning
 * summary panel.
 *
 * <p>Without this the values of the previous night would still be around the next time the panel is
 * opened, which would briefly show stale numbers (and would show a stale entry for a player that did not
 * sleep the following night). The cache is keyed by player name, so it is not tied to the local player in
 * any way.</p>
 *
 * <p>The target is {@code DailySummaryScreen#onClose()V}, which Midnight Thoughts overrides to send its
 * {@code SummaryAcknowledgePacket} and then calls {@code Screen#onClose()}. Injecting at the head of that
 * override therefore runs before the screen is actually dismissed.</p>
 */
@Pseudo
@Mixin(targets = "mt.client.ui.DailySummaryScreen", remap = false)
public abstract class DailySummaryScreenMixin {

    @Inject(method = "onClose()V", at = @At(value = "HEAD"))
    private void midnightThoughtsXpGift$onSummaryClosed(CallbackInfo callback) {
        ClientPayloadHandler.handleSummaryClosed();
    }
}
