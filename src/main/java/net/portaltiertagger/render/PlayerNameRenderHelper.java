package net.portaltiertagger.render;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.class_1657;
import net.minecraft.class_243;
import net.minecraft.class_2561;
import net.minecraft.class_2583;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_327;
import net.minecraft.class_4587;
import net.minecraft.class_4597;
import net.minecraft.class_5250;
import net.portaltiertagger.PortalTierTagger;
import net.portaltiertagger.config.ModConfig;
import net.portaltiertagger.network.RankingEntry;

import java.awt.Color;

/**
 * Renders the cached Portal Tier above (or beside) nearby players' heads.
 *
 * <p>Registered against {@code WorldRenderEvents.AFTER_ENTITIES} in
 * {@link PortalTierTagger#onInitializeClient()}. That is the critical detail:
 * this code was previously wired to {@code WorldRenderEvents.LAST}, which
 * fires after the shared entity {@code VertexConsumerProvider.Immediate} has
 * already been flushed for the frame, so anything queued here never made it
 * to the screen. AFTER_ENTITIES fires before that flush, while we're still
 * allowed to add geometry to the same buffer entities/nametags use.</p>
 */
public class PlayerNameRenderHelper {

    // Font identifier — resolves to:
    //   assets/portal_tier_tagger/font/default.json
    private static final class_2960 FONT_ID =
            class_2960.method_60655("portal_tier_tagger", "default");

    // Resource-manager path used only to check that the font file is present
    private static final class_2960 FONT_RESOURCE_PATH =
            class_2960.method_60655("portal_tier_tagger", "font/default.json");

    // Cached once per session so we only log once
    private static Boolean fontAvailable = null;

    public static void renderTierTags(WorldRenderContext context) {
        ModConfig config = PortalTierTagger.getConfig();
        if (config == null) return;
        if (!config.enabled) return;
        if (config.displaySide == ModConfig.DisplaySide.DISABLED) return;

        class_310 client = class_310.method_1551();
        if (client.field_1724 == null) return;
        if (client.field_1687  == null) return;

        // Check font availability once and log result
        if (fontAvailable == null) {
            fontAvailable = client.method_1478()
                    .method_14486(FONT_RESOURCE_PATH)
                    .isPresent();
            if (fontAvailable) {
                PortalTierTagger.LOGGER.info("[PTT] Custom font loaded: {}", FONT_ID);
            } else {
                PortalTierTagger.LOGGER.warn(
                        "[PTT] Custom font NOT found at {}. Falling back to plain text.",
                        FONT_RESOURCE_PATH);
            }
        }

        class_243 cameraPos = context.camera().method_19326();
        class_4587 matrices = context.matrixStack();
        class_4597 vcp = context.consumers();
        if (vcp == null) return;

        float tickDelta = client.method_60646().method_60637(true);

        for (class_1657 player : client.field_1687.method_18456()) {
            if (player == client.field_1724 && !client.field_1773.method_19418().method_19333()) continue;

            double distSq = player.method_5707(cameraPos);
            if (distSq > (double) config.distanceCutoff * config.distanceCutoff) continue;

            if (!player.method_5805()) continue;
            if (player.method_5756(client.field_1724)) continue;

            String name = player.method_5477().getString();
            RankingEntry entry = PortalTierTagger.getCache().get(name);
            if (entry == null) continue;

            RankingEntry.HighestTierResult targetTier = null;
            if (config.displayMode == ModConfig.DisplayMode.HIGHEST) {
                targetTier = entry.getHighestTier();
            } else if (config.displayMode == ModConfig.DisplayMode.LOWEST) {
                targetTier = entry.getLowestTier();
            }

            if (targetTier == null) continue;

            renderTierTag(matrices, vcp, player, cameraPos,
                    targetTier.gamemode, targetTier.tier, config, tickDelta);
        }
    }

