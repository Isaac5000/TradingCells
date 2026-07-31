package com.cosmocraft.trading_cells.platform.neoforge.client.screen.trader;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import org.jspecify.annotations.Nullable;

/** Shared visual rules for the manual and automatic villager trade screens. */
public final class VillagerTradeScreenCommon {
    public static final int TEXT_DARK = 0xFF3A3A3A;
    private static final int EXPERIENCE_TEXT_COLOR = 0xFF80FF20;
    private static final float EXPERIENCE_TEXT_SCALE = 0.85F;
    private static final int DISCOUNT_OLD_PRICE_COLOR = 0xFFFFFFFF;
    private static final int DISCOUNT_STRIKETHROUGH_COLOR = 0xFFFF3030;
    private static final float DISCOUNT_OLD_PRICE_SCALE = 0.65F;
    private static final Identifier EXPERIENCE_ORB_TEXTURE =
            Identifier.withDefaultNamespace("textures/entity/experience/experience_orb.png");
    private static final Identifier EXPERIENCE_BAR_BACKGROUND =
            Identifier.withDefaultNamespace("container/villager/experience_bar_background");
    private static final Identifier EXPERIENCE_BAR_CURRENT =
            Identifier.withDefaultNamespace("container/villager/experience_bar_current");

    private VillagerTradeScreenCommon() {
    }

    public static Component professionDisplayName(@Nullable VillagerData data) {
        if (data == null || data.profession() == VillagerProfession.NONE) {
            return Component.translatable("tooltip.trading_cells.unemployed");
        }
        if (data.profession() == VillagerProfession.NITWIT) {
            return Component.translatable("entity.minecraft.villager.nitwit");
        }
        return data.profession().unwrapKey()
                .map(key -> {
                    Identifier id = key.identifier();
                    return Component.translatable(
                            "entity." + id.getNamespace() + ".villager." + id.getPath()
                    );
                })
                .orElse(Component.translatable("gui.trading_cells.profession_title"));
    }

    public static Component levelName(int level) {
        return Component.translatable("merchant.level." + Math.max(1, level));
    }

    public static Component professionExperienceText(int level, int experience) {
        ProfessionProgress progress = professionProgress(level, experience);
        return progress.maximum()
                ? Component.translatable("gui.trading_cells.maximum")
                : Component.translatable(
                        "gui.trading_cells.profession_xp",
                        Math.max(0, experience),
                        progress.maximumExperience()
                );
    }

    public static ProfessionProgress professionProgress(int level, int experience) {
        int safeLevel = Math.max(1, level);
        int minimum = VillagerData.getMinXpPerLevel(safeLevel);
        int maximum = VillagerData.getMaxXpPerLevel(safeLevel);
        boolean atMaximum = !VillagerData.canLevelUp(safeLevel) || maximum <= minimum;
        float fraction = atMaximum
                ? 1.0F
                : Mth.clamp((experience - minimum) / (float) (maximum - minimum), 0.0F, 1.0F);
        return new ProfessionProgress(minimum, maximum, fraction, atMaximum);
    }

    public static void drawProfessionProgress(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int level,
            int experience
    ) {
        float progress = professionProgress(level, experience).fraction();
        int filled = Math.round(width * progress);
        graphics.blitSprite(
                RenderPipelines.GUI_TEXTURED,
                EXPERIENCE_BAR_BACKGROUND,
                x,
                y + 1,
                width,
                5
        );
        if (filled > 0) {
            graphics.blitSprite(
                    RenderPipelines.GUI_TEXTURED,
                    EXPERIENCE_BAR_CURRENT,
                    width,
                    5,
                    0,
                    0,
                    x,
                    y + 1,
                    Math.min(width, filled),
                    5
            );
        }
    }

