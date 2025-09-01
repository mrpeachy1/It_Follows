package com.example.itfollows.avatar;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.itfollows.R;

public class AvatarSettingsActivity extends AppCompatActivity {
    private AvatarConfig cfg;
    private AvatarEditorView editor;

    private ActivityResultLauncher<String[]> openImageLauncher;

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_avatar_settings);

        cfg = AvatarStorage.load(this);

        editor = findViewById(R.id.avatarEditor);
        editor.setConfig(cfg);

        LinearLayout paletteRow = findViewById(R.id.paletteRow);
        inflatePaletteButtons(paletteRow);

        Button btnUseDefault = findViewById(R.id.btnUseDefault);
        Button btnImportPhoto = findViewById(R.id.btnImportPhoto);
        Button btnClear = findViewById(R.id.btnClear);
        Button btnRandom = findViewById(R.id.btnRandom);
        Button btnSave = findViewById(R.id.btnSave);

        btnUseDefault.setOnClickListener(v -> {
            cfg = AvatarPresets.defaultPreset();
            editor.setConfig(cfg);
            editor.invalidate();
        });

        // Register the image picker
        openImageLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    if (uri != null) {
                        // Persist read permission so we can decode again if needed
                        final int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        try {
                            getContentResolver().takePersistableUriPermission(uri, flags);
                        } catch (Exception ignored) {}
                        importUriToAvatar(uri);
                    }
                });

        btnImportPhoto.setOnClickListener(v -> {
            // MIME filter for images only
            openImageLauncher.launch(new String[]{"image/*"});
        });

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
            Toast.makeText(this, "Avatar saved", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void importUriToAvatar(Uri uri){
        try {
            AvatarImageConverter.Options opts = new AvatarImageConverter.Options();
            opts.autoBackgroundToTransparent = true; // turn border background into transparency
            opts.backgroundTolerance = 44;           // tweak if halos appear
            opts.dither = true;                      // nicer gradients

            AvatarConfig converted = AvatarImageConverter.fromImage(this, uri, cfg, opts);
            if (converted != null) {
                cfg = converted;
                editor.setConfig(cfg);
                editor.invalidate();
                Toast.makeText(this, "Imported photo → 8-bit avatar", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Import failed", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Import error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
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
