package com.example.itfollows.avatar;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;

/**
 * Utility class for importing arbitrary photos into avatar-sized bitmaps.
 */
public final class AvatarImporter {
    private AvatarImporter(){}

    /**
     * Creates an avatar-sized {@link Bitmap} from an arbitrary source image.
     * <p>
     * The source is first placed, without scaling, at the center of a new
     * transparent square canvas whose side equals the larger of the source's
     * width or height. This pads the shorter dimension with transparency so the
     * entire image is preserved. The padded square is then scaled to
     * {@link AvatarConfig#WIDTH}×{@link AvatarConfig#HEIGHT} using bilinear
     * filtering.
     *
     * @param src source bitmap; caller retains ownership
     * @return a new bitmap of size {@code AvatarConfig.WIDTH × AvatarConfig.HEIGHT}
     *         or {@code null} if {@code src} is {@code null}
     */
    public static Bitmap importPhoto(Bitmap src) {
        if (src == null) return null;
        int w = src.getWidth();
        int h = src.getHeight();
        int size = Math.max(w, h);

        Bitmap square = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        square.eraseColor(Color.TRANSPARENT);
        Canvas c = new Canvas(square);
        c.drawBitmap(src, (size - w) / 2f, (size - h) / 2f, null);

        Bitmap out = Bitmap.createScaledBitmap(square, AvatarConfig.WIDTH, AvatarConfig.HEIGHT, true);
        square.recycle();
        return out;
    }
}
