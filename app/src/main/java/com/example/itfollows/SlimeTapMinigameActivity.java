package com.example.itfollows;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SlimeTapMinigameActivity extends AppCompatActivity {

    private int hits = 0;
    private int targetHits = 15;
    private long timeLimit = 15000; // 15 seconds
    private long startTime;

    private TextView timerText;
    private TextView hitCounter;
    private FrameLayout slimeField;
    private ProgressBar slimeProgressBar;
    private int targetBubbles;
    private int bubblesSpawned = 0;

    private Handler handler = new Handler();
    private Runnable gameLoop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slime_tap_minigame);
        SharedPreferences.Editor editor = getSharedPreferences("GameSettings", MODE_PRIVATE).edit();
        editor.putBoolean("vibration", false);
        editor.apply();
        hitCounter = findViewById(R.id.hitCounter);
        slimeField = findViewById(R.id.slimeField);
        slimeProgressBar = findViewById(R.id.slimeProgressBar);

        targetBubbles = 20 + (int)(Math.random() * 81);

        startTime = System.currentTimeMillis();
        spawnSlime();

        gameLoop = new Runnable() {
            @Override
            public void run() {
                long elapsed = System.currentTimeMillis() - startTime;
                long remaining = timeLimit - elapsed;
                int progress = (int) ((remaining * 100) / timeLimit);
                slimeProgressBar.setProgress(progress);

                if (elapsed >= timeLimit) {
                    endMinigame(false); // time up
                    return;
                }

                if (bubblesSpawned < targetBubbles) {
                    spawnSlimeBubble();
                }

                handler.postDelayed(this, 300); // spawn every 300ms
            }
        };

        handler.post(gameLoop);
    }
    private void spawnSlimeBubble() {
        if (slimeField.getWidth() == 0 || slimeField.getHeight() == 0) return;

        ImageView slime = new ImageView(this);
        slime.setImageResource(R.drawable.slime_bubble); // Use your bubble PNG
        int size = 100 + (int)(Math.random() * 50); // random size 100–150

        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
        params.leftMargin = (int)(Math.random() * (slimeField.getWidth() - size));
        params.topMargin = (int)(Math.random() * (slimeField.getHeight() - size));
        slime.setLayoutParams(params);

        slime.setOnClickListener(v -> {
            slimeField.removeView(slime);
            hits++;
            hitCounter.setText("Hits: " + hits);
            if (hits >= targetBubbles) {
                endMinigame(true); // success!
            }
        });

        slimeField.addView(slime);
        bubblesSpawned++;
    }
    private void endMinigame(boolean success) {
        handler.removeCallbacks(gameLoop);
        Intent result = new Intent();
        result.putExtra("minigameSuccess", success);
        setResult(RESULT_OK, result);
        finish();
    }

    private void spawnSlime() {
        ImageView slime = new ImageView(this);
        slime.setImageResource(R.drawable.slime_bubble); // your slime asset
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(150, 150);
        params.leftMargin = (int) (Math.random() * (slimeField.getWidth() - 150));
        params.topMargin = (int) (Math.random() * (slimeField.getHeight() - 150));
        slime.setLayoutParams(params);
        slime.setOnClickListener(v -> {
            slimeField.removeView(slime);
            hits++;
            hitCounter.setText("Hits: " + hits);
            if (hits >= targetHits) {
                endMinigame(true); // success!
            }
        });
        slimeField.addView(slime);
    }

}
