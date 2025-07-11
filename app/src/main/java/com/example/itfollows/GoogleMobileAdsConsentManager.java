package com.example.itfollows; // Ensure this matches your package structure

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.ump.ConsentDebugSettings;
import com.google.android.ump.ConsentForm;
import com.google.android.ump.ConsentInformation;
import com.google.android.ump.ConsentRequestParameters;
import com.google.android.ump.FormError;
import com.google.android.ump.UserMessagingPlatform;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A helper class to manage User Messaging Platform (UMP) consent.
 * It encapsulates the logic for gathering and managing user consent.
 */
public class GoogleMobileAdsConsentManager {
    private static final String TAG = "AdsConsentManager";
    private static GoogleMobileAdsConsentManager instance;
    private final ConsentInformation consentInformation;

    // Use an atomic boolean to track whether consent information is updating.
    private final AtomicBoolean isGatheringConsent = new AtomicBoolean(false);


    /**
     * Listener for the consent gathering process.
     */
    public interface OnConsentGatheringCompleteListener {
        void consentGatheringComplete(FormError error);
    }

    private GoogleMobileAdsConsentManager(Context context) {
        this.consentInformation = UserMessagingPlatform.getConsentInformation(context);
    }

    public static synchronized GoogleMobileAdsConsentManager getInstance(Context context) {
        if (instance == null) {
            instance = new GoogleMobileAdsConsentManager(context.getApplicationContext());
        }
        return instance;
    }

    /**
     * Helper method to call the UMP SDK methods to request consent information and load/show a
     * consent form if necessary.
     *
     * @param activity The activity context.
     * @param onConsentGatheringCompleteListener Listener for completion.
     */
    public void gatherConsent(
            Activity activity,
            OnConsentGatheringCompleteListener onConsentGatheringCompleteListener) {

        // Set isGatheringConsent to true.
        if (!isGatheringConsent.compareAndSet(false, true)) {
            // If already gathering consent, notify the listener that the process is in progress
            // You might choose to queue requests or simply return if already in progress.
            // For simplicity, we'll assume one call at a time is sufficient from AdConsentActivity.
            Log.d(TAG, "Consent gathering is already in progress.");
            // Optionally, call onConsentGatheringCompleteListener.consentGatheringComplete(null)
            // if you want to proceed as if it completed, but this might be risky.
            return;
        }

        // TODO: For testing purposes, you can force geography and reset consent.
        // Remove or comment out these lines for production builds.
        ConsentDebugSettings debugSettings = new ConsentDebugSettings.Builder(activity)
                 .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
                 .addTestDeviceHashedId("92F39A4C67C13C446D207A13BE7EF52E") // Can be same as AdMob's
                .build();

        ConsentRequestParameters params = new ConsentRequestParameters.Builder()
                .setConsentDebugSettings(debugSettings) // Uncomment for testing
                .setTagForUnderAgeOfConsent(false) // Or true, based on your app's audience
                .build();

        // Requesting an update to consent information should be called on every app launch.
        consentInformation.requestConsentInfoUpdate(
                activity,
                params,
                () -> {
                    // Consent information updated successfully.
                    // Load and show the consent form if required.
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                            activity,
                            loadAndShowError -> {
                                // Consent form shown or not required.
                                isGatheringConsent.set(false);
                                onConsentGatheringCompleteListener.consentGatheringComplete(loadAndShowError);
                            });
                },
                requestConsentError -> {
                    // Consent information update failed.
                    isGatheringConsent.set(false);
                    onConsentGatheringCompleteListener.consentGatheringComplete(requestConsentError);
                });
    }

    /**
     * Helper method to check if the app can request ads.
     * This should be checked before initializing the Mobile Ads SDK.
     *
     * @return True if consent is not required or if consent has been obtained.
     */
    public boolean canRequestAds() {
        return consentInformation.canRequestAds();
    }

    /**
     * Helper method to check if a privacy options form is required.
     *
     * @return True if privacy options form is required.
     */
    public boolean isPrivacyOptionsRequired() {
        return consentInformation.getPrivacyOptionsRequirementStatus()
                == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED;
    }

    /**
     * Helper method to show the privacy options form.
     *
     * @param activity The activity context.
     * @param onConsentFormDismissedListener Listener for form dismissal.
     */
    public void showPrivacyOptionsForm(
            Activity activity,
            ConsentForm.OnConsentFormDismissedListener onConsentFormDismissedListener) { // Corrected type
        UserMessagingPlatform.showPrivacyOptionsForm(activity, onConsentFormDismissedListener);
    }
    /**
     * Resets the consent information. Useful for testing.
     * Call this before gatherConsent() in testing scenarios if you want to see the form again.
     */
    public void resetConsent() {
        consentInformation.reset();
        Log.d(TAG, "Consent information has been reset.");
    }
}