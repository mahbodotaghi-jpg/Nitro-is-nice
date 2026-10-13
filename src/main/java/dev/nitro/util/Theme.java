package dev.nitro.util;

import java.awt.Color;

public class Theme {
    /** Animated pink -> magenta accent colour (ARGB). */
    public static int accent(long timeMs, int offset) {
        float h = 0.89f + 0.05f * (float) Math.sin((timeMs + offset * 40L) / 600.0);
        return Color.HSBtoRGB(h, 0.55f, 1f);
    }

    public static int withAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }
}
