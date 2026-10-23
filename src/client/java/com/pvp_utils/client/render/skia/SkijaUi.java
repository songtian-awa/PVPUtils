package com.pvp_utils.client.render.skia;

import com.pvp_utils.client.render.font.FontRenderer;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.types.Point;
import io.github.humbleui.types.Rect;
import io.github.humbleui.types.RRect;

/**
 * 插件系统渲染适配层：把官方插件的 SkijaUi 绘制调用映射到本地的
 * FontRenderer 与 skija Canvas 之上，保证插件无需依赖独立的 Skija UI 实现。
 */
public final class SkijaUi {
    private SkijaUi() {
    }

    public enum IconSet {
        PVP_CONTROLS,
        MATERIAL_SYMBOLS
    }

    public static final class TextMetrics {
        private final float ascent;
        private final float descent;

        TextMetrics(float ascent, float descent) {
            this.ascent = ascent;
            this.descent = descent;
        }

        public float getAscent() {
            return ascent;
        }

        public float getDescent() {
            return descent;
        }
    }

    public static float textWidthWithFallback(String text, float size) {
        return FontRenderer.measureTextWidth(text == null ? "" : text, size);
    }

    public static float textWidthWithFallback(String text, float size, String font) {
        return FontRenderer.measureTextWidth(text == null ? "" : text, size, font);
    }

    public static TextMetrics textMetrics(float size) {
        float ascent = FontRenderer.getAscent(size);
        return new TextMetrics(ascent, FontRenderer.getLineHeight(size) - ascent);
    }

    public static void textWithFallback(Canvas canvas, String text, float x, float y, float height, int color, float size) {
        drawText(canvas, text, x, y, height, color, size, FontRenderer.DEFAULT);
    }

    public static void textWithFallback(Canvas canvas, String text, float x, float y, float height, int color, float size, String font) {
        drawText(canvas, text, x, y, height, color, size, font);
    }

    public static void textShadowWithFallback(Canvas canvas, String text, float x, float y, float height, int color, float size) {
        drawText(canvas, text, x + 1f, y + 1f, height, 0xCC000000, size, FontRenderer.DEFAULT);
        drawText(canvas, text, x, y, height, color, size, FontRenderer.DEFAULT);
    }

    public static void textShadowWithFallback(Canvas canvas, String text, float x, float y, float height, int color, float size, String font) {
        drawText(canvas, text, x + 1f, y + 1f, height, 0xCC000000, size, font);
        drawText(canvas, text, x, y, height, color, size, font);
    }

    private static void drawText(Canvas canvas, String text, float x, float y, float height, int color, float size, String font) {
        float lineHeight = FontRenderer.getLineHeight(size, font);
        float ascent = FontRenderer.getAscent(size, font);
        float baseline = y + (height - lineHeight) * 0.5f + ascent;
        FontRenderer.drawText(canvas, text == null ? "" : text, x, baseline, size, color, font);
    }

    public static void fill(Canvas canvas, float x, float y, float w, float h, int color) {
        if (w <= 0f || h <= 0f) return;
        try (Paint paint = new Paint().setAntiAlias(true).setColor(color)) {
            canvas.drawRect(Rect.makeXYWH(x, y, w, h), paint);
        }
    }

    public static void rounded(Canvas canvas, float x, float y, float w, float h, float radius, int color) {
        if (w <= 0f || h <= 0f) return;
        try (Paint paint = new Paint().setAntiAlias(true).setColor(color)) {
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, radius), paint);
        }
    }

    public static void line(Canvas canvas, float x1, float y1, float x2, float y2, float thickness, int color) {
        try (Paint paint = new Paint().setAntiAlias(true).setColor(color).setStrokeWidth(Math.max(1f, thickness))) {
            canvas.drawLine(x1, y1, x2, y2, paint);
        }
    }

    public static void outline(Canvas canvas, float x, float y, float w, float h, float radius, float thickness, int color) {
        try (Paint paint = new Paint().setAntiAlias(true).setColor(color).setStrokeWidth(Math.max(1f, thickness))) {
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, radius), paint);
        }
    }

    public static void gradient(Canvas canvas, float x, float y, float w, float h, int startColor, int endColor, boolean horizontal, float angle) {
        if (w <= 0f || h <= 0f) return;
        Point start = new Point(x, y);
        Point end = horizontal ? new Point(x + w, y) : new Point(x, y + h);
        try (Shader shader = Shader.makeLinearGradient(start, end, new int[]{startColor, endColor}, null);
             Paint paint = new Paint().setShader(shader)) {
            canvas.drawRect(Rect.makeXYWH(x, y, w, h), paint);
        }
    }

    public static void gradientDiagonal(Canvas canvas, float x, float y, float w, float h, int startColor, int endColor, float angle) {
        if (w <= 0f || h <= 0f) return;
        try (Shader shader = Shader.makeLinearGradient(new Point(x, y), new Point(x + w, y + h), new int[]{startColor, endColor}, null);
             Paint paint = new Paint().setShader(shader)) {
            canvas.drawRect(Rect.makeXYWH(x, y, w, h), paint);
        }
    }

    public static void dropShadowRounded(Canvas canvas, float x, float y, float w, float h, float radius, float offset, float effect, int color) {
        int shadowAlpha = (color >>> 24) & 0xFF;
        int base = (int) Math.min(255, shadowAlpha * 0.45f);
        for (int i = 1; i <= 3; i++) {
            int alpha = Math.max(1, base * (4 - i) / 4);
            try (Paint paint = new Paint().setAntiAlias(true).setColor((alpha << 24) | (color & 0xFFFFFF))) {
                canvas.drawRRect(RRect.makeXYWH(x + offset, y + offset + i, w, h, radius), paint);
            }
        }
        try (Paint paint = new Paint().setAntiAlias(true).setColor(color)) {
            canvas.drawRRect(RRect.makeXYWH(x, y, w, h, radius), paint);
        }
    }

    public static void icon(Canvas canvas, String icon, float x, float y, float size, int color, float fontSize, IconSet set) {
        String font = set == IconSet.PVP_CONTROLS ? FontRenderer.ICON : FontRenderer.MATERIAL_SYMBOLS;
        FontRenderer.drawText(canvas, icon == null ? "" : icon, x, y + FontRenderer.getAscent(fontSize, font), fontSize, color, font);
    }
}
