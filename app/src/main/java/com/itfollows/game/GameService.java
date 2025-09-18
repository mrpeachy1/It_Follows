package com.itfollows.game;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

public class GameService extends Service {
    private static final int NOTIF_ID = 42;
    public static final String ACTION_GAME_STATE_UPDATE = "ACTION_GAME_STATE_UPDATE";
    public static final String ACTION_GAME_OVER = "ACTION_GAME_OVER";
    private FusedLocationProviderClient fused;
    private PendingIntent locationPI;

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
     *
     * @param context context used to access shared preferences
     */
    public static void clearSavedState(Context context) {
        SharedPreferences.Editor editor =
                context.getSharedPreferences("SnailGameState", Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
        Log.d("GameService", "Saved game state cleared.");
    }
}
