package com.cosmocraft.trading_cells.platform.neoforge.event;

/** Pure color curve used for enchantment levels above their normal maximum. */
public final class HighLevelEnchantmentPalette {
    private static final int FIXED_BLUE_THROUGH_LEVEL = 10;
    private static final float START_HUE_DEGREES = 220.0F;
    private static final float HUE_TRAVEL_DEGREES = 280.0F;
    public static final int FIXED_BLUE = 0x5599FF;

    private HighLevelEnchantmentPalette() {
    }

    public static int colorFor(int level, int supportedMaximum) {
        if (level <= FIXED_BLUE_THROUGH_LEVEL || supportedMaximum <= FIXED_BLUE_THROUGH_LEVEL) {
            return FIXED_BLUE;
        }
        float progress = (Math.clamp(level, FIXED_BLUE_THROUGH_LEVEL, supportedMaximum)
                - FIXED_BLUE_THROUGH_LEVEL) / (float) (supportedMaximum - FIXED_BLUE_THROUGH_LEVEL);
        float hueDegrees = START_HUE_DEGREES - HUE_TRAVEL_DEGREES * progress;
        float normalizedHue = Math.floorMod(Math.round(hueDegrees * 1_000.0F), 360_000) / 360_000.0F;
        return hsvToRgb(normalizedHue, 0.72F, 1.0F);
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        float scaledHue = hue * 6.0F;
        int sector = (int) Math.floor(scaledHue);
        float fraction = scaledHue - sector;
        float low = value * (1.0F - saturation);
        float descending = value * (1.0F - fraction * saturation);
        float ascending = value * (1.0F - (1.0F - fraction) * saturation);
        float red;
        float green;
        float blue;
        switch (Math.floorMod(sector, 6)) {
            case 0 -> {
                red = value;
                green = ascending;
                blue = low;
            }
            case 1 -> {
                red = descending;
                green = value;
                blue = low;
            }
            case 2 -> {
                red = low;
                green = value;
                blue = ascending;
            }
            case 3 -> {
                red = low;
                green = descending;
                blue = value;
            }
            case 4 -> {
                red = ascending;
                green = low;
                blue = value;
            }
            default -> {
                red = value;
                green = low;
                blue = descending;
            }
        }
        return channel(red) << 16 | channel(green) << 8 | channel(blue);
    }

    private static int channel(float value) {
        return Math.clamp(Math.round(value * 255.0F), 0, 255);
    }
}
