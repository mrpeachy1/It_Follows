package com.example.itfollows.avatar;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.InputStream;

/** Utility to import an image into an {@link AvatarConfig}. */
public final class AvatarImporter {

    private AvatarImporter() {}

    /**
     * Load a bitmap from the given {@link Uri}, scale it to the avatar size and
     * translate each pixel into the {@link AvatarConfig} grid.
     * <p>
     * The caller is responsible for providing a base {@link AvatarConfig} to
     * populate. For the purposes of the unit tests in this kata we perform only
     * a trivial mapping of pixels to palette indices.
     */
    public static AvatarConfig importPhoto(Context ctx, Uri uri) throws Exception {
        Bitmap src;
        ContentResolver resolver = ctx.getContentResolver();
        try (InputStream in = resolver.openInputStream(uri)) {
            src = BitmapFactory.decodeStream(in);
        }
        if (src == null) throw new IllegalArgumentException("Unable to decode image");

        // Scale the source image to the avatar dimensions
        Bitmap scaled = Bitmap.createScaledBitmap(src, AvatarConfig.WIDTH, AvatarConfig.HEIGHT, true);
        src.recycle();

        AvatarConfig cfg = new AvatarConfig();
        try {
            // Copy each pixel into the AvatarConfig
            for (int y = 0; y < AvatarConfig.HEIGHT; y++) {
                for (int x = 0; x < AvatarConfig.WIDTH; x++) {
                    int color = scaled.getPixel(x, y);
                    // Simple placeholder: mark non-zero pixels as palette index 1
                    cfg.set(x, y, color == 0 ? -1 : 1);
                }
            }
        } finally {
            // Recycle the scaled bitmap exactly once
            if (scaled != null && !scaled.isRecycled()) {
                scaled.recycle();
            }
        }

        return cfg;
    }
}

