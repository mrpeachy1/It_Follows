package com.example.itfollows.avatar;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;

import java.io.InputStream;
import java.util.Arrays;

/**
 * Utilities to convert an arbitrary image into a 16x16, palette-quantized AvatarConfig.
 * Steps:
 * 1) Load -> center-crop square -> scale to 16x16 (bilinear)
 * 2) Optional background removal: detect dominant border color and mark near-match pixels transparent
 * 3) Optional Floyd–Steinberg dithering
 * 4) Quantize each pixel to nearest color in avatar palette, write palette index to AvatarConfig
 */
public final class AvatarImageConverter {

    public static class Options {
        public boolean autoBackgroundToTransparent = true;
        /** 0..255 approx color distance tolerance used for background keying */
        public int backgroundTolerance = 40;
        /** Apply Floyd–Steinberg dithering in RGB before final quantization */
        public boolean dither = true;
    }

    private AvatarImageConverter(){}

    /** High-level helper: from image Uri to AvatarConfig, using cfg.palette */
    public static AvatarConfig fromImage(Context ctx, Uri uri, AvatarConfig basePaletteSource, Options opts) throws Exception {
        if (opts == null) opts = new Options();
        Bitmap src = loadBitmap(ctx, uri);
        if (src == null) throw new IllegalArgumentException("Could not decode image");

        Bitmap square = centerCropSquare(src);
        if (square != src) src.recycle();

        Bitmap small = Bitmap.createScaledBitmap(square, AvatarConfig.WIDTH, AvatarConfig.HEIGHT, true /*bilinear*/);
        square.recycle();

        // Prepare working float buffers for optional dithering
        int w = small.getWidth(), h = small.getHeight();
        float[][] rf = new float[h][w];
        float[][] gf = new float[h][w];
        float[][] bf = new float[h][w];

        for (int y=0; y<h; y++) {
            for (int x=0; x<w; x++) {
                int c = small.getPixel(x,y);
                rf[y][x] = (float) Color.red(c);
                gf[y][x] = (float) Color.green(c);
                bf[y][x] = (float) Color.blue(c);
            }
        }

        // Detect dominant border color if requested
        int[] bgRGB = null;
        if (opts.autoBackgroundToTransparent) {
            bgRGB = estimateBorderColor(small);
        }

        if (opts.dither) {
            // Simple Floyd–Steinberg dithering toward the palette
            // We'll diffuse error in RGB space, while snapping to nearest palette at each step for better visual fidelity.
            for (int y=0; y<h; y++) {
                for (int x=0; x<w; x++) {
                    int rr = clampToByte(Math.round(rf[y][x]));
                    int gg = clampToByte(Math.round(gf[y][x]));
                    int bb = clampToByte(Math.round(bf[y][x]));

                    // Background check (use original RGB running values)
                    if (bgRGB != null && colorNear(rr, gg, bb, bgRGB[0], bgRGB[1], bgRGB[2], opts.backgroundTolerance)) {
                        // Treat as transparent; propagate zero error (or small bias to avoid halos)
                        quantizeDiffuse(rf, gf, bf, x, y, w, h, rr, gg, bb, rr, gg, bb);
                        continue;
                    }

                    int nearest = nearestPaletteColor(rr, gg, bb, basePaletteSource.palette);
                    int nr = Color.red(nearest), ng = Color.green(nearest), nb = Color.blue(nearest);

                    // Diffuse error
                    quantizeDiffuse(rf, gf, bf, x, y, w, h, rr, gg, bb, nr, ng, nb);
                }
            }
        }

        // Build AvatarConfig
        AvatarConfig out = new AvatarConfig();
        // Keep the same palette as provided
        out.palette = Arrays.copyOf(basePaletteSource.palette, basePaletteSource.palette.length);

        for (int y=0; y<h; y++) {
            for (int x=0; x<w; x++) {
                int rr = clampToByte(Math.round(rf[y][x]));
                int gg = clampToByte(Math.round(gf[y][x]));
                int bb = clampToByte(Math.round(bf[y][x]));

                // If background enabled AND near background color -> transparent
                if (bgRGB != null && colorNear(rr, gg, bb, bgRGB[0], bgRGB[1], bgRGB[2], opts.backgroundTolerance)) {
                    out.set(x, y, -1);
                } else {
                    // Snap to nearest palette color and store index
                    int idx = nearestPaletteIndex(rr, gg, bb, out.palette);
                    out.set(x, y, idx);
                }
            }
        }

        small.recycle();
        return out;
    }

    // ---------- helpers ----------

