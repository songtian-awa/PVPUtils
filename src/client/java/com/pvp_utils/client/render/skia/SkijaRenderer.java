package com.pvp_utils.client.render.skia;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.types.RRect;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;

/**
 * 插件系统渲染适配层：把官方插件的 SkijaRenderer 调用映射到本地
 * SkiaBlurRenderer 与 skija Canvas 之上。
 */
public final class SkijaRenderer {
    private SkijaRenderer() {
    }

    public static void drawBlurredBackdrop(Canvas canvas, RRect rrect, float x, float y, float w, float h, float effect) {
        SkiaBlurRenderer blur = SkiaBlurRenderer.getInstance();
        try {
            int fb = SkiaBlurRenderer.currentDrawFramebufferId();
            io.github.humbleui.skija.DirectContext ctx = blur.context();
            float radius = rrect._radii.length > 0 ? rrect._radii[0] : 12f;
            if (fb != 0 && ctx != null && blur.render(canvas, ctx, Minecraft.getInstance(), fb,
                    x, y, w, h, radius, 0x24000000, Math.max(0f, effect))) {
                return;
            }
        } catch (Throwable ignored) {
        }
        try (Paint paint = new Paint().setAntiAlias(true).setColor(0x32000000)) {
            canvas.drawRRect(rrect, paint);
        }
    }

    public static BorrowedImage borrowTexture(Identifier id) {
        if (id == null) return null;
        try {
            Resource res = Minecraft.getInstance().getResourceManager().getResource(id).orElse(null);
            if (res == null) return null;
            byte[] data;
            try (InputStream in = res.open()) {
                data = in.readAllBytes();
            }
            Image image = Image.makeFromEncoded(data);
            if (image == null) return null;
            return new BorrowedImage(image);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static final class BorrowedImage implements AutoCloseable {
        private final Image image;

        BorrowedImage(Image image) {
            this.image = image;
        }

        public Image image() {
            return image;
        }

        @Override
        public void close() {
            image.close();
        }
    }
}
