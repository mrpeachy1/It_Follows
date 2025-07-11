package com.example.itfollows; // Your package name

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.content.SharedPreferences;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class GameOverActivity extends AppCompatActivity {

    public static final String EXTRA_TIME_TAKEN = "com.example.itfollows.EXTRA_TIME_TAKEN";
    public static final String EXTRA_DISTANCE_TRAVELED = "com.example.itfollows.EXTRA_DISTANCE_TRAVELED";
    public static final int RESULT_RETRY = Activity.RESULT_FIRST_USER; // Value: 1
    public static final int RESULT_MAIN_MENU = Activity.RESULT_FIRST_USER + 1; // Value: 2
    public static final int RESULT_REVIVE = Activity.RESULT_FIRST_USER + 2; // Value: 3

    private TextView timeTakenText, distanceTraveledText;
    private Button retryButton, mainMenuButton, reviveButton;

    private RewardedAd mRewardedAd;
    private final String TAG = "GameOverActivity_Ad";
    // Use test Ad Unit ID for development. Replace with your real ID for production.
    private final String AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";
    private boolean adLoading = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game_over); // Ensure you have this layout

        timeTakenText = findViewById(R.id.timeTakenText); // Ensure these IDs exist in your layout
        distanceTraveledText = findViewById(R.id.distanceTraveledText);
        retryButton = findViewById(R.id.retryButton);
        mainMenuButton = findViewById(R.id.mainMenuButton);
        reviveButton = findViewById(R.id.reviveButton); // Add this button to your layout

        long timeTakenMillis = getIntent().getLongExtra(EXTRA_TIME_TAKEN, 0);
        float distanceTraveled = getIntent().getFloatExtra(EXTRA_DISTANCE_TRAVELED, 0f);
        SharedPreferences prefs = getSharedPreferences("GameSettings", MODE_PRIVATE);
        boolean useImperial = "Imperial".equals(prefs.getString(SettingsActivity.KEY_MEASUREMENT_UNIT, "Metric"));

        long seconds = (timeTakenMillis / 1000) % 60;
        long minutes = (timeTakenMillis / (1000 * 60)) % 60;
        // long hours = (timeTakenMillis / (1000 * 60 * 60)) % 24; // If needed

        timeTakenText.setText(String.format("Time Survived: %02d:%02d", minutes, seconds));
        if (useImperial) {
            double feet = distanceTraveled * 3.28084;
            distanceTraveledText.setText(String.format(Locale.US, "Snail Traveled: %.1f ft", feet));
        } else {
            distanceTraveledText.setText(String.format(Locale.US, "Snail Traveled: %.1f m", distanceTraveled));
        }

        // Initialize Mobile Ads SDK
        MobileAds.initialize(this, initializationStatus -> {
            Log.d(TAG, "Mobile Ads SDK Initialized.");
            loadRewardedAd(); // Load an ad when the activity starts
        });


        retryButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            setResult(RESULT_RETRY, resultIntent);
            finish();
        });

        mainMenuButton.setOnClickListener(v -> {
            Intent resultIntent = new Intent();
            setResult(RESULT_MAIN_MENU, resultIntent);
            finish();
        });

        reviveButton.setOnClickListener(v -> {
            if (mRewardedAd != null) {
                mRewardedAd.show(GameOverActivity.this, new OnUserEarnedRewardListener() {
                    @Override
                    public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                        // Handle the reward.
                        Log.d(TAG, "User earned the reward.");
                        // User watched the ad, set result for revive
                        Intent resultIntent = new Intent();
                        setResult(RESULT_REVIVE, resultIntent);
                        finish();
                    }
                });
            } else {
                Log.d(TAG, "The rewarded ad wasn't ready yet.");
                Toast.makeText(GameOverActivity.this, "Ad not ready. Try again shortly.", Toast.LENGTH_SHORT).show();
                // Optionally try to load another ad here or disable the button temporarily
                if (!adLoading) loadRewardedAd();
            }
        });
        reviveButton.setEnabled(false); // Initially disable until ad is loaded
    }

    private void loadRewardedAd() {
        if (mRewardedAd == null && !adLoading) {
            adLoading = true;
            AdRequest adRequest = new AdRequest.Builder().build();
            RewardedAd.load(this, AD_UNIT_ID,
                    adRequest, new RewardedAdLoadCallback() {
                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                            // Handle the error.
                            Log.d(TAG, loadAdError.toString());
                            mRewardedAd = null;
                            adLoading = false;
                            reviveButton.setEnabled(false); // Keep disabled or provide feedback
                            Toast.makeText(GameOverActivity.this, "Revive ad failed to load.", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                            mRewardedAd = rewardedAd;
                            adLoading = false;
                            Log.d(TAG, "Ad was loaded.");
                            reviveButton.setEnabled(true); // Enable the button now

                            mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                                @Override
                                public void onAdClicked() {
                                    // Called when a click is recorded for an ad.
                                    Log.d(TAG, "Ad was clicked.");
                                }

                                @Override
                                public void onAdDismissedFullScreenContent() {
                                    // Called when ad is dismissed.
                                    // Set the ad reference to null so you don't show the ad a second time.
                                    Log.d(TAG, "Ad dismissed fullscreen content.");
                                    mRewardedAd = null;
                                    reviveButton.setEnabled(false); // Ad consumed
                                    // Load the next ad:
                                    loadRewardedAd();
                                }

                                @Override
                                public void onAdFailedToShowFullScreenContent(AdError adError) {
                                    // Called when ad fails to show.
                                    Log.e(TAG, "Ad failed to show fullscreen content.");
                                    mRewardedAd = null;
                                    reviveButton.setEnabled(false);
                                }

                                @Override
                                public void onAdImpression() {
                                    // Called when an impression is recorded for an ad.
                                    Log.d(TAG, "Ad recorded an impression.");
                                }

                                @Override
                                public void onAdShowedFullScreenContent() {
                                    // Called when ad is shown.
                                    Log.d(TAG, "Ad showed fullscreen content.");
                                }
                            });
                        }
                    });
        }
    }

    @Override
    public void onBackPressed() {
        // Default behavior might just finish. If you want to force a choice:
        Intent resultIntent = new Intent();
        setResult(RESULT_MAIN_MENU, resultIntent); // Or another default
        Log.d("BackButton", "User pressed BACK!");
        super.onBackPressed();
    }
}