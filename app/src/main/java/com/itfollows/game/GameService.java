package com.itfollows.game;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.itfollows.game.ui.achievements.AchievementsManager; // <-- adjust if your path differs

import java.util.Calendar;

public class GameService extends Service {
    private static final int NOTIF_ID = 42;

    // Broadcasts you already use
    public static final String ACTION_GAME_STATE_UPDATE = "ACTION_GAME_STATE_UPDATE";
    public static final String ACTION_GAME_OVER = "ACTION_GAME_OVER";

    private static final String PREFS_RUN = "RunState";
    private static final String KEY_RUN_STARTED_MS = "runStartedMs";
    private static final String KEY_USED_POWERUP = "usedPowerUpThisRun";
    private static final String KEY_SAW_AFTER_8PM = "sawAfter8pmThisRun";

    private FusedLocationProviderClient fused;
    private PendingIntent locationPI;

    // Tick loop for survival-time progress
    private Handler tickHandler;
    private Runnable tickRunnable;
    private long lastTickMs;

    @Override
    public void onCreate() {
        super.onCreate();

        Notification n = new NotificationCompat.Builder(this, App.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Snail is chasing…")
                .setContentText("Tracking your position for gameplay.")
                .setOngoing(true)
                .build();
        startForeground(NOTIF_ID, n);

        fused = LocationServices.getFusedLocationProviderClient(this);
        scheduleReconcile();

        // Mark (or re-mark) the start of a run when service spins up
        startRun(getApplicationContext());

        startTickLoop();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Intent i = new Intent(this, LocationUpdatesReceiver.class).setAction("LOC_TICK");
        locationPI = PendingIntent.getBroadcast(
                this, 0, i, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        LocationRequest req = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 2000L)
                .setMinUpdateIntervalMillis(1500L)
                .setMaxUpdateDelayMillis(15000L)
                .setWaitForAccurateLocation(false)
                .build();

        fused.requestLocationUpdates(req, locationPI);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fused != null && locationPI != null) {
            fused.removeLocationUpdates(locationPI);
        }
        stopTickLoop();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void scheduleReconcile() {
        ReconcileScheduler.schedule(getApplicationContext());
    }

    /**
     * Clears any persisted game state so a new game can start fresh.
     */
    public static void clearSavedState(Context context) {
        SharedPreferences.Editor editor =
                context.getSharedPreferences("SnailGameState", Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        Log.d("GameService", "Saved game state cleared.");
    }

    /* -------------------------------------------------------------------------
     *                              ACHIEVEMENTS HOOKS
     * ------------------------------------------------------------------------- */

    /** Call this once when a run starts (service onCreate already does it). */
    public static void startRun(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(PREFS_RUN, MODE_PRIVATE);
        prefs.edit()
                .putLong(KEY_RUN_STARTED_MS, System.currentTimeMillis())
                .putBoolean(KEY_USED_POWERUP, false)
                .putBoolean(KEY_SAW_AFTER_8PM, isAfter8pmNow())
                .apply(); // record if we started after 8pm
    }

    /** Call when a run ends successfully (player survives/escapes). */
    public static void endRunSuccess(Context c) {
        SharedPreferences prefs = c.getSharedPreferences(PREFS_RUN, MODE_PRIVATE);
        // First escape (binary) – leave it idempotent
        AchievementsManager.unlock(c, "first_escape");

        // If at any point 8pm+ was seen during this run, unlock Night Crawler
        if (prefs.getBoolean(KEY_SAW_AFTER_8PM, false) || isAfter8pmNow()) {
            AchievementsManager.unlock(c, "night_crawler");
        }

        // If no power-ups were used, progress Thrifty Survivor by the run duration in seconds
        long started = prefs.getLong(KEY_RUN_STARTED_MS, System.currentTimeMillis());
        long seconds = Math.max(0, (System.currentTimeMillis() - started) / 1000);
        if (!prefs.getBoolean(KEY_USED_POWERUP, false)) {
            AchievementsManager.addProgress(c, "thrifty_survivor", (int) seconds);
        }
    }

    /** Call when a run fails/ends (optional; keeps flags tidy). */
    public static void endRunFailure(Context c) {
        // For now, nothing to unlock; keep method for symmetry and future use.
    }

    /** Mark that any power-up was used this run. */
    public static void markPowerUpUsed(Context c) {
        c.getSharedPreferences(PREFS_RUN, MODE_PRIVATE)
                .edit().putBoolean(KEY_USED_POWERUP, true).apply();
    }

    /** Add distance delta (meters) – call from your LocationUpdatesReceiver. */
    public static void addDistanceMeters(Context c, int metersDelta) {
        if (metersDelta <= 0) return;
        AchievementsManager.addProgress(c, "endurance_master", metersDelta);
        AchievementsManager.addProgress(c, "road_runner", metersDelta);
        AchievementsManager.addProgress(c, "it_follows_vet", metersDelta);
    }

    /* ----------------------- survival-time tick loop ------------------------ */

    private void startTickLoop() {
        tickHandler = new Handler(Looper.getMainLooper());
        lastTickMs = System.currentTimeMillis();
        tickRunnable = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                int deltaSec = (int) Math.max(0, (now - lastTickMs) / 1000);
                if (deltaSec > 0) {
                    feedSurvivalSeconds(deltaSec);
                    lastTickMs = now;
                }
                tickHandler.postDelayed(this, 1000L);
            }
        };
        tickHandler.postDelayed(tickRunnable, 1000L);
    }

    private void stopTickLoop() {
        if (tickHandler != null && tickRunnable != null) {
            tickHandler.removeCallbacks(tickRunnable);
        }
    }

    /** Adds survival seconds and time-of-day counters for achievements. */
    private void feedSurvivalSeconds(int secondsDelta) {
        Context ctx = getApplicationContext();

        // Base survival achievements (single-run duration)
        AchievementsManager.addProgress(ctx, "ten_min_survivor", secondsDelta);
        AchievementsManager.addProgress(ctx, "thirty_min_survivor", secondsDelta);

        // If midnight or later, count toward Midnight Oil
        if (isAfterMidnightNow()) {
            AchievementsManager.addProgress(ctx, "midnight_oil", secondsDelta);
        }

        // If we cross 8pm for the first time this run, record it
        if (isAfter8pmNow()) {
            getSharedPreferences(PREFS_RUN, MODE_PRIVATE)
                    .edit().putBoolean(KEY_SAW_AFTER_8PM, true).apply();
        }

        // If no powerups used this run, accrue for Thrifty Survivor in real-time too
        boolean used = getSharedPreferences(PREFS_RUN, MODE_PRIVATE)
                .getBoolean(KEY_USED_POWERUP, false);
        if (!used) {
            AchievementsManager.addProgress(ctx, "thrifty_survivor", secondsDelta);
        }
    }

    /* --------------------------- time helpers ------------------------------- */

    private static boolean isAfter8pmNow() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        return hour >= 20; // 20:00+
    }

    private static boolean isAfterMidnightNow() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        // strictly 00:00–04:59 window is often “late night”; we’ll count any 00:00+
        return hour >= 0; // true from midnight onward
    }
}