    public static void drawStoredExperiencePanel(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            int orbX,
            int orbY
    ) {
        graphics.fill(x, y, x + width, y + height, 0xFF343A31);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFFA8B0A0);
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, 0xFF6C7468);
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, 0xFF6C7468);
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, 0xFFD2D7CC);
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, 0xFFD2D7CC);
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                EXPERIENCE_ORB_TEXTURE,
                orbX,
                orbY,
                32.0F,
                0.0F,
                16,
                16,
                16,
                16,
                64,
                64,
                0xFF9CFF45
        );
    }

    public static void drawStoredExperienceText(
            GuiGraphicsExtractor graphics,
            Font font,
            Component experienceText,
            Component levelText,
            int x,
            int experienceY,
            int levelY
    ) {
        drawScaledText(graphics, font, experienceText, x, experienceY);
        drawScaledText(graphics, font, levelText, x, levelY);
    }

    private static void drawScaledText(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int x,
            int y
    ) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(x, y);
        graphics.pose().scale(EXPERIENCE_TEXT_SCALE, EXPERIENCE_TEXT_SCALE);
        graphics.text(font, text, 0, 0, EXPERIENCE_TEXT_COLOR, true);
        graphics.pose().popMatrix();
    }

    public static void drawOfferCostA(
            GuiGraphicsExtractor graphics,
            Font font,
            MerchantOffer offer,
            int x,
            int y
    ) {
        ItemStack cost = offer.getCostA();
        ItemStack baseCost = offer.getBaseCostA();
        graphics.fakeItem(cost, x, y);
        if (cost.getCount() >= baseCost.getCount()) {
            graphics.itemDecorations(font, cost, x, y);
            return;
        }

        graphics.itemDecorations(font, cost, x, y, "");
        drawOldDiscountPrice(graphics, font, baseCost.getCount(), x, y);

        String discountedPrice = String.valueOf(cost.getCount());
        graphics.text(
                font,
                discountedPrice,
                x + 17 - font.width(discountedPrice),
                y + 9,
                EXPERIENCE_TEXT_COLOR,
                true
        );
    }

    private static void drawOldDiscountPrice(
            GuiGraphicsExtractor graphics,
            Font font,
            int originalPrice,
            int x,
            int y
    ) {
        String originalPriceText = String.valueOf(originalPrice);
        int originalPriceWidth = Math.max(3, font.width(originalPriceText));
        graphics.pose().pushMatrix();
        graphics.pose().translate(x + 1.0F, y);
        graphics.pose().scale(DISCOUNT_OLD_PRICE_SCALE, DISCOUNT_OLD_PRICE_SCALE);
        graphics.text(font, originalPriceText, 0, 0, DISCOUNT_OLD_PRICE_COLOR, true);
        graphics.fill(
                0,
                4,
                originalPriceWidth + 1,
                6,
                DISCOUNT_STRIKETHROUGH_COLOR
        );
        graphics.pose().popMatrix();
    }

    public static void drawTradeArrow(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            VillagerTradeSprites.State state
    ) {
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                VillagerTradeSprites.arrow(state),
                x,
                y,
                0,
                0,
                VillagerTradeSprites.ARROW_WIDTH,
                VillagerTradeSprites.ARROW_HEIGHT,
                VillagerTradeSprites.ARROW_WIDTH,
                VillagerTradeSprites.ARROW_HEIGHT
        );
    }

    public static void drawDropdownChevron(
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            boolean enabled,
            boolean hovered
    ) {
        int shadow = chevronShadow(enabled, hovered);
        int light = chevronLight(enabled, hovered);
        graphics.fill(x, y, x + 5, y + 1, light);
        graphics.fill(x + 1, y + 1, x + 4, y + 2, shadow);
        graphics.fill(x + 2, y + 2, x + 3, y + 3, shadow);
    }

    public static void drawBeveledButton( // NOSONAR - This drawing primitive needs explicit geometry and state.
            GuiGraphicsExtractor graphics,
            int x,
            int y,
            int width,
            int height,
            boolean active,
            boolean hovered,
            boolean pressed
    ) {
        ButtonColors colors = buttonColors(active, hovered, pressed);
        int outer = active ? 0xFF404040 : 0xFF5D5D5D;
        graphics.fill(x, y, x + width, y + height, outer);
        graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, colors.fill());
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, colors.light());
        graphics.fill(x + 1, y + 1, x + 2, y + height - 1, colors.light());
        graphics.fill(x + 1, y + height - 2, x + width - 1, y + height - 1, colors.shadow());
        graphics.fill(x + width - 2, y + 1, x + width - 1, y + height - 1, colors.shadow());
    }

    private static int chevronShadow(boolean enabled, boolean hovered) {
        if (!enabled) {
            return 0xFF8B8B8B;
        }
        return hovered ? 0xFF3F3F3F : 0xFF575757;
    }

    private static int chevronLight(boolean enabled, boolean hovered) {
        if (!enabled) {
            return 0xFFAAAAAA;
        }
        return hovered ? 0xFF686868 : 0xFF767676;
    }

    private static ButtonColors buttonColors(boolean active, boolean hovered, boolean pressed) {
        if (!active) {
            return new ButtonColors(0xFF929292, 0xFFA8A8A8, 0xFF747474);
        }
        if (pressed) {
            return new ButtonColors(0xFFB8B8B8, 0xFF888888, 0xFFE2E2E2);
        }
        int fill = hovered ? 0xFFE2E2E2 : 0xFFD0D0D0;
        return new ButtonColors(fill, 0xFFF0F0F0, 0xFF888888);
    }

    public static int buttonTextColor(boolean active) {
        return active ? TEXT_DARK : 0xFFE0E0E0;
    }

    public static void drawCenteredText(
            GuiGraphicsExtractor graphics,
            Font font,
            Component text,
            int centerX,
            int y,
            int color
    ) {
        graphics.text(font, text, centerX - font.width(text) / 2, y, color, false);
    }

    public record ProfessionProgress(
            int minimumExperience,
            int maximumExperience,
            float fraction,
            boolean maximum
    ) {
    }

    private record ButtonColors(int fill, int light, int shadow) {
    }
}
