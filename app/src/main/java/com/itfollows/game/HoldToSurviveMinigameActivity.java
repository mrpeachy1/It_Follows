package com.itfollows.game;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;


import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class HoldToSurviveMinigameActivity extends AppCompatActivity {

    private boolean isHolding = false;
    private ImageView snailFadeImage;

    private long holdStartTime;
    private long HOLD_DURATION_MS;
    private Handler handler = new Handler();
    private ProgressBar progressBar;
    private Runnable progressUpdater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hold_to_survive_minigame);
        SharedPreferences.Editor editor = getSharedPreferences("GameSettings", MODE_PRIVATE).edit();
        editor.putBoolean("vibration", false);
        editor.apply();
        snailFadeImage = findViewById(R.id.snailFadeImage);
        HOLD_DURATION_MS = (5 + new Random().nextInt(26)) * 1000;
// Random between 5–30 seconds

        FrameLayout holdLayout = findViewById(R.id.holdLayout);
        progressBar = findViewById(R.id.holdProgressBar);

        holdLayout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                isHolding = true;
                holdStartTime = System.currentTimeMillis();
                startProgressUpdater();
                return true;
            } else if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL) {
                isHolding = false;
                stopProgressUpdater();

                long heldTime = System.currentTimeMillis() - holdStartTime;
                boolean success = heldTime >= HOLD_DURATION_MS;

                Intent resultIntent = new Intent();
                resultIntent.putExtra("minigameSuccess", success);
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
                return true;
            }
            return false;
        });
    }

    private void startProgressUpdater() {
        progressUpdater = new Runnable() {
            @Override
            public void run() {
                if (isHolding) {
                    long elapsed = System.currentTimeMillis() - holdStartTime;
                    int progress = (int) ((elapsed * 100.0) / HOLD_DURATION_MS);
                    progressBar.setProgress(Math.min(progress, 100));

                    // ✅ Fade in image smoothly based on hold duration
                    float alpha = Math.min(elapsed / (float) HOLD_DURATION_MS, 1f);
                    snailFadeImage.setAlpha(alpha);

                    if (elapsed >= HOLD_DURATION_MS) {
                        // Auto-complete success
                        isHolding = false;
                        stopProgressUpdater();
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("minigameSuccess", true);
                        setResult(Activity.RESULT_OK, resultIntent);
                        finish();
                    } else {
                        handler.postDelayed(this, 50);
                    }
                }
            }
        };
        handler.post(progressUpdater);
    }

    private void stopProgressUpdater() {
        if (progressUpdater != null) {
            handler.removeCallbacks(progressUpdater);
        }
    }
}
