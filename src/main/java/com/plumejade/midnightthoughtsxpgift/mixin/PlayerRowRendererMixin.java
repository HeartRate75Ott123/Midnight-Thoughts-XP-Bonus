package com.plumejade.midnightthoughtsxpgift.mixin;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import com.plumejade.midnightthoughtsxpgift.client.XpRewardBadgeRenderer;

import mt.client.ui.summary.AchievementTooltipArea;
import mt.client.ui.summary.PlayerRowRenderer;
import mt.client.ui.summary.SummaryDimensions;
import mt.network.packet.DailySummaryPacket;

/**
 * Adds the {@code 经验奖励} entry to every player row of the Midnight Thoughts morning summary
 * panel.
 *
 * <p>The {@link Inject} runs after Midnight Thoughts drew the row (name badge, skin, the six
 * statistics and the achievement badges) and adds a seventh, full width badge underneath them. Only
 * the entry of the local player carries a value; the row of any other player simply shows
 * {@code 0}, because a settlement is personal and is only synchronised to the player it belongs
 * to.</p>
 */
@Pseudo
@Mixin(targets = "mt.client.ui.summary.PlayerRowRenderer", remap = false)
public abstract class PlayerRowRendererMixin {

    @Inject(
            method = "render",
            at = @At(value = "TAIL"))
    private static void midnightThoughtsXpGift$renderXpReward(
            GuiGraphics context,
            Font textRenderer,
            DailySummaryPacket.PlayerDailySummary player,
            int x,
            int y,
            SummaryDimensions dims,
            float fadeAlpha,
            long animationStartTime,
            List<AchievementTooltipArea> achievementAreas,
            CallbackInfo callback) {
        // Mirror the geometry Midnight Thoughts uses to place the statistics inside the player row.
        int contentPaddingSides = dims.s(60);
        int rowWidth = dims.panelWidth - contentPaddingSides * 2;
        int rowHeight = dims.playerRowHeight - dims.s(11);
        int headSize = (int) ((float) dims.headSize * 0.75F);
        int leftPad = dims.s(24);
        int headColumnWidth = leftPad + headSize + dims.s(19);
        int pad = dims.s(8);

        int contentX = x + headColumnWidth;
        int contentY = y + pad;
        int contentHeight = rowHeight - pad * 2;
        int contentWidth = rowWidth - headColumnWidth - pad;
        int statsWidth = (int) ((float) contentWidth * 0.65F);

        XpRewardBadgeRenderer.render(
                context,
                textRenderer,
                contentX,
                contentY,
                statsWidth,
                contentHeight,
                dims,
                animationStartTime,
                fadeAlpha);
    }
}
