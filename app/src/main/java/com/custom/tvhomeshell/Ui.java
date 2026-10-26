package com.custom.tvhomeshell;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;

/** Мелкие утилиты: dp, скругления, цвет из иконки. */
public final class Ui {

    private Ui() {
    }

    public static int dp(Context c, float v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                c.getResources().getDisplayMetrics()));
    }

    public static GradientDrawable round(int fill, int radiusPx) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(fill);
        d.setCornerRadius(radiusPx);
        return d;
    }

    public static GradientDrawable roundStroke(int fill, int stroke, int strokePx, int radiusPx) {
        GradientDrawable d = round(fill, radiusPx);
        d.setStroke(strokePx, stroke);
        return d;
    }

    public static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /** Доминирующий насыщенный цвет иконки; при неудаче — нейтральный синий. */
    public static int dominant(Drawable d) {
        try {
            Bitmap bmp = toBitmap(d, 32);
            long r = 0, g = 0, b = 0;
            int n = 0;
            for (int y = 0; y < bmp.getHeight(); y += 2) {
                for (int x = 0; x < bmp.getWidth(); x += 2) {
                    int p = bmp.getPixel(x, y);
                    if (Color.alpha(p) < 200) {
                        continue;
                    }
                    int rr = Color.red(p);
                    int gg = Color.green(p);
                    int bb = Color.blue(p);
                    int mx = Math.max(rr, Math.max(gg, bb));
                    int mn = Math.min(rr, Math.min(gg, bb));
                    if (mx - mn < 30 || mx < 45) {
                        continue;
                    }
                    r += rr;
                    g += gg;
                    b += bb;
                    n++;
                }
            }
            if (n == 0) {
                return 0xFF5B6CFF;
            }
            return Color.rgb((int) (r / n), (int) (g / n), (int) (b / n));
        } catch (Exception e) {
            return 0xFF5B6CFF;
        }
    }

    private static Bitmap toBitmap(Drawable d, int size) {
        if (d instanceof BitmapDrawable && ((BitmapDrawable) d).getBitmap() != null) {
            return Bitmap.createScaledBitmap(((BitmapDrawable) d).getBitmap(), size, size, true);
        }
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        d.setBounds(0, 0, size, size);
        d.draw(c);
        return bmp;
    }
}
