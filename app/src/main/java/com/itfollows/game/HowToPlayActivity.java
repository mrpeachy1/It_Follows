package com.itfollows.game;

import android.os.Bundle;
import android.widget.ImageButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class HowToPlayActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_how_to_play);

        // Handle window insets (e.g. for edge-to-edge display)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.howToPlayLayout), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 🔊 Start shared ambient music
        MusicManager.start(this);

        // ❌ Handle close button
        ImageButton closeBtn = findViewById(R.id.closeButton);
        if (closeBtn != null) {
            closeBtn.setOnClickListener(v -> finish());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.setVolume(this);
        MusicManager.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.pause();
    }
}
