package com.example.itfollows;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.animation.ObjectAnimator;
import android.view.animation.LinearInterpolator;
import androidx.appcompat.app.AppCompatActivity;

public class CoinFlipMinigameActivity extends AppCompatActivity {

    private ImageView coinImage;
    private boolean isFlipping = false;
    private String playerChoice;
    private String coinResult;
    private TextView resultText;
    private Button headsBtn, tailsBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_coin_flip_minigame);
        SharedPreferences.Editor editor = getSharedPreferences("GameSettings", MODE_PRIVATE).edit();
        editor.putBoolean("vibration", false);
        editor.apply();
        coinImage = findViewById(R.id.coinImage);

        resultText = findViewById(R.id.resultText);
        headsBtn = findViewById(R.id.headsBtn);
        tailsBtn = findViewById(R.id.tailsBtn);

        headsBtn.setOnClickListener(v -> flipCoin("Heads"));
        tailsBtn.setOnClickListener(v -> flipCoin("Tails"));
    }

    private void flipCoin(String choice) {
        if (isFlipping) return;
        isFlipping = true;
        playerChoice = choice;

        // Reset image to neutral
        coinImage.setImageResource(R.drawable.coin_heads);

        // Create flip animation
        ObjectAnimator flipAnim = ObjectAnimator.ofFloat(coinImage, "rotationY", 0f, 1080f);
        flipAnim.setDuration(1500);
        flipAnim.setInterpolator(new LinearInterpolator());

        // 👇 Flip image every 150ms between heads and tails
        Handler swapHandler = new Handler();
        Runnable swapRunnable = new Runnable() {
            boolean showingHeads = true;
            int flips = 0;

            @Override
            public void run() {
                if (flips >= 10) return; // swap 10 times (~1.5s)
                showingHeads = !showingHeads;
                coinImage.setImageResource(showingHeads ? R.drawable.coin_heads : R.drawable.coin_tails);
                flips++;
                swapHandler.postDelayed(this, 150);
            }
        };
        swapHandler.post(swapRunnable);

        flipAnim.start();

        // Final result
        coinResult = Math.random() < 0.5 ? "Heads" : "Tails";

        new Handler().postDelayed(() -> {
            coinImage.setRotationY(0); // reset rotation
            coinImage.setImageResource(coinResult.equals("Heads") ? R.drawable.coin_heads : R.drawable.coin_tails);
            boolean playerWins = playerChoice.equals(coinResult);
            resultText.setText(playerWins ? "You Win!" : "You Lose!");
            resultText.setTextColor(playerWins ? Color.GREEN : Color.RED);


            new Handler().postDelayed(() -> {
                Intent result = new Intent();
                result.putExtra("minigameSuccess", playerChoice.equals(coinResult));
                setResult(RESULT_OK, result);
                finish();
            }, 1750);
        }, 1750); // wait for animation to end
    }
}
