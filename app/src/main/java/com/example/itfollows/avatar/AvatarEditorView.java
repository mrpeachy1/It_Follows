import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class AvatarEditorView extends View {
    private AvatarConfig cfg;
    private int currentColorIndex = 1; // default white
    private boolean drawGrid = true;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect r = new Rect();

    public AvatarEditorView(Context c, AttributeSet a){ super(c,a); }

    public void setConfig(AvatarConfig cfg){
        this.cfg = cfg;
        invalidate();
    }

    public void setCurrentColorIndex(int idx){
        this.currentColorIndex = idx;
    }

    public void setDrawGrid(boolean on){ this.drawGrid = on; invalidate(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (cfg == null) return;

        int w = getWidth(), h = getHeight();
        int px = AvatarConfig.WIDTH, py = AvatarConfig.HEIGHT;
        int cell = Math.min(w/px, h/py);
        int bmpW = cell*px, bmpH = cell*py;
        int left = (w - bmpW)/2, top = (h - bmpH)/2;

        canvas.drawColor(0xFF1E1E1E);

        // Pixels
        for(int y=0;y<py;y++){
            for(int x=0;x<px;x++){
                int idx = cfg.get(x,y);
                if (idx >= 0 && idx < cfg.palette.length) {
                    p.setColor(cfg.palette[idx]);
                } else {
                    p.setColor(0x00000000);
                }
                r.set(left + x*cell, top + y*cell, left + (x+1)*cell, top + (y+1)*cell);
                canvas.drawRect(r, p);
            }
        }

        // Grid
        if (drawGrid) {
            Paint grid = new Paint();
            grid.setColor(0x33FFFFFF);
            grid.setStrokeWidth(1f);
            for (int gx=0; gx<=px; gx++){
                int x = left + gx*cell;
                canvas.drawLine(x, top, x, top + bmpH, grid);
            }
            for (int gy=0; gy<=py; gy++){
                int y = top + gy*cell;
                canvas.drawLine(left, y, left + bmpW, y, grid);
            }
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        if (cfg == null) return true;

        int action = e.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_POINTER_DOWN) {
            paintAt(e.getX(), e.getY(), false);
            return true;
        } else if (action == MotionEvent.ACTION_LONG_PRESS || action == MotionEvent.ACTION_BUTTON_PRESS) {
            paintAt(e.getX(), e.getY(), true); // eyedrop
            return true;
        }
        return super.onTouchEvent(e);
    }

    private void paintAt(float sx, float sy, boolean eyeDrop) {
        int w = getWidth(), h = getHeight();
        int px = AvatarConfig.WIDTH, py = AvatarConfig.HEIGHT;
        int cell = Math.min(w/px, h/py);
        int bmpW = cell*px, bmpH = cell*py;
        int left = (w - bmpW)/2, top = (h - bmpH)/2;

        if (sx < left || sy < top || sx >= left + bmpW || sy >= top + bmpH) return;

        int x = (int)((sx - left) / cell);
        int y = (int)((sy - top) / cell);

        if (eyeDrop) {
            int idx = cfg.get(x,y);
            if (idx >= 0) currentColorIndex = idx;
        } else {
            cfg.set(x, y, currentColorIndex);
            invalidate();
        }
    }
}
