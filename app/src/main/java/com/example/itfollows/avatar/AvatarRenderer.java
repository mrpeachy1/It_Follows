package com.example.itfollows.avatar;

import android.graphics.*;

public class AvatarRenderer {

    /**
     * Renders the 16x16 avatar to a Bitmap at requested pixel size (e.g., 128 or 192).
     * Nearest-neighbor, optional outline and grid (grid off by default for marker).
     */
    public static Bitmap render(AvatarConfig cfg, int outSize, boolean drawGrid, boolean drawOutline) {
        int w = AvatarConfig.WIDTH, h = AvatarConfig.HEIGHT;
        int scale = Math.max(1, outSize / Math.max(w, h));
        int bmpW = w * scale, bmpH = h * scale;

        Bitmap bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setFilterBitmap(false);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        Rect r = new Rect();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = cfg.get(x, y);
                if (idx >= 0 && idx < cfg.palette.length) {
                    p.setColor(cfg.palette[idx]);
                    r.set(x * scale, y * scale, (x + 1) * scale, (y + 1) * scale);
                    canvas.drawRect(r, p);
                }
            }
        }

        if (drawOutline) draw1pxOutline(canvas, cfg, scale);

        if (drawGrid) {
            Paint grid = new Paint();
            grid.setColor(0x22FFFFFF);
            grid.setStrokeWidth(1f);
            for (int gx = 1; gx < w; gx++) canvas.drawLine(gx * scale, 0, gx * scale, bmpH, grid);
            for (int gy = 1; gy < h; gy++) canvas.drawLine(0, gy * scale, bmpW, gy * scale, grid);
        }

        return bmp;
    }

    private static void draw1pxOutline(Canvas c, AvatarConfig cfg, int s){
        int w = AvatarConfig.WIDTH, h = AvatarConfig.HEIGHT;
        Paint outline = new Paint();
        outline.setColor(0xFF1A1A1A);
        outline.setStrokeWidth(1f);
        for(int y=0;y<h;y++){
            for(int x=0;x<w;x++){
                int idx = cfg.get(x,y);
                if (idx < 0) continue;
                if (cfg.get(x-1,y) < 0) c.drawLine(x*s, y*s, x*s, (y+1)*s, outline);
                if (cfg.get(x+1,y) < 0) c.drawLine((x+1)*s, y*s, (x+1)*s, (y+1)*s, outline);
                if (cfg.get(x,y-1) < 0) c.drawLine(x*s, y*s, (x+1)*s, y*s, outline);
                if (cfg.get(x,y+1) < 0) c.drawLine(x*s, (y+1)*s, (x+1)*s, (y+1)*s, outline);
            }
        }
    }
}
