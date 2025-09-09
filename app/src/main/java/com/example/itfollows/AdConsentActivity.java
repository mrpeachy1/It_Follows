/*
 * Copyright 2013-2023 Google LLC
 * (Your license header)
 */
package com.example.itfollows; // Ensure this matches your package structure

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
// Import Menu, MenuItem, View, FrameLayout, PopupMenu, Toast if you uncomment that logic
// import android.view.Menu;
// import android.view.MenuItem;
// import android.widget.Toast;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
// Import AdSize, AdView if you uncomment banner logic
// import com.google.android.gms.ads.AdSize;
// import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.Arrays;
import java.util.Collections; // For Collections.singletonList if only one test device
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Activity to handle User Messaging Platform (UMP) consent,
 * initialize the Google Mobile Ads SDK, and pre-load the rewarded ad.
 * This should be your LAUNCHER activity.
 */
public class AdConsentActivity extends AppCompatActivity {

    // ** STEP 1: ACTION REQUIRED BY YOU **
    // Find this in your Logcat output when running the app with ads for the first time.
    // It will say something like:
    // "Use RequestConfiguration.Builder().setTestDeviceIds(Arrays.asList("YOUR_HASHED_ID")) ..."
    public static final String TEST_DEVICE_HASHED_ID = "92F39A4C67C13C446D207A13BE7EF52E"; // <-- REPLACE THIS!

    // Your Ad Unit ID for the REWARDED AD to be pre-loaded for GameOverActivity
    private static final String REWARDED_AD_UNIT_ID = "ca-app-pub-2457437725733859/2353449523";

    private static final String TAG = "AdConsentActivity";
    private final AtomicBoolean isMobileAdsInitializeCalled = new AtomicBoolean(false);
    private final AtomicBoolean isGameActivityLaunched = new AtomicBoolean(false); // To prevent multiple launches
    private GoogleMobileAdsConsentManager googleMobileAdsConsentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ad_consent); // You'll create this in Step 2

        //Log.d(TAG, "Google Mobile Ads SDK Version: " + MobileAds.getVersionString());

        googleMobileAdsConsentManager =
                GoogleMobileAdsConsentManager.getInstance(getApplicationContext());
        googleMobileAdsConsentManager.gatherConsent(
                this,
                consentError -> {
                    if (consentError != null) {
                        Log.w(
                                TAG,
                                String.format(
                                        "Consent error: %s: %s",
                                        consentError.getErrorCode(), consentError.getMessage()));
                    }

                    if (googleMobileAdsConsentManager.canRequestAds()) {
                        initializeMobileAdsSdkAndPreload();
                    } else {
                        Log.d(TAG, "Cannot request ads based on consent. Proceeding to GameActivity.");
                        launchGameActivityOnce();
                    }
                });

        // If consent is already sufficient from a previous session, initialize immediately.
        if (googleMobileAdsConsentManager.canRequestAds()) {
            // Check if already called by the callback above to avoid double initialization
            if (!isMobileAdsInitializeCalled.get()) {
                initializeMobileAdsSdkAndPreload();
            }
        }
    }

    private void initializeMobileAdsSdkAndPreload() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return; // Initialize only once
        }

        // **IMPORTANT**: Ensure TEST_DEVICE_HASHED_ID is set correctly.
        if ("PLACEHOLDER_TEST_DEVICE_ID".equals(TEST_DEVICE_HASHED_ID) || TEST_DEVICE_HASHED_ID.isEmpty()) {
            Log.e(TAG, "******************************************************************************");
            Log.e(TAG, "** TEST_DEVICE_HASHED_ID is not set!                                        **");
            Log.e(TAG, "** Find it in Logcat (search for 'Use RequestConfiguration.Builder()')      **");
            Log.e(TAG, "** and update it in AdConsentActivity.java.                                 **");
            Log.e(TAG, "** Running with real ads without a test device can violate AdMob policy.    **");
            Log.e(TAG, "******************************************************************************");
            // Optionally, you could prevent ad loading here or throw an error in debug builds.
        }

        RequestConfiguration.Builder requestConfigBuilder = new RequestConfiguration.Builder();
        if (!"PLACEHOLDER_TEST_DEVICE_ID".equals(TEST_DEVICE_HASHED_ID) && !TEST_DEVICE_HASHED_ID.isEmpty()) {
            requestConfigBuilder.setTestDeviceIds(Collections.singletonList(TEST_DEVICE_HASHED_ID));
        }
        MobileAds.setRequestConfiguration(requestConfigBuilder.build());

        new Thread(
                () -> {
                    MobileAds.initialize(this, initializationStatus -> {
                        Log.d(TAG, "Mobile Ads SDK Initialized. Initialization status: " + initializationStatus.getAdapterStatusMap());
                        runOnUiThread(() -> {
                            if (googleMobileAdsConsentManager.canRequestAds()) {
                                Log.d(TAG, "Attempting to pre-load Rewarded Ad.");
                                RewardedAd.load(
                                        AdConsentActivity.this,
                                        REWARDED_AD_UNIT_ID, // Your actual rewarded ad unit ID
                                        new AdRequest.Builder().build(),
                                        new RewardedAdLoadCallback() {
                                            @Override
                                            public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                                                AdManager.getInstance().setPreloadedRewardedAd(rewardedAd);
                                                Log.i(TAG, "Rewarded ad pre-loaded successfully.");
                                                launchGameActivityOnce();
                                            }

                                            @Override
                                            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                                                Log.e(TAG, "Rewarded ad failed to pre-load: " + loadAdError.getMessage());
                                                AdManager.getInstance().setPreloadedRewardedAd(null);
                                                launchGameActivityOnce();
                                            }
                                        });
                            } else {
                                Log.d(TAG, "Cannot request ads for pre-loading after SDK init. Proceeding to GameActivity.");
                                launchGameActivityOnce();
                            }
                        });
                    });
                })
                .start();
    }

    private void launchGameActivityOnce() {
        if (isGameActivityLaunched.compareAndSet(false, true)) {
            if (isFinishing() || isDestroyed()) {
                Log.w(TAG, "Activity is finishing, not launching GameActivity.");
                return;
            }
            Log.d(TAG, "Launching GameActivity.");
            Intent intent = new Intent(AdConsentActivity.this, GameActivity.class);
            startActivity(intent);
            finish();
        }
    }
}