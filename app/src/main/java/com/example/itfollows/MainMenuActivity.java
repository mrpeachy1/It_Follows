package com.example.itfollows;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainMenuActivity extends AppCompatActivity {
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

    private void clearSavedState() {
        SharedPreferences.Editor editor = getSharedPreferences("SnailGameState", MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        Log.d("MainMenuActivity", "Saved game state cleared.");
    }
    public void onStartNewGameClick(View view) {
        // Step 1: Clear the power-up inventory
        resetPowerUps();
        Log.d("MainMenuActivity", "Power-ups reset for new game.");

        // Step 2: Start the game activity
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("isNewGame", true);
        startActivity(intent);
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
            stopGameServiceAndReset(); // Stop current game
            clearSavedState();         // Optional: clear saved state
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("isNewGame", true);
            startActivity(intent);
        });

        // ✅ Resume Game
        continueButton.setOnClickListener(v -> {
            GameManager.isNewGame = false;
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("isNewGame", false);
            startActivity(intent);
        });

        // Other buttons
        findViewById(R.id.buttonHowToPlay).setOnClickListener(v ->
                startActivity(new Intent(this, HowToPlayActivity.class)));

        findViewById(R.id.buttonSettings).setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        findViewById(R.id.buttonCredits).setOnClickListener(v ->
                startActivity(new Intent(this, CreditsActivity.class)));
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
    }


    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.pause();          // optional: pause music when app is backgrounded
    }
}
