package com.itfollows.game;

import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class CreditsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_credits);

        // 🔊 Use shared ambient music
        MusicManager.start(this);

        Button backBtn = findViewById(R.id.buttonBack);
        backBtn.setOnClickListener(v -> finish());
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
