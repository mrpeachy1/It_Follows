package com.itfollows.game;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.itfollows.game.ui.achievements.AchievementsManager;

public class MainMenuActivity extends AppCompatActivity {
    private static final int REQUEST_SETTINGS_BEFORE_GAME = 1001;
    private View achievementsPanel;
    private Button achievementsBtn;
    private ImageButton closeAchievementsBtn;
    private LinearLayout achievementsList;
    private TextView achievementsBadge;
    private boolean isGameServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (GameService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
    private void stopGameServiceAndReset() {
        Intent serviceIntent = new Intent(this, GameService.class);
        stopService(serviceIntent);
        Log.d("MainMenuActivity", "GameService stopped (reset for new game).");
    }

    private void resetPowerUps() {
        SharedPreferences powerUpPrefs = getSharedPreferences("PowerUpInventory", MODE_PRIVATE);
        SharedPreferences.Editor powerUpEditor = powerUpPrefs.edit();

        powerUpEditor.putInt("saltBomb", 0);
        powerUpEditor.putInt("decoyShell", 0);
        powerUpEditor.putInt("shellShield", 0);
        powerUpEditor.apply();

        Log.d("GameReset", "Power-ups have been reset to 0.");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);

        MusicManager.start(this);
        Button continueButton = findViewById(R.id.continueButton);
        TextView snailWarningText = findViewById(R.id.snailWarningText);
        achievementsPanel = findViewById(R.id.achievementsPanel);
        achievementsBtn = findViewById(R.id.achievementsBtn);
        closeAchievementsBtn = findViewById(R.id.closeAchievementsBtn);
        achievementsList = findViewById(R.id.achievementsList);
        achievementsBadge = findViewById(R.id.achievementsBadge);

        // ✅ Show Continue if GameService is running
        if (isGameServiceRunning()) {
            continueButton.setVisibility(View.VISIBLE);
            continueButton.setEnabled(true);
            continueButton.setAlpha(1f);
            snailWarningText.setVisibility(View.VISIBLE);
        } else {
            continueButton.setVisibility(View.GONE);
            snailWarningText.setVisibility(View.GONE);
        }

        // ✅ Start New Game
        Button newGameButton = findViewById(R.id.buttonStart);
        newGameButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivityForResult(intent, REQUEST_SETTINGS_BEFORE_GAME);
        });

        // ✅ Resume Game
        continueButton.setOnClickListener(v -> {
            GameManager.isNewGame = false;
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("isNewGame", false);
            startActivity(intent);
        });

        // Other buttons
        findViewById(R.id.buttonHowToPlay).setOnClickListener(v ->
                startActivity(new Intent(this, HowToPlayActivity.class)));

        findViewById(R.id.buttonCredits).setOnClickListener(v ->
                startActivity(new Intent(this, CreditsActivity.class)));

        if (achievementsBtn != null && achievementsPanel != null) {
            achievementsBtn.setOnClickListener(v -> {
                achievementsPanel.setVisibility(View.VISIBLE);
                populateAchievements();
            });
        }

        if (closeAchievementsBtn != null && achievementsPanel != null) {
            closeAchievementsBtn.setOnClickListener(v -> achievementsPanel.setVisibility(View.GONE));
        }

        updateAchievementsBadge();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_SETTINGS_BEFORE_GAME && resultCode == RESULT_OK) {
            stopGameServiceAndReset();
            GameService.clearSavedState(this);
            resetPowerUps();
            Log.d("MainMenuActivity", "Power-ups reset for new game.");

            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra("isNewGame", true);
            startActivity(intent);
        }
    }



    @Override
    protected void onResume() {
        super.onResume();

        MusicManager.setVolume(this);
        MusicManager.resume();

        SharedPreferences prefs = getSharedPreferences("SnailGameState", MODE_PRIVATE);
        boolean hasSavedGame = prefs.contains("snail_lat_before_pause") && prefs.contains("player_lat_before_pause");

        Button continueButton = findViewById(R.id.continueButton);
        TextView snailWarningText = findViewById(R.id.snailWarningText);

        if (hasSavedGame) {
            continueButton.setVisibility(View.VISIBLE);
            snailWarningText.setVisibility(View.VISIBLE);
        } else {
            continueButton.setVisibility(View.GONE);
            snailWarningText.setVisibility(View.GONE);
        }

        updateAchievementsBadge();
        if (achievementsPanel != null && achievementsPanel.getVisibility() == View.VISIBLE) {
            populateAchievements();
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.pause();          // optional: pause music when app is backgrounded
    }

    private void updateAchievementsBadge() {
        if (achievementsBadge == null) {
            return;
        }
        int unlocked = AchievementsManager.getUnlockedCount(this);
        int total = AchievementsManager.getTotalCount();
        if (total > 0) {
            achievementsBadge.setText(unlocked + "/" + total);
            achievementsBadge.setVisibility(View.VISIBLE);
        } else {
            achievementsBadge.setVisibility(View.GONE);
        }
    }

    private void populateAchievements() {
        if (achievementsList == null) {
            return;
        }

        achievementsList.removeAllViews();

        for (AchievementsManager.Achievement a : AchievementsManager.catalog()) {
            View item = getLayoutInflater().inflate(R.layout.achievement_item, achievementsList, false);

            TextView title = item.findViewById(R.id.achievementTitle);
            TextView desc  = item.findViewById(R.id.achievementDesc);
            TextView status= item.findViewById(R.id.achievementStatus);
            ImageView icon = item.findViewById(R.id.achievementIcon);

            title.setText(a.title);
            desc.setText(a.desc);

            boolean unlocked = (a.target == 0)
                    ? AchievementsManager.isUnlocked(this, a.key)
                    : AchievementsManager.getProgress(this, a.key) >= a.target;

            if (unlocked) {
                status.setText("Unlocked");
                status.setTextColor(Color.parseColor("#A5D6A7"));
                icon.setImageResource(R.drawable.ic_trophy_unlocked);
            } else {
                if (a.target > 0) {
                    int p = AchievementsManager.getProgress(this, a.key);
                    status.setText(p + " / " + a.target);
                } else {
                    status.setText("Locked");
                }
                status.setTextColor(Color.parseColor("#FF8A80"));
                icon.setImageResource(R.drawable.ic_trophy_locked);
            }

            achievementsList.addView(item);
        }
    }
}