    private static Bitmap loadBitmap(Context ctx, Uri uri) throws Exception {
        ContentResolver cr = ctx.getContentResolver();
        try (InputStream in = cr.openInputStream(uri)) {
            if (in == null) return null;
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            return BitmapFactory.decodeStream(in, null, opts);
        }
    }

    private static Bitmap centerCropSquare(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        if (w == h) return src;
        int size = Math.min(w, h);
        int x = (w - size) / 2;
        int y = (h - size) / 2;
        return Bitmap.createBitmap(src, x, y, size, size);
    }

    /** Estimate dominant border color (mean of outer ring). */
    private static int[] estimateBorderColor(Bitmap b) {
        int w = b.getWidth(), h = b.getHeight();
        long sr=0, sg=0, sb=0; int count=0;

        // top & bottom rows
        for (int x=0;x<w;x++){
            int ct = b.getPixel(x,0);
            int cb = b.getPixel(x,h-1);
            sr += Color.red(ct) + Color.red(cb);
            sg += Color.green(ct) + Color.green(cb);
            sb += Color.blue(ct) + Color.blue(cb);
            count += 2;
        }
        // left & right cols (skip corners already counted)
        for (int y=1;y<h-1;y++){
            int cl = b.getPixel(0,y);
            int cr = b.getPixel(w-1,y);
            sr += Color.red(cl) + Color.red(cr);
            sg += Color.green(cl) + Color.green(cr);
            sb += Color.blue(cl) + Color.blue(cr);
            count += 2;
        }
        int r = (int)(sr / Math.max(1, count));
        int g = (int)(sg / Math.max(1, count));
        int bl= (int)(sb / Math.max(1, count));
        return new int[]{r,g,bl};
    }

    private static int clampToByte(int v){ return Math.max(0, Math.min(255, v)); }

    private static boolean colorNear(int r1,int g1,int b1,int r2,int g2,int b2,int tol){
        int dr=r1-r2, dg=g1-g2, db=b1-b2;
        // Euclidean distance threshold in RGB
        return (dr*dr + dg*dg + db*db) <= (tol*tol);
    }

    private static int nearestPaletteColor(int r,int g,int b,int[] palette){
        int best=palette[0], bestD=Integer.MAX_VALUE;
        for(int c: palette){
            int dr=r-Color.red(c), dg=g-Color.green(c), db=b-Color.blue(c);
            int d = dr*dr + dg*dg + db*db;
            if (d < bestD){ bestD = d; best = c; }
        }
        return best;
    }

    private static int nearestPaletteIndex(int r,int g,int b,int[] palette){
        int bestIdx=0, bestD=Integer.MAX_VALUE;
        for(int i=0;i<palette.length;i++){
            int c = palette[i];
            int dr=r-Color.red(c), dg=g-Color.green(c), db=b-Color.blue(c);
            int d = dr*dr + dg*dg + db*db;
            if (d < bestD){ bestD = d; bestIdx = i; }
        }
        return bestIdx;
    }

    /** Apply F–S error diffusion to neighbors (x+1,y), (x-1,y+1), (x,y+1), (x+1,y+1). */
    private static void quantizeDiffuse(float[][] rf, float[][] gf, float[][] bf,
                                        int x,int y,int w,int h,
                                        int srcR,int srcG,int srcB,
                                        int qR,int qG,int qB) {
        float er = srcR - qR;
        float eg = srcG - qG;
        float eb = srcB - qB;

        // x+1, y     (7/16)
        if (x+1 < w){
            rf[y][x+1] += er * (7f/16f);
            gf[y][x+1] += eg * (7f/16f);
            bf[y][x+1] += eb * (7f/16f);
        }
        // x-1, y+1   (3/16)
        if (x-1 >= 0 && y+1 < h){
            rf[y+1][x-1] += er * (3f/16f);
            gf[y+1][x-1] += eg * (3f/16f);
            bf[y+1][x-1] += eb * (3f/16f);
        }
        // x, y+1     (5/16)
        if (y+1 < h){
            rf[y+1][x] += er * (5f/16f);
            gf[y+1][x] += eg * (5f/16f);
            bf[y+1][x] += eb * (5f/16f);
        }
        // x+1, y+1   (1/16)
        if (x+1 < w && y+1 < h){
            rf[y+1][x+1] += er * (1f/16f);
            gf[y+1][x+1] += eg * (1f/16f);
            bf[y+1][x+1] += eb * (1f/16f);
        }
        // overwrite this pixel with quantized color
        rf[y][x] = qR; gf[y][x] = qG; bf[y][x] = qB;
    }
}
