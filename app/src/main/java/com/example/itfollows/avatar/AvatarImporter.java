package com.example.itfollows.avatar;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

/**
 * Utility for importing a photo for use with {@link AvatarConfig}.
 * <p>
 * Non-square photos are centered inside a square canvas before being scaled to
 * {@link AvatarConfig#WIDTH} × {@link AvatarConfig#HEIGHT}. The longer dimension is
 * cropped around the center and any remaining area is padded with transparency to
 * avoid aspect ratio distortion.
 * </p>
 */
public final class AvatarImporter {
    private AvatarImporter() {}

    /**
     * Convert the provided bitmap into a square avatar-sized bitmap.
     *
     * @param src source bitmap; not modified
     * @return a {@link AvatarConfig#WIDTH} × {@link AvatarConfig#HEIGHT} bitmap
     */
    public static Bitmap importPhoto(Bitmap src) {
        if (src == null) return null;

        Bitmap square = cropOrPadToSquare(src);
        Bitmap scaled = Bitmap.createScaledBitmap(square,
                AvatarConfig.WIDTH, AvatarConfig.HEIGHT, true);
        if (square != src) {
            square.recycle();
        }
        return scaled;
    }

    /** Center-crop or pad a bitmap to a square, using transparency for padding. */
    private static Bitmap cropOrPadToSquare(Bitmap src) {
        int w = src.getWidth();
        int h = src.getHeight();
        if (w == h) return src;

        int size = Math.max(w, h);
        Bitmap out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(out);
        canvas.drawColor(Color.TRANSPARENT);

        int left = (size - w) / 2;
        int top = (size - h) / 2;
        canvas.drawBitmap(src, left, top, null);

        return out;
    }
}