    private static void renderTierTag(class_4587 matrices, class_4597 vcp,
                                      class_1657 player, class_243 cameraPos,
                                      String gamemode, String tier, ModConfig config,
                                      float tickDelta) {
        class_310 client = class_310.method_1551();
        class_327 textRenderer = client.field_1772;

        // ── Build the display text ──────────────────────────────────────────────
        class_5250 finalText;

        if (Boolean.TRUE.equals(fontAvailable)) {

            // The emoji character and the trailing space must both be inside
            // the custom-font span. Splitting them into separately-styled
            // literals causes the space to fall back to the default font and
            // throws off the glyph advance, pushing the tier text too far
            // right.
            class_2583 emojiStyle  = class_2583.field_24360.method_27704(FONT_ID);
            class_5250 emojiText = class_2561.method_43470(getGamemodeEmoji(gamemode) + " ")
                    .method_10862(emojiStyle);

            // Alpha must always be 0xFF — withColor(int) treats the int as
            // ARGB, and a zero alpha channel makes the text fully invisible
            // even though the RGB component looks correct.
            String colorHex = config.getRankColor(tier);
            Color  color    = parseHex(colorHex);
            int    colorInt = (0xFF << 24)
                            | (color.getRed()   << 16)
                            | (color.getGreen() << 8)
                            |  color.getBlue();

            class_5250 tierText = class_2561.method_43470("[" + tier + "]").method_54663(colorInt);
            finalText = emojiText.method_10852(tierText);

        } else {
            // Plain-text fallback when font is unavailable
            String code = gamemode.substring(0, Math.min(3, gamemode.length())).toUpperCase();
            finalText = class_2561.method_43470("[" + code + "][" + tier + "]").method_54663(0xAAAAAA);
        }

        // ── Camera-relative interpolated position ──────────────────────────────
        double interpX = player.field_6014 + (player.method_23317() - player.field_6014) * tickDelta;
        double interpY = player.field_6036 + (player.method_23318() - player.field_6036) * tickDelta;
        double interpZ = player.field_5969 + (player.method_23321() - player.field_5969) * tickDelta;

        double relX = interpX - cameraPos.field_1352;
        double relY = interpY - cameraPos.field_1351;
        double relZ = interpZ - cameraPos.field_1350;

        float yOffset   = player.method_17682() + 0.25f + (float) config.offsetY;
        float scale     = 0.025f;
        float textWidth = textRenderer.method_27525(finalText) * scale;

        float xOffset = 0f;
        if (config.displaySide == ModConfig.DisplaySide.LEFT) {
            xOffset = -textWidth - 0.15f;
        } else if (config.displaySide == ModConfig.DisplaySide.RIGHT) {
            xOffset = 0.15f;
        }

        matrices.method_22903();
        matrices.method_22904(relX, relY + yOffset, relZ);
        matrices.method_22907(client.field_1773.method_19418().method_23767());
        // Negative X flips the text right-side-up (MC text renders upside-down)
        matrices.method_22905(-scale, -scale, scale);

        float drawX = -(textRenderer.method_27525(finalText) / 2.0f) + (xOffset / scale);

        textRenderer.method_30882(
                finalText,
                drawX,
                0.0f,
                0xFFFFFF,
                false,
                matrices.method_23760().method_23761(),
                vcp,
                class_327.class_6415.field_33994,  // visible through blocks
                0,
                15728880   // full brightness
        );

        matrices.method_22909();
    }

    // Emoji map — must match the chars defined in default.json.
    // Gamemode keys in RankingEntry are stored lowercase (see addTier()),
    // so we compare against lowercase here.
    private static String getGamemodeEmoji(String gamemode) {
        if (gamemode == null) return "";
        switch (gamemode.toLowerCase()) {
            case "mace":    return "\uE001";
            case "sword":   return "\uE002";
            case "axe":     return "\uE003";
            case "smp":     return "\uE004";
            case "uhc":     return "\uE005";
            case "pot":     return "\uE006";
            case "nethop":  return "\uE007";
            case "vanilla": return "\uE008";
            default:        return "";
        }
    }

    private static Color parseHex(String hex) {
        try {
            if (hex == null) return new Color(0xAAAAAA);
            hex = hex.replace("#", "");
            if (hex.length() == 6) return new Color(Integer.parseInt(hex, 16));
        } catch (NumberFormatException ignored) {}
        return new Color(0xAAAAAA);
    }

    /** Call this on world disconnect to re-check font on next join */
    public static void resetFontCache() {
        fontAvailable = null;
    }
}
