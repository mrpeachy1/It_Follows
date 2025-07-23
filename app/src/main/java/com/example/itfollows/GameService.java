package com.example.itfollows;
import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GameService extends Service {
    private static final int NOTIFICATION_ID = 123;
    private static final String CHANNEL_ID = "GameServiceChannel";
    private static final long GAME_TICK_INTERVAL = 1000; // 1 second
    private static final String TAG = "GameService";
    private final Handler snailHandler = new Handler();
    private LatLng snailPosition;
    private LatLng currentPlayerLocation;
    private static final float GAME_OVER_RADIUS_METERS = 3.0f;
    public static final String ACTION_GAME_STATE_UPDATE = "com.example.itfollows.GAME_STATE_UPDATE";
    public static final String ACTION_GAME_OVER = "com.example.itfollows.GAME_OVER";
    private String currentSnailSpeedSetting = "Normal Chase";
    private float gameOverDistanceMeters = 5.0f;
    private boolean hasGameEnded = false;
    private boolean isGameRunning = false;
    private static final String PREFS_GAME_SERVICE_STATE = "GameServiceState";
    private static final String KEY_SNAIL_LAT = "snailLat";
    private static final String KEY_SNAIL_LNG = "snailLng";
    private static final String KEY_PLAYER_LAT = "playerLat";
    private static final String KEY_PLAYER_LNG = "playerLng";
    private static final String KEY_IS_GAME_RUNNING = "isGameRunning";
    private long gameTickIntervalMs = 2000;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private final Handler gameLoopHandler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate called");
        createNotificationChannel();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        loadGameState();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand called");
        if (intent != null) {
            // Retrieve initial data passed from MainActivity
            if (intent.hasExtra("player_lat") && intent.hasExtra("player_lng")) {
                currentPlayerLocation = new LatLng(
                        intent.getDoubleExtra("player_lat", 0),
                        intent.getDoubleExtra("player_lng", 0)
                );
            }
            if (intent.hasExtra("snail_lat") && intent.hasExtra("snail_lng")) {
                snailPosition = new LatLng(
                        intent.getDoubleExtra("snail_lat", 0),
                        intent.getDoubleExtra("snail_lng", 0)
                );
                saveSnailTrailPoint(snailPosition);
            }

            // Retrieve game settings if passed
            currentSnailSpeedSetting = intent.getStringExtra("snail_speed_setting"); // Use semicolon or nothing if next line is new statement
            gameOverDistanceMeters = intent.getFloatExtra("game_over_distance", 5.0f); // Use semicolon or nothing
            gameTickIntervalMs = intent.getLongExtra("game_tick_interval", 2000L); // Semicolon is optional if it's the last statement in block

        } // End of if (intent != null)

        // Fallback if initial positions are not set (e.g., service restarted by system)
        if (currentPlayerLocation == null && snailPosition == null && !isGameRunning) {
            Log.w(TAG, "Service started without initial positions and no saved running game. Stopping.");
            stopSelf(); // Or attempt to load from a more robust saved state.
            return START_NOT_STICKY;
        }
            Notification notification = createNotification("Snail is hunting...");
            startForeground(NOTIFICATION_ID, notification);
            if (!isGameRunning) { // Start game logic only if not already running (e.g. from a previous start command)
                isGameRunning = true;
                startLocationUpdates();
                startGameLoop();
                saveGameState(); // Save that the game is now running
            }

            if (intent.hasExtra("snail_lat") && intent.hasExtra("snail_lng")) {
                snailPosition = new LatLng(
                        intent.getDoubleExtra("snail_lat", 0),
                        intent.getDoubleExtra("snail_lng", 0)

                );
                saveSnailTrailPoint(snailPosition);
            }
        startForeground(NOTIFICATION_ID, notification);

        startLocationUpdates();
        startGameLoop();

        if (!isGameRunning) { // Start game logic only if not already running (e.g. from a previous start command)
            isGameRunning = true;
            startLocationUpdates();
            startGameLoop();
            saveGameState(); // Save that the game is now running
        }
        return START_NOT_STICKY;
    }

    private void startGameLoop() {
        Log.d(TAG, "Starting game loop");
        gameLoopHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!isGameRunning) {
                    Log.d(TAG, "Game loop stopping as isGameRunning is false.");
                    return;
                }

                if (snailPosition != null && currentPlayerLocation != null) {
                    moveSnailCloser();
                    if (checkGameOver()) {
                        handleGameOver();
                    } else {
                        broadcastGameState();
                        updateNotificationText();
                        saveGameState(); // Persist snail position
                        gameLoopHandler.postDelayed(this, gameTickIntervalMs); // Continue loop
                    }
                } else {
                    Log.w(TAG, "Snail or player position is null in game loop. Skipping tick.");
                    gameLoopHandler.postDelayed(this, gameTickIntervalMs); // Retry next tick
                }
            }
        }, gameTickIntervalMs);
    }

    private void startLocationUpdates() {
        Log.d(TAG, "Starting location updates in service");
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(5000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {

            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null || !isGameRunning) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        currentPlayerLocation = new LatLng(location.getLatitude(), location.getLongitude());
                        Log.d(TAG, "Service Location Update: " + currentPlayerLocation);
                        // No need to broadcast every location update unless MainActivity specifically needs it
                        // The game loop will use this updated currentPlayerLocation
                        saveGameState(); // Persist last known player location
                    }
                }
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            Log.e(TAG, "Location permission not granted for service. Stopping.");
            stopGameAndService(); // Or handle differently
        }
    }

    private void moveSnailCloser() {
        if (snailPosition == null || currentPlayerLocation == null) return;

        // --- Re-implement snail movement logic here (simplified from MainActivity) ---
        // You'll need access to `getSnailMoveStepDegrees` or its equivalent values.
        // For simplicity, let's assume a fixed step for now or pass speed settings.
        float speedDegrees = getSnailMoveStepDegreesFromSetting(currentSnailSpeedSetting); // Implement this helper

        double latDiff = currentPlayerLocation.latitude - snailPosition.latitude;
        double lngDiff = currentPlayerLocation.longitude - snailPosition.longitude;
        double angle = Math.atan2(latDiff, lngDiff);

        double newSnailLat = snailPosition.latitude + (speedDegrees * Math.sin(angle));
        double newSnailLng = snailPosition.longitude + (speedDegrees * Math.cos(angle));
        snailPosition = new LatLng(newSnailLat, newSnailLng);
        saveSnailTrailPoint(snailPosition);
        Log.d(TAG, "Snail moved to: " + snailPosition);
    }

    // You'll need this method or similar from MainActivity
    private float getSnailMoveStepDegreesFromSetting(String speedLabel) {
        float baseSpeed;

        switch (speedLabel != null ? speedLabel : "Sluggish Crawl") {
            case "Sluggish Crawl":            // ~0.03 m/s
                baseSpeed = 0.000000675f;
                break;
            case "Fast Snail":                // ~0.06 m/s
                baseSpeed = 0.00000135f;
                break;
            case "Turtle Speed":              // ~0.1 m/s
                baseSpeed = 0.00000225f;
                break;
            case "Casual Walk":              // ~1.0 m/s
                baseSpeed = 0.0000225f;
                break;
            case "Power Walk":               // ~1.5 m/s
                baseSpeed = 0.00003375f;
                break;
            case "Jogging Snail":            // ~2.5 m/s
                baseSpeed = 0.00005625f;
                break;
            case "Running Snail":            // ~5.0 m/s
                baseSpeed = 0.0001125f;
                break;
            case "Olympic Sprinting Snail":  // ~10.4 m/s
                baseSpeed = 0.000234f;
                break;
            case "Snail Drives Car":         // ~20.0 m/s
                baseSpeed = 0.00045f;
                break;
            default:                         // Fallback to Sluggish Crawl
                baseSpeed = 0.000000675f;
                break;
        }

        return baseSpeed * (gameTickIntervalMs / 250f); // Scale relative to tick interval
    }

    private boolean checkGameOver() {
        if (snailPosition == null || currentPlayerLocation == null) return false;

        float[] distanceResults = new float[1];
        Location.distanceBetween(
                currentPlayerLocation.latitude, currentPlayerLocation.longitude,
                snailPosition.latitude, snailPosition.longitude,
                distanceResults);
        float distance = distanceResults[0];
        Log.d(TAG, "Distance to snail: " + distance + "m. Game over if < " + gameOverDistanceMeters + "m");
        return distance < gameOverDistanceMeters;
    }
    private void handleGameOver() {
        Log.i(TAG, "Game Over! Snail caught the player.");
        isGameRunning = false;
        // Stop location updates and game loop
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        gameLoopHandler.removeCallbacksAndMessages(null);

        // Broadcast game over event to MainActivity
        Intent gameOverIntent = new Intent(ACTION_GAME_OVER);
        if (snailPosition != null) { // Include final positions if available
            gameOverIntent.putExtra("final_snail_lat", snailPosition.latitude);
            gameOverIntent.putExtra("final_snail_lng", snailPosition.longitude);
        }
        if (currentPlayerLocation != null) {
            gameOverIntent.putExtra("final_player_lat", currentPlayerLocation.latitude);
            gameOverIntent.putExtra("final_player_lng", currentPlayerLocation.longitude);
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(gameOverIntent);

        saveGameState(); // Save final state (game not running)
        stopForeground(true); // Remove notification
        stopSelf(); // Stop the service
    }
    private void updateNotificationText() {
        if (snailPosition != null && currentPlayerLocation != null) {
            float[] distanceResults = new float[1];
            Location.distanceBetween(
                    currentPlayerLocation.latitude, currentPlayerLocation.longitude,
                    snailPosition.latitude, snailPosition.longitude,
                    distanceResults);
            float distance = distanceResults[0];

            String notificationText = String.format("Snail is %.1fm away!", distance);
            Notification notification = createNotification(notificationText);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.notify(NOTIFICATION_ID, notification);
            }
        }
    }

    private void broadcastGameState() {
        if (!isGameRunning) return;

        Intent intent = new Intent(ACTION_GAME_STATE_UPDATE);
        if (snailPosition != null) {
            intent.putExtra("snail_lat", snailPosition.latitude);
            intent.putExtra("snail_lng", snailPosition.longitude);
        }
        if (currentPlayerLocation != null) { // Send current player location from service
            intent.putExtra("player_lat", currentPlayerLocation.latitude);
            intent.putExtra("player_lng", currentPlayerLocation.longitude);
        }
        // Add other game data if needed (e.g., score, power-up status if service manages them)
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        Log.d(TAG, "Game state broadcasted.");
    }
    private void stopGameAndService() {
        Log.d(TAG, "stopGameAndService called");
        isGameRunning = false;
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        gameLoopHandler.removeCallbacksAndMessages(null);
        saveGameState(); // Save that game is not running
        stopForeground(true);
        stopSelf();
    }
    private Notification createNotification(String text) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setAction(Intent.ACTION_MAIN);
        notificationIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        // Important: FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP ensures MainActivity isn't recreated unnecessarily.
        notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);


        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("It Follows")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_snail_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Game Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
    private LatLng moveTowards(LatLng from, LatLng to, float metersPerStep) {
        float[] results = new float[1];
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results);
        float distance = results[0];

        if (distance <= metersPerStep) {
            return to; // Close enough, snap to player
        }

        double fraction = metersPerStep / distance;

        double lat = from.latitude + (to.latitude - from.latitude) * fraction;
        double lng = from.longitude + (to.longitude - from.longitude) * fraction;

        return new LatLng(lat, lng);
    }
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy called");
        stopGameAndService(); // Ensure everything is cleaned up
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not using binding for this example
    }

    // --- Game State Persistence ---
    private void saveGameState() {
        Log.d(TAG, "Saving GameService state. isGameRunning: " + isGameRunning);
        SharedPreferences prefs = getSharedPreferences(PREFS_GAME_SERVICE_STATE, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        if (snailPosition != null) {
            editor.putLong(KEY_SNAIL_LAT, Double.doubleToRawLongBits(snailPosition.latitude));
            editor.putLong(KEY_SNAIL_LNG, Double.doubleToRawLongBits(snailPosition.longitude));
        }
        if (currentPlayerLocation != null) { // Save last known player location
            editor.putLong(KEY_PLAYER_LAT, Double.doubleToRawLongBits(currentPlayerLocation.latitude));
            editor.putLong(KEY_PLAYER_LNG, Double.doubleToRawLongBits(currentPlayerLocation.longitude));
        }
        editor.putBoolean(KEY_IS_GAME_RUNNING, isGameRunning);
        // Save other relevant state
        editor.apply();
    }

    private void loadGameState() {
        Log.d(TAG, "Loading GameService state");
        SharedPreferences prefs = getSharedPreferences(PREFS_GAME_SERVICE_STATE, MODE_PRIVATE);
        isGameRunning = prefs.getBoolean(KEY_IS_GAME_RUNNING, false);

        if (prefs.contains(KEY_SNAIL_LAT) && prefs.contains(KEY_SNAIL_LNG)) {
            snailPosition = new LatLng(
                    Double.longBitsToDouble(prefs.getLong(KEY_SNAIL_LAT, 0)),
                    Double.longBitsToDouble(prefs.getLong(KEY_SNAIL_LNG, 0))
            );
        }
        if (prefs.contains(KEY_PLAYER_LAT) && prefs.contains(KEY_PLAYER_LNG)) {
            currentPlayerLocation = new LatLng(
                    Double.longBitsToDouble(prefs.getLong(KEY_PLAYER_LAT, 0)),
                    Double.longBitsToDouble(prefs.getLong(KEY_PLAYER_LNG, 0))
            );
        }
        Log.d(TAG, "Loaded GameService state. isGameRunning: " + isGameRunning + ", Snail: " + snailPosition + ", Player: " + currentPlayerLocation);
        // Load other state variables
    }

    // Helper to clear saved service state, e.g., when a truly new game starts from scratch
    public static void clearSavedState(Context context) {
        Log.d(TAG, "Clearing saved GameService state.");
        SharedPreferences prefs = context.getSharedPreferences(PREFS_GAME_SERVICE_STATE, MODE_PRIVATE);
        prefs.edit().clear().apply();
    }

    /**
     * Append the given point to the persistent snail trail stored in SharedPreferences.
     * Each point is stored as a JSON object within a JSON array under the key
     * "trailPoints" in the "SnailTrail" preference file.
     */
    private void saveSnailTrailPoint(LatLng newPoint) {
        SharedPreferences trailPrefs = getSharedPreferences("SnailTrail", MODE_PRIVATE);
        String trailJson = trailPrefs.getString("trailPoints", "[]");

        try {
            JSONArray trailArray = new JSONArray(trailJson);
            JSONObject point = new JSONObject();
            point.put("lat", newPoint.latitude);
            point.put("lng", newPoint.longitude);
            trailArray.put(point);

            trailPrefs.edit().putString("trailPoints", trailArray.toString()).apply();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
