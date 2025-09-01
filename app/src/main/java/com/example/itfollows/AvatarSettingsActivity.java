import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AvatarSettingsActivity extends AppCompatActivity {
    private AvatarConfig cfg;
    private AvatarEditorView editor;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_settings);

        cfg = AvatarStorage.load(this);

        editor = findViewById(R.id.avatarEditor);
        editor.setConfig(cfg);

        LinearLayout paletteRow = findViewById(R.id.paletteRow);
        inflatePaletteButtons(paletteRow);

        Button btnClear = findViewById(R.id.btnClear);
        Button btnRandom = findViewById(R.id.btnRandom);
        Button btnSave = findViewById(R.id.btnSave);

        btnClear.setOnClickListener(v -> {
            for (int i=0;i<cfg.pixels.length;i++) cfg.pixels[i] = -1;
            editor.invalidate();
        });

        btnRandom.setOnClickListener(v -> {
            java.util.Random rand = new java.util.Random();
            for (int y=0;y<AvatarConfig.HEIGHT;y++){
                for (int x=0;x<AvatarConfig.WIDTH;x++){
                    int r = rand.nextInt(10);
                    cfg.set(x,y, r<2 ? -1 : rand.nextInt(cfg.palette.length)); // sparse
                }
            }
            editor.invalidate();
        });

        btnSave.setOnClickListener(v -> {
            AvatarStorage.save(this, cfg);
            // Optionally, stash a pre-rendered PNG size for map marker speed:
            // Bitmap b = AvatarRenderer.render(cfg, 128, false, true);
            // ...save if you like...
            finish();
        });
    }

    private void inflatePaletteButtons(LinearLayout row) {
        for (int i=0; i<cfg.palette.length; i++){
            final int idx = i;
            View swatch = new View(this);
            int size = (int)(getResources().getDisplayMetrics().density * 44);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(8,8,8,8);
            swatch.setLayoutParams(lp);
            swatch.setBackgroundColor(cfg.palette[i]);
            swatch.setOnClickListener(v -> editor.setCurrentColorIndex(idx));
            row.addView(swatch);
        }
    }
}
