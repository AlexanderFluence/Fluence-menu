package com.fluence.menu;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;

/** Convierte el ícono de cualquier app en una baldosa cuadrada de esquinas redondeadas. */
final class IconFactory {

    private static final int TILE_COLOR = 0xFF2C2C2E;
    private static final float RADIUS = 0.225f;

    private IconFactory() {}

    static Bitmap tile(Drawable d, int size) {
        Bitmap content = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(content);

        boolean drawn = false;
        if (d != null && Build.VERSION.SDK_INT >= 26) {
            drawn = drawAdaptive(d, c, size);
        }
        if (!drawn) {
            c.drawColor(TILE_COLOR);
            if (d != null) {
                int pad = Math.round(size * 0.14f);
                d.setBounds(pad, pad, size - pad, size - pad);
                d.draw(c);
            }
        }

        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas oc = new Canvas(out);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        p.setShader(new BitmapShader(content, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP));
        float r = size * RADIUS;
        oc.drawRoundRect(new RectF(0, 0, size, size), r, r, p);
        content.recycle();
        return out;
    }

    @TargetApi(26)
    private static boolean drawAdaptive(Drawable d, Canvas c, int size) {
        if (!(d instanceof AdaptiveIconDrawable)) return false;
        AdaptiveIconDrawable a = (AdaptiveIconDrawable) d;
        // Las capas adaptativas miden 108 y solo se ven las 72 centrales: se dibujan al 150 %.
        int ext = Math.round(size * 0.25f);
        Drawable bg = a.getBackground();
        Drawable fg = a.getForeground();
        if (bg != null) {
            bg.setBounds(-ext, -ext, size + ext, size + ext);
            bg.draw(c);
        } else {
            c.drawColor(0xFFFFFFFF);
        }
        if (fg != null) {
            fg.setBounds(-ext, -ext, size + ext, size + ext);
            fg.draw(c);
        }
        return true;
    }

    /** Ícono de cuadrícula 2×2 para el botón de inicio de la barra lateral. */
    static Drawable gridGlyph() {
        return new Drawable() {
            private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

            @Override
            public void draw(Canvas canvas) {
                paint.setColor(0xFFFFFFFF);
                float w = getBounds().width();
                float h = getBounds().height();
                float s = Math.min(w, h);
                float left = getBounds().left + (w - s) / 2f;
                float top = getBounds().top + (h - s) / 2f;
                float gap = s * 0.14f;
                float cell = (s - gap) / 2f;
                float r = cell * 0.28f;
                for (int i = 0; i < 2; i++) {
                    for (int j = 0; j < 2; j++) {
                        float x = left + j * (cell + gap);
                        float y = top + i * (cell + gap);
                        canvas.drawRoundRect(new RectF(x, y, x + cell, y + cell), r, r, paint);
                    }
                }
            }

            @Override
            public void setAlpha(int alpha) {
                paint.setAlpha(alpha);
            }

            @Override
            public void setColorFilter(ColorFilter cf) {
                paint.setColorFilter(cf);
            }

            @Override
            public int getOpacity() {
                return PixelFormat.TRANSLUCENT;
            }
        };
    }
}
