package com.example.itfollows; // Ensure this matches your package structure

import androidx.annotation.Nullable;
import com.google.android.gms.ads.rewarded.RewardedAd;

/**
 * A Singleton class to manage and provide access to pre-loaded ads.
 */
public class AdManager {

    private static AdManager instance;
    private RewardedAd preloadedRewardedAd;
    private boolean isRewardedAdBeingConsumed = false; // Flag to prevent multiple consumptions

    // Private constructor to prevent instantiation from other classes
    private AdManager() {}

    /**
     * Gets the single instance of the AdManager.
     *
     * @return The singleton AdManager instance.
     */
    public static synchronized AdManager getInstance() {
        if (instance == null) {
            instance = new AdManager();
        }
        return instance;
    }

    /**
     * Sets the pre-loaded rewarded ad.
     * This should be called from AdConsentActivity when an ad is successfully loaded.
     *
     * @param rewardedAd The rewarded ad that has been loaded, or null if loading failed.
     */
    public void setPreloadedRewardedAd(@Nullable RewardedAd rewardedAd) {
        this.preloadedRewardedAd = rewardedAd;
        this.isRewardedAdBeingConsumed = false; // Reset consumption flag when a new ad is set
        if (rewardedAd == null) {
            // Log.d("AdManager", "Preloaded rewarded ad set to null (likely failed to load or consumed).");
        } else {
            // Log.d("AdManager", "Preloaded rewarded ad has been set.");
        }
    }

    /**
     * Gets the pre-loaded rewarded ad.
     * This method is designed to be called by GameOverActivity.
     * It returns the ad and marks it as "being consumed" to help prevent
     * trying to show the same ad instance multiple times if there are rapid calls.
     * The ad object should be explicitly set to null after it's shown or if
     * GameOverActivity is destroyed before showing it, by calling `consumePreloadedRewardedAd()`.
     *
     * @return The preloaded RewardedAd, or null if not available or already being consumed.
     */
    @Nullable
    public RewardedAd retrievePreloadedRewardedAd() {
        if (preloadedRewardedAd != null && !isRewardedAdBeingConsumed) {
            // Log.d("AdManager", "Preloaded rewarded ad is being retrieved.");
            // isRewardedAdBeingConsumed = true; // Mark as being consumed
            // Consider if you want to nullify it immediately here or let the caller do it via consumePreloadedRewardedAd()
            return preloadedRewardedAd;
        }
        if (preloadedRewardedAd == null) {
            // Log.d("AdManager", "Attempted to retrieve preloaded ad, but it was null.");
        } else if (isRewardedAdBeingConsumed) {
            // Log.d("AdManager", "Attempted to retrieve preloaded ad, but it's already marked as being consumed.");
        }
        return null;
    }

    /**
     * Call this method after the preloaded rewarded ad has been shown
     * or if the activity intending to show it is destroyed before showing it.
     * This ensures the ad object is cleared and won't be accidentally reused.
     */
    public void consumePreloadedRewardedAd() {
        // Log.d("AdManager", "Preloaded rewarded ad consumed/cleared.");
        this.preloadedRewardedAd = null;
        this.isRewardedAdBeingConsumed = false;
    }

    /**
     * Checks if a rewarded ad is currently loaded and ready to be shown.
     * @return true if an ad is loaded, false otherwise.
     */
    public boolean isRewardedAdLoaded() {
        return preloadedRewardedAd != null;
    }
}