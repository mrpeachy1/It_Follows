package com.example.itfollows.avatar;

import static org.junit.Assert.*;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.test.mock.MockContentResolver;
import android.test.mock.MockContext;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

/**
 * Tests for {@link AvatarImageConverter} verifying palette index mapping and transparency.
 */
public class AvatarImporterTest {

    @Test
    public void convertsBitmapToPaletteIndicesAndTransparentPixels() throws Exception {
        // Prepare a 16x16 bitmap with a magenta border acting as background
        Bitmap bmp = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        int bg = Color.MAGENTA;
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                bmp.setPixel(x, y, bg);
            }
        }
        // Interior pixels for palette checks
        bmp.setPixel(1, 1, Color.BLACK);           // palette index 0
        bmp.setPixel(2, 1, Color.WHITE);           // palette index 1
        bmp.setPixel(1, 2, 0xFFE84B3C);            // palette index 3
        bmp.setPixel(2, 2, bg);                    // should become transparent (-1)

        // Encode bitmap to an in-memory stream
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] data = bos.toByteArray();

        // Stub resolver/context returning the bitmap stream
        MockContentResolver resolver = new MockContentResolver() {
            @Override
            public InputStream openInputStream(Uri uri) {
                return new ByteArrayInputStream(data);
            }
        };
        Context ctx = new MockContext() {
            @Override
            public ContentResolver getContentResolver() {
                return resolver;
            }
        };

        AvatarImageConverter.Options opts = new AvatarImageConverter.Options();
        opts.autoBackgroundToTransparent = true;
        opts.dither = false;

        AvatarConfig cfg = AvatarImageConverter.fromImage(ctx, Uri.parse("content://test/image"), new AvatarConfig(), opts);

        assertEquals(0, cfg.get(1, 1));  // black
        assertEquals(1, cfg.get(2, 1));  // white
        assertEquals(3, cfg.get(1, 2));  // red
        assertEquals(-1, cfg.get(2, 2)); // background -> transparent
    }
}

