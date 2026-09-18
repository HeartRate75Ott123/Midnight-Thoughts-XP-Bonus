package com.plumejade.midnightthoughtsxpgift.client;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import com.plumejade.midnightthoughtsxpgift.MidnightThoughtsXpGift;
import com.plumejade.midnightthoughtsxpgift.network.SyncXpRewardPacket;

import mt.client.ui.summary.AnimationHelper;
import mt.client.ui.summary.BadgeDimensions;
import mt.client.ui.summary.RenderHelper;
import mt.client.ui.summary.RenderUtils;
import mt.client.ui.summary.SummaryConstants;
import mt.client.ui.summary.SummaryDimensions;
import mt.client.ui.summary.ThemeColors;
import mt.client.util.NumberFormatter;
import mt.config.MidnightThoughtsConfig;

/**
 * Renders the additional {@code 经验奖励} (experience reward) entry on the Midnight Thoughts morning
 * summary panel.
 *
 * <p>The entry is drawn as one extra badge in exactly the same style as the vanilla statistics of
 * Midnight Thoughts (same theme aware badge background, same text colour, same roll up animation),
 * placed as a full width row directly below the six statistic badges. The icon is this addon's own
 * experience orb sprite, the label is {@code 经验奖励} and the value is the amount of experience the
 * player was awarded for the night.</p>
 */
public final class XpRewardBadgeRenderer {

    /**
     * The experience orb sprite used as the icon of the entry.
     *
     * <p>This is a single 16x16 sprite shipped by this addon. The vanilla
     * {@code minecraft:textures/entity/experience_orb.png} cannot be used directly: it is a 64x64
     * sprite sheet holding sixteen 16x16 frames, so blitting it as one icon would sample the whole
     * sheet.</p>
     */
    public static final ResourceLocation EXPERIENCE_ORB_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            MidnightThoughtsXpGift.MOD_ID, "textures/gui/experience_orb.png");

    /** The label of the entry. Always displayed in Chinese, with an English translation available. */
    private static final String LABEL_KEY = "midnight_thoughts_xp_gift.summary.xp_bonus";
    private static final String LABEL_FALLBACK = "经验奖励";

    /** Width and height of the experience orb sprite, which is blitted 1:1 into the icon box. */
    private static final int ORB_SPRITE_SIZE = 16;

    private XpRewardBadgeRenderer() {
    }

    /**
     * Draws the badge for one player row.
     *
     * @param context the gui graphics of the summary screen
     * @param font    the font used by the screen
     * @param x       the left edge of the statistics area of the row
     * @param y       the top edge of the statistics area of the row
     * @param width   the width of the statistics area of the row
     * @param height  the height of the statistics area of the row
     * @param dims    the layout dimensions of the summary screen
     * @param animationStartTime the timestamp the row animation started at
     * @param fadeAlpha the current fade in alpha of the screen
     */
    public static void render(
            GuiGraphics context,
            Font font,
            int x,
            int y,
            int width,
            int height,
            SummaryDimensions dims,
            long animationStartTime,
            float fadeAlpha) {
        SyncXpRewardPacket reward = getLocalReward();

        BadgeDimensions badgeDims = BadgeDimensions.calculate(dims);
        int badgeHeight = badgeDims.height();
        int rowSpacing = badgeDims.rowSpacing();
        int iconSize = Math.max(6, Math.min(badgeHeight - 2, (int) (badgeHeight * 0.7F)));
        float textScale = badgeDims.textScale();

        // Same geometry the six statistic badges use, so the extra row is perfectly aligned with them.
        int gridHeight = 3 * badgeHeight + 2 * rowSpacing;
        int gridStartY = y + Math.max(0, (height - gridHeight) / 2);
        int badgeY = gridStartY + gridHeight + rowSpacing;

        if (badgeY + badgeHeight > y + height) {
            // Not enough room (very small screen / extreme gui scale): instead of drawing over the
            // statistics, the entry is placed directly underneath the row.
            badgeY = y + height - badgeHeight;
        }

        if (badgeY < y || badgeY + badgeHeight > y + height) {
            return;
        }

        String theme = MidnightThoughtsConfig.getInstance().getUiTheme();
        int textColor = ThemeColors.getThemeColors(theme).statTextColor();
        float animationProgress = AnimationHelper.getProgress(animationStartTime);
        int animatedValue = NumberFormatter.safeAnimatedValue(reward == null ? 0 : reward.total(), animationProgress);

        int textureWidth = 100;
        int textureHeight = 14;
        RenderHelper.ScaledBlit scaled = RenderHelper.computeScaledBlit(badgeY, width, badgeHeight, textureWidth, textureHeight);
        RenderHelper.blitTexture(context, SummaryConstants.getStatBadgeTexture(), x, scaled.renderY(), scaled.renderW(), scaled.renderH(), fadeAlpha);

        int padding = Math.max(3, (int) (4.0F * ((float) scaled.renderH() / (float) textureHeight)));
        int iconY = badgeY + (badgeHeight - iconSize) / 2;
        RenderHelper.blitTextureSimple(
                context, EXPERIENCE_ORB_TEXTURE, x + padding, iconY, iconSize, iconSize, ORB_SPRITE_SIZE, ORB_SPRITE_SIZE, fadeAlpha);

        int alpha = (int) (fadeAlpha * 255.0F);
        int color = alpha << 24 | textColor;
        int textY = badgeY + (badgeHeight - (int) (8.0F * textScale)) / 2;

        Component label = Component.translatableWithFallback(LABEL_KEY, LABEL_FALLBACK);
        RenderUtils.renderScaledText(
                context, font, label.getString(), x + padding + iconSize + padding, textY, color, textScale, true);

        String valueText = NumberFormatter.formatLargeNumber(animatedValue);
        int valueWidth = (int) ((float) font.width(valueText) * textScale);
        int valueX = x + scaled.renderW() - padding - valueWidth;
        RenderUtils.renderScaledText(context, font, valueText, valueX, textY, color, textScale, true);
    }

    /** The reward that was settled for the local player, or {@code null} when there is none. */
    private static SyncXpRewardPacket getLocalReward() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return null;
        }

        UUID playerId = minecraft.player.getUUID();
        return ClientXpRewardStore.get(playerId);
    }
}
