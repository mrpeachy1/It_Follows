package com.example.itfollows;

import static com.example.itfollows.GameManager.isNewGame;

import android.Manifest;
import java.util.Calendar;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.widget.LinearLayout;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import androidx.core.app.NotificationCompat;
import android.os.Build;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock; // Using SystemClock for elapsed time
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import android.content.IntentFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import android.widget.ImageButton;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.model.MapStyleOptions;
import java.util.concurrent.TimeUnit;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String KEY_TODAYS_GAME = "TodaysGame";
    private boolean isNightModeActive = false;
    private Marker snailBeaconMarker;
    private LatLng beaconPosition;
    private boolean beaconActive = false;
    private float snailSpeedMultiplier = 1.0f;
    private boolean snailSpeedBoostActive = false;
    private long snailSpeedBoostEndTimeMs = 0;
    private boolean snailInactivityBoostActive = false;
    private long snailInactivityBoostEndTimeMs = 0;
    private static final float INACTIVITY_SPEED_THRESHOLD_MPS = 0.5f;
    private static final long INACTIVITY_DURATION_MS = 30_000; // 30 seconds
    private static final float INACTIVITY_SPEED_MULTIPLIER = 1.5f;
    private static final long INACTIVITY_BOOST_DURATION_MS = 15_000; // 15 seconds
    private long lastMovementTimeMs = 0;
    private long lastSpeedCheckTimeMs = 0;
    private LatLng lastSpeedCheckPosition = null;
    private Handler beaconSpawnHandler = new Handler();
    private Runnable beaconSpawnRunnable;

    private static final String HOLD_MINIGAME_PREFS = "HoldMinigamePrefs";
    private static final String KEY_LAST_HOLD_PLAYED_DATE = "LastHoldMinigamePlayed";
    private static final long TWENTY_FOUR_HOURS = 24 * 60 * 60 * 1000L;
    private static final int REQUEST_CODE_HOLD = 444;
    private GoogleMap mMap;
    private boolean isFollowingPlayer = true;
    private int saltBombCount = 0;
    private int shellShieldCount = 0;
    private int decoyShellCount = 0;
    private Marker playerMarker;

    private static final String MINIGAME_PREFS = "MinigamePrefs";
    private static final String KEY_LAST_PLAYED_DATE = "LastPlayedDate";
    private static final String KEY_TODAYS_TRIGGER_TIME = "TodaysTriggerTime";
    private String currentSnailSpeedSetting = "Normal Chase";
    private boolean useImperial = false;
    private static final String TAG_MAIN_ACTIVITY = "MainActivity";

    private FusedLocationProviderClient fusedLocationClient;

    private SharedPreferences sharedPreferences;
    private boolean isGameServiceActive = false;
    private long gameTickIntervalMillis = 2000;
    private LocationCallback locationCallback;
    // Track whether the activity is currently in the background
    private boolean isInBackground = false;
    // True while a minigame activity is being played
    private boolean isPlayingMinigame = false;
    private LatLng currentPlayerLocation;
    // private Marker playerMarker; // Player marker is usually the blue dot from setMyLocationEnabled
    private Marker snailMarker;
    private LatLng snailPosition;
    private boolean isGameOver = false;
    private final Handler snailHandler = new Handler();
    private boolean hasSpawnedSnail = false;
    private boolean hasCenteredOnce = false;
    private TextView snailDistanceText;
    private float gameOverDistanceMeters; // DECLARE THE VARIABLE HERE
    private Polyline snailTrail;
    private List<LatLng> snailTrailPoints;
    private static final int MAX_TRAIL_POINTS = 100000;
    private static final String PREFS_NAME = "GameSettings";
    public static final String KEY_SELECTED_SNAIL_SPRITE = "selectedSnailSprite";
    public static final String DEFAULT_SNAIL_SPRITE_IDENTIFIER = "snail_classic";
    private String loadedSnailSpriteIdentifier;
    private static final double INTERPOLATION_STEP_DEGREES = 0.00005; // How granular the trail jump is

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private float distanceSinceLastReward = 0f;
    private static final float REWARD_DISTANCE_THRESHOLD_METERS = 5000f; // Adjust as needed
    private LatLng lastRewardCheckLocation = null;
    private long gameStartTimeElapsedMillis; // Using SystemClock.elapsedRealtime()
    private float totalSnailDistanceMeters;
    public static final int REQUEST_CODE_GAME_OVER = 1;

    // --- Pause State Variables ---
    private LatLng snailPositionBeforePause;
    private LatLng playerPositionBeforePause;
    private long timePausedElapsedMillis; // Using SystemClock.elapsedRealtime()
    private float totalSnailDistanceBeforePause;

    private static final String PREFS_GAME_STATE = "SnailGameState";
    private static final String KEY_SNAIL_LAT_BEFORE_PAUSE = "snailLatBeforePause";
    private static final String KEY_SNAIL_LNG_BEFORE_PAUSE = "snailLngBeforePause";
    private static final String KEY_PLAYER_LAT_BEFORE_PAUSE = "playerLatBeforePause";
    private static final String KEY_PLAYER_LNG_BEFORE_PAUSE = "playerLngBeforePause";
    private static final String KEY_TIME_PAUSED_ELAPSED = "timePausedElapsedMillis";
    private static final String KEY_SNAIL_DISTANCE_BEFORE_PAUSE = "snailDistanceBeforePause";
    private static final String KEY_GAME_START_TIME_ELAPSED = "gameStartTimeElapsedMillis";
    private static final String KEY_SNAIL_HAS_SPAWNED_ON_PAUSE = "snailHasSpawnedOnPause";
    private static final String KEY_HAS_SAVED_GAME = "hasSavedGame";
    private RelativeLayout inventoryPanel;
    private Button inventoryButton, useSaltBombBtn;
    private TextView saltBombLabel;
    private SharedPreferences powerUpPrefs;
    private SharedPreferences.Editor powerUpEditor;
    private boolean isSaltBombActive = false;
    private long saltBombEndTimeMs = 0;
    private Button useDecoyBtn;
    private LatLng playerPosition;
    private TextView decoyLabel;
    private boolean isDecoyActive = false;
    private long decoyEndTimeMs = 0;
    private LatLng decoyPosition = null;
    private Button useShieldBtn;
    private TextView shieldLabel;
    private boolean isShieldActive = false;
    private int beaconSpawnCount = 0;
    private boolean isSaltBombOnCooldown = false;
    private final long saltBombCooldownMillis = 30_000; // 30 seconds
    private CountDownTimer saltBombCooldownTimer;
    private boolean isDecoyShellOnCooldown = false;
    private final long decoyShellCooldownMillis = 45_000; // 45 sec cooldown (adjust as needed)
    private static final long updateIntervalMs = 250; // 250 milliseconds per game tick
    private CountDownTimer decoyShellCooldownTimer;
    private boolean isShellShieldOnCooldown = false;
    private final long shellShieldCooldownMillis = 60_000; // 60 seconds
    private Handler repelCooldownHandler = new Handler();
    private Runnable repelCooldownRunnable;
    private CountDownTimer shellShieldCooldownTimer;
    private long shieldShieldEndTimeMs;
    private boolean isDecoyShellActive;
    private float snailDegreesPerMillisecond = 0f;
    private Handler snailAbilityHandler = new Handler();
    private Runnable snailAbilityRunnable;
    private final long ABILITY_INTERVAL_MS = 3 * 60 * 1000; // Every 3 minutes
    private Marker fakeSnailMarker;
    private Handler fakeSnailHandler;
    private Runnable fakeSnailRunnable;
    private long fakeSnailStartTime;
    private static final long FAKE_SNAIL_DURATION_MS = 60_000;
    private static final float FAKE_SNAIL_MOVE_METERS_PER_SECOND = 0.2f; // tweak this as needed

    private boolean snailInvisible = false;
    // --- Shell Split Ability ---
    private boolean shellSplitActive = false;
    private Marker splitMarker1, splitMarker2;
    private LatLng splitPos1, splitPos2;
    private long shellSplitEndTimeMs;
    private static final long SHELL_SPLIT_DURATION_MS = 60 * 60 * 1000L; // 60 minutes

    private void updateSnailIcon() {
        // Implement the logic to update the snail's icon here.
        // For example, if you want to change the snailMarker's icon:
        if (mMap != null && snailMarker != null) {
            // Assuming you have a way to get the new BitmapDescriptor
            // For example, based on the loadedSnailSpriteIdentifier
            BitmapDescriptor newIcon = getSnailBitmapDescriptor(loadedSnailSpriteIdentifier);
            if (newIcon != null) {
                snailMarker.setIcon(newIcon);
            } else {
                Log.e("UpdateSnailIcon", "Could not get new snail icon.");
            }
        }
    }
    private void checkAndActivateNightMode() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        if (hour >= 20) { // After 8 PM
            isNightModeActive = true;
            applyNightMapStyle();             // <- this should call it
            increaseSnailSpeedForNight();     // optional
        }
    }

    private void increaseSnailSpeedForNight() {
        snailSpeedMultiplier = 1.5f; // Or increase snail speed value directly
    }
    private boolean isInSleepHours() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        return hour >= 20 || hour < 6;
    }
    private void spawnSnailBeacon() {
        if (playerPosition == null || mMap == null) return;

        beaconSpawnCount++; // 🧮 Track how many beacons have been spawned
        double beaconRadiusMeters = 100.0 + (beaconSpawnCount * 20); // 🔁 Expanding radius

        double randomBearing = Math.toRadians(new Random().nextInt(360));
        double latOffset = beaconRadiusMeters / 111111.0 * Math.cos(randomBearing);
        double lngOffset = beaconRadiusMeters / (111111.0 * Math.cos(Math.toRadians(playerPosition.latitude))) * Math.sin(randomBearing);
        beaconPosition = new LatLng(playerPosition.latitude + latOffset, playerPosition.longitude + lngOffset);

        if (snailBeaconMarker != null) {
            snailBeaconMarker.remove(); // Remove old one just in case
        }

        snailBeaconMarker = mMap.addMarker(new MarkerOptions()
                .position(beaconPosition)
                .title("Snail Beacon")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
        beaconActive = true;
    }

    private void checkBeaconCapture() {
        if (!beaconActive || snailPosition == null || playerPosition == null || beaconPosition == null) return;

        float playerDist = (float) distanceBetween(
                playerPosition.latitude, playerPosition.longitude,
                beaconPosition.latitude, beaconPosition.longitude);

        float snailDist = (float) distanceBetween(
                snailPosition.latitude, snailPosition.longitude,
                beaconPosition.latitude, beaconPosition.longitude);


        if (playerDist < 10f) {
            beaconActive = false;
            snailBeaconMarker.remove();
            pushSnailBack(50);
            Toast.makeText(this, "You reached the beacon! The snail is pushed back.", Toast.LENGTH_SHORT).show();
            scheduleBeaconSpawn();
        } else if (snailDist < 10f) {
            beaconActive = false;
            snailBeaconMarker.remove();
            boostSnailSpeedTemporarily();
            Toast.makeText(this, "The snail consumed the beacon! It moves faster...", Toast.LENGTH_SHORT).show();
            scheduleBeaconSpawn();
        }
    }

    private void scheduleBeaconSpawn() {
        long minDelay = TimeUnit.HOURS.toMillis(1);
        long maxDelay = TimeUnit.HOURS.toMillis(3);
        long delay = minDelay + (long) (Math.random() * (maxDelay - minDelay));
        if (beaconSpawnRunnable == null) {
            beaconSpawnRunnable = () -> {
                spawnSnailBeacon();
                scheduleBeaconSpawn();
            };
        }
        beaconSpawnHandler.removeCallbacks(beaconSpawnRunnable);
        beaconSpawnHandler.postDelayed(beaconSpawnRunnable, delay);
    }

    private void applyNightMapStyle() {
        try {
            boolean success = mMap.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.night_map_style)
            );
            if (!success) {
                Log.e("MapStyle", "Style parsing failed.");
            }
        } catch (Resources.NotFoundException e) {
            Log.e("MapStyle", "Can't find style. Error: ", e);
        }
    }

    private void startSaltBombCooldownUI() {
        saltBombCooldownTimer = new CountDownTimer(saltBombCooldownMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000;
                useSaltBombBtn.setText("Recharging... " + secondsLeft + "s");
            }

            public void onFinish() {
                isSaltBombOnCooldown = false;
                useSaltBombBtn.setEnabled(true);
                useSaltBombBtn.setText("Use Salt Bomb");
            }
        }.start();
    }

    private BitmapDescriptor getSnailBitmapDescriptor(String spriteIdentifier) {
        if (spriteIdentifier == null) return null; // Or return a default

        int resourceId = getResources().getIdentifier(spriteIdentifier, "drawable", getPackageName());
        if (resourceId != 0) {
            return BitmapDescriptorFactory.fromResource(resourceId);
        } else {
            Log.w("SnailIcon", "Snail sprite resource not found for: " + spriteIdentifier + ". Using default.");
            // Fallback to a default if the specific one isn't found
            return BitmapDescriptorFactory.fromResource(R.drawable.snail); // Ensure snail_classic exists
        }
    }

    private void spawnSnailAtRandomLocation() {
        // TODO: Implement the logic to spawn the snail at a random location.
        // This might involve:
        // 1. Generating random LatLng coordinates (within reasonable bounds, perhaps near the player).
        // 2. Setting the initial 'snailPosition'.
        // 3. Creating or updating the 'snailMarker' on the map.
        // 4. Setting 'hasSpawnedSnail' to true.

        // Example (very basic, you'll need to refine this):
        if (mMap != null && currentPlayerLocation != null) {
            Random random = new Random();
            // Spawn snail within a certain radius of the player, for example
            double latOffset = (random.nextDouble() - 0.5) * 0.01; // Adjust range as needed
            double lngOffset = (random.nextDouble() - 0.5) * 0.01; // Adjust range as needed
            snailPosition = new LatLng(currentPlayerLocation.latitude + latOffset, currentPlayerLocation.longitude + lngOffset);

            if (snailMarker == null) {
                BitmapDescriptor snailIcon = getSnailBitmapDescriptor(loadedSnailSpriteIdentifier);
                if (snailIcon == null) { // Fallback if custom sprite isn't found
                    snailIcon = BitmapDescriptorFactory.fromResource(R.drawable.snail); // Ensure you have a default
                }
                snailMarker = mMap.addMarker(new MarkerOptions()
                        .position(snailPosition)
                        .title("Snail")
                        .icon(snailIcon));
                Log.d("SnailMarker", "Created snail marker at: " + snailPosition);
            } else {
                snailMarker.setPosition(snailPosition);

            }
            hasSpawnedSnail = true;
            Log.d(TAG_MAIN_ACTIVITY, "Snail spawned at: " + snailPosition);
        } else {
            Log.e(TAG_MAIN_ACTIVITY, "Cannot spawn snail: Map or player location not available.");
            // Optionally, try to spawn later or use a default location
        }
    }

    private double distanceBetween(double lat1, double lng1, double lat2, double lng2) {
        float[] results = new float[1];
        Location.distanceBetween(lat1, lng1, lat2, lng2, results);
        return results[0]; // distance in meters
    }

    private int getSnailCoinBalance() {
        SharedPreferences prefs = getSharedPreferences("SnailGameState", MODE_PRIVATE); // ✅ MATCHES updateSnailCoinBalance
        return prefs.getInt("snailCoins", 1_000_000); // default to 1 million only once
    }

    private TextView coinBalanceText;
    private LinearLayout shopPanel;
    private int snailCoinBalance;
    private void updateCoinDisplay() {
        coinBalanceText.setText("💰 Snail Coins: " + snailCoinBalance);
    }
    private int selectedRepelDistance = 100;
    private static final int MIN_REPEL = 50;
    private static final int MAX_REPEL = 200;
    private static final int REPEL_INCREMENT = 50;
    private static final long REPEL_COOLDOWN_MS = 86_400_000L; // 24 hours
    private static final long SHELL_SWAP_COOLDOWN_MS = 86_400_000L; // 24 hours
    private void updateSnailCoinBalance(int newAmount) {
        snailCoinBalance = newAmount;
        SharedPreferences.Editor editor = getSharedPreferences("SnailGameState", MODE_PRIVATE).edit();
        editor.putInt("snailCoins", snailCoinBalance);
        editor.apply();
        updateCoinDisplay();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG_MAIN_ACTIVITY, "onCreate called");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        SharedPreferences statePrefs = getSharedPreferences(PREFS_GAME_STATE, MODE_PRIVATE);
        boolean hasSavedGame = statePrefs.getBoolean(KEY_HAS_SAVED_GAME, false)
                && statePrefs.contains(KEY_SNAIL_LAT_BEFORE_PAUSE)
                && statePrefs.contains(KEY_PLAYER_LAT_BEFORE_PAUSE);
        boolean isNewGame = getIntent().getBooleanExtra("isNewGame", false); // safer default

        Log.d("MainActivity", "isNewGame = " + isNewGame);
        Log.d("MainActivity", "hasSavedSnail = " + hasSavedGame);
        if (!hasSavedGame) {
            Log.d("MainActivity", "No saved state found. Proceeding with isNewGame = " + isNewGame);
        } else {
            Log.d("MainActivity", "Saved snail state detected. Forcing isNewGame = false");
            isNewGame = false; // 👈 OVERRIDE to prevent false reset
        }
        if (isNewGame && !hasSavedGame) {
            // Fresh game setup. Snail will spawn once map and player location are ready.
            statePrefs.edit().clear().apply();
            getSharedPreferences("PowerUpInventory", MODE_PRIVATE).edit().clear().apply();
            resetLocalGameState();
            Log.d("MainActivity", "New game: reset state. Snail will spawn when ready.");
        } else if (hasSavedGame) {
            // Resume previous state but delay chase until map/location available
            loadGameState();
            Log.d("MainActivity", "Loaded saved game state.");
        }
        getSharedPreferences("PowerUpCooldowns", MODE_PRIVATE).edit().clear().apply(); // ✅ Reset cooldowns

        powerUpPrefs = getSharedPreferences("PowerUpInventory", MODE_PRIVATE);
        powerUpEditor = powerUpPrefs.edit();
        updatePowerUpUI();

        SharedPreferences currencyPrefs = getSharedPreferences("snailCoins", MODE_PRIVATE);
        SharedPreferences.Editor currencyEditor = currencyPrefs.edit();

        if (!currencyPrefs.contains("snailCoins")) {
            currencyEditor.putInt("snailCoins", 1_000_000);
            currencyEditor.apply();
        }

        snailCoinBalance = getSharedPreferences("SnailGameState", MODE_PRIVATE)
                .getInt("snailCoins", 1_000_000);

        shopPanel = findViewById(R.id.shopPanel);
        coinBalanceText = findViewById(R.id.coinBalanceText);
        updateCoinDisplay();

        ImageButton shopToggle = findViewById(R.id.shopToggleButton);
        shopToggle.setOnClickListener(v -> {
            if (shopPanel.getVisibility() == View.VISIBLE) {
                shopPanel.setVisibility(View.GONE);
            } else {
                updateCoinDisplay();
                shopPanel.setVisibility(View.VISIBLE);
                shopPanel.bringToFront();
            }
        });

        Button buySaltBombBtn = findViewById(R.id.buySaltBombBtn);
        buySaltBombBtn.setOnClickListener(v -> {
            int currentCount = powerUpPrefs.getInt("saltBomb", 0);
            if (currentCount >= 5) {
                Toast.makeText(this, "Max Salt Bombs reached!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (getSnailCoinBalance() >= 5000) {
                updateSnailCoinBalance(getSnailCoinBalance() - 5000);
                powerUpEditor.putInt("saltBomb", currentCount + 1);
                powerUpEditor.apply();
                updatePowerUpUI();
                Toast.makeText(this, "Purchased Salt Bomb!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Not enough Snail Coins!", Toast.LENGTH_SHORT).show();
            }
        });

        Button buyDecoyBtn = findViewById(R.id.buyDecoyBtn);
        buyDecoyBtn.setOnClickListener(v -> {
            int currentCount = powerUpPrefs.getInt("decoyShell", 0);
            if (currentCount >= 5) {
                Toast.makeText(this, "Max Decoy Shells reached!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (getSnailCoinBalance() >= 7500) {
                updateSnailCoinBalance(getSnailCoinBalance() - 7500);
                powerUpEditor.putInt("decoyShell", currentCount + 1);
                powerUpEditor.apply();
                updatePowerUpUI();
                Toast.makeText(this, "Purchased Decoy Shell!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Not enough Snail Coins!", Toast.LENGTH_SHORT).show();
            }
        });

        Button buyShieldBtn = findViewById(R.id.buyShieldBtn);
        buyShieldBtn.setOnClickListener(v -> {
            int currentCount = powerUpPrefs.getInt("shellShield", 0);
            if (currentCount >= 1) {
                Toast.makeText(this, "You already have a Shell Shield!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (getSnailCoinBalance() >= 10000) {
                updateSnailCoinBalance(getSnailCoinBalance() - 10000);
                powerUpEditor.putInt("shellShield", currentCount + 1);
                powerUpEditor.apply();
                updatePowerUpUI();
                Toast.makeText(this, "Purchased Shell Shield!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Not enough Snail Coins!", Toast.LENGTH_SHORT).show();
            }
        });
        Button buyWhistleBtn = findViewById(R.id.buyWhistleBtn);
        buyWhistleBtn.setOnClickListener(v -> {
            if (getSnailCoinBalance() >= 3000) {
                updateSnailCoinBalance(getSnailCoinBalance() - 3000);
                lureSnailToRandomLocation();
                Toast.makeText(this, "You blew the Snail Whistle!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Not enough Snail Coins!", Toast.LENGTH_SHORT).show();
            }
        });
        Button buyShellSwapBtn = findViewById(R.id.buyShellSwapBtn);
        buyShellSwapBtn.setOnClickListener(v -> {
            SharedPreferences cooldownPrefs = getSharedPreferences("PowerUpCooldowns", MODE_PRIVATE);
            long lastUsed = cooldownPrefs.getLong("shellSwapLastUsed", 0);
            if (System.currentTimeMillis() - lastUsed < SHELL_SWAP_COOLDOWN_MS) {
                Toast.makeText(this, "Shell Swap is recharging...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (getSnailCoinBalance() >= 15000) {
                updateSnailCoinBalance(getSnailCoinBalance() - 15000);
                performShellSwap();
                cooldownPrefs.edit().putLong("shellSwapLastUsed", System.currentTimeMillis()).apply();
                startShellSwapCooldownUI(buyShellSwapBtn);
                Toast.makeText(this, "You swapped shells with the snail!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Not enough Snail Coins!", Toast.LENGTH_SHORT).show();
            }
        });
        startShellSwapCooldownUI(buyShellSwapBtn);
        Button buttonQuitYes = findViewById(R.id.buttonQuitYes);
        Button buttonQuitNo = findViewById(R.id.buttonQuitNo);

        buttonQuitYes.setOnClickListener(v -> {
            // Stop the foreground service completely and clear any persisted data
            stopService(new Intent(this, GameService.class));
            GameService.clearSavedState(this);

            // Clear any saved game progress so a new game starts fresh
            clearGameStatePrefs();

            Intent intent = new Intent(this, MainMenuActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish(); // Exit this activity
        });

        buttonQuitNo.setOnClickListener(v -> {
            LinearLayout quitConfirmPanel = findViewById(R.id.quitConfirmPanel);
            quitConfirmPanel.setVisibility(View.GONE);
        });

        // Zoom button setup
        ImageButton zoomFitButton = findViewById(R.id.zoomFitButton);
        zoomFitButton.setOnClickListener(v -> {
            if (mMap == null || currentPlayerLocation == null || snailPosition == null) {
                Toast.makeText(this, "Waiting for locations...", Toast.LENGTH_SHORT).show();
                return;
            }

            // Re-enable following the player and zoom out enough so the snail is visible
            isFollowingPlayer = true;
            zoomOutToShowSnailButKeepPlayerCentered();
        });



        sharedPreferences = getSharedPreferences("GameSettings", MODE_PRIVATE);
        String loadedSnailSpriteIdentifier = sharedPreferences.getString("snailSprite", "snail_classic");
        useImperial = "Imperial".equals(sharedPreferences.getString(SettingsActivity.KEY_MEASUREMENT_UNIT, "Metric"));
        RelativeLayout myContainer = (RelativeLayout) findViewById(R.id.my_container);
        powerUpPrefs = getSharedPreferences("PowerUpInventory", MODE_PRIVATE);
        powerUpEditor = powerUpPrefs.edit();

        useDecoyBtn = findViewById(R.id.useDecoyBtn);
        decoyLabel = findViewById(R.id.decoyLabel);
        useShieldBtn = findViewById(R.id.useShieldBtn);
        shieldLabel = findViewById(R.id.shieldLabel);

        loadSelectedSnailSprite();
        checkIfMinigameShouldTrigger();
        LinearLayout minigameMenuPanel = findViewById(R.id.minigameMenuPanel);

        Button playSlimeGameBtn = findViewById(R.id.playSlimeGameBtn);
        Button playCoinFlipBtn = findViewById(R.id.playCoinFlipBtn);
        Button playHoldMinigameBtn = findViewById(R.id.playHoldMinigameBtn);

        playHoldMinigameBtn.setOnClickListener(v -> {
        Intent intent = new Intent(MainActivity.this, HoldToSurviveMinigameActivity.class);
        isPlayingMinigame = true;
        startActivityForResult(intent, 444); // use unique requestCode
        });

        playSlimeGameBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SlimeTapMinigameActivity.class);
            isPlayingMinigame = true;
            startActivityForResult(intent, 222);
        });

        playCoinFlipBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CoinFlipMinigameActivity.class);
            isPlayingMinigame = true;
            startActivityForResult(intent, 333);
        });


        useShieldBtn.setOnClickListener(v -> {
            int count = powerUpPrefs.getInt("shellShield", 0);

            if (isShellShieldOnCooldown) {
                Toast.makeText(this, "Shell Shield is recharging...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (count > 0) {
                powerUpEditor.putInt("shellShield", count - 1);
                powerUpEditor.apply();
                activateShellShield(); // Define this to enable the shield behavior
                updatePowerUpUI();
                Toast.makeText(this, "Shell Shield activated!", Toast.LENGTH_SHORT).show();

                isShellShieldOnCooldown = true;
                useShieldBtn.setEnabled(false);
                startShellShieldCooldownUI(); // Start cooldown + button feedback
            } else {
                Toast.makeText(this, "No Shell Shields left!", Toast.LENGTH_SHORT).show();
            }
        });

        if (snailTrailPoints == null) {
            snailTrailPoints = new ArrayList<>();
        } else {
            snailTrailPoints.clear();
        }

        useDecoyBtn.setOnClickListener(v -> {
            int count = powerUpPrefs.getInt("decoyShell", 0);

            if (isDecoyShellOnCooldown) {
                Toast.makeText(this, "Decoy Shell is recharging...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (count > 0) {
                powerUpEditor.putInt("decoyShell", count - 1);
                powerUpEditor.apply();
                activateDecoyShell(); // make sure this exists
                updatePowerUpUI();
                Toast.makeText(this, "Decoy Shell deployed!", Toast.LENGTH_SHORT).show();

                isDecoyShellOnCooldown = true;
                useDecoyBtn.setEnabled(false);
                startDecoyShellCooldownUI(); // ⬅️ Call cooldown
            } else {
                Toast.makeText(this, "No Decoy Shells left!", Toast.LENGTH_SHORT).show();
            }
        });

        inventoryButton = findViewById(R.id.inventoryButton);
        inventoryPanel = findViewById(R.id.inventoryPanel);
        useSaltBombBtn = findViewById(R.id.useSaltBombBtn);
        saltBombLabel = findViewById(R.id.saltBombLabel);

        inventoryButton.setOnClickListener(v -> {
            if (inventoryPanel.getVisibility() == View.VISIBLE) {
                inventoryPanel.setVisibility(View.GONE);
            } else {
                updatePowerUpUI(); // Update UI before showing
                inventoryPanel.setVisibility(View.VISIBLE);
                inventoryPanel.bringToFront(); // Ensure it's on top
            }
        });

        useSaltBombBtn.setOnClickListener(v -> {
            int count = powerUpPrefs.getInt("saltBomb", 0);

            if (isSaltBombOnCooldown) {
                Toast.makeText(this, "Salt Bomb is recharging...", Toast.LENGTH_SHORT).show();
                return;
            }

            if (count > 0 && !isSaltBombActive) {
                // Use and activate power-up
                powerUpEditor.putInt("saltBomb", count - 1);
                powerUpEditor.apply();
                activateSaltBomb();
                updatePowerUpUI();
                Log.d("InventoryCheck", "SaltBomb: " + powerUpPrefs.getInt("saltBomb", -1));
                Log.d("InventoryCheck", "Decoy: " + powerUpPrefs.getInt("decoyShell", -1));
                Log.d("InventoryCheck", "Shield: " + powerUpPrefs.getInt("shellShield", -1));

                Toast.makeText(this, "Salt Bomb activated!", Toast.LENGTH_SHORT).show();

                // Start cooldown
                isSaltBombOnCooldown = true;
                useSaltBombBtn.setEnabled(false);
                startSaltBombCooldownUI(); // 👇 Shows countdown
            } else if (isSaltBombActive) {
                Toast.makeText(this, "Already active!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No Salt Bombs left!", Toast.LENGTH_SHORT).show();
            }
        });
        if (playerPosition != null && snailPosition != null) {
            spawnSnailBeacon();
        }
        scheduleBeaconSpawn();
        snailDistanceText = findViewById(R.id.snailDistanceText); // Ensure this ID exists
        snailTrailPoints = new ArrayList<>();

        SharedPreferences gameSettingsPrefs = getSharedPreferences("GameSettings", MODE_PRIVATE);
        currentSnailSpeedSetting = gameSettingsPrefs.getString("snailSpeed", "Normal Chase");
        String snailDistanceTextSetting = gameSettingsPrefs.getString("snailDistance", "Distant");

        float snailMetersPerSecond = getSnailMetersPerSecond(currentSnailSpeedSetting) * snailSpeedMultiplier;
        float snailMetersPerMillisecond = snailMetersPerSecond / 1000f;
        snailDegreesPerMillisecond = snailMetersPerMillisecond / 111_111f; // ~degrees/ms

        Log.d("DEBUG_PREF", "Snail speed selected in onCreate: " + currentSnailSpeedSetting);
        Log.d("DEBUG_PREF", "Snail distance selected: " + snailDistanceTextSetting);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map); // Ensure this ID exists
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        setupLocationCallback(snailDistanceTextSetting);
        setupSnailRepelUI();
    }
    private void setupSnailRepelUI() {
        TextView repelDistanceText = findViewById(R.id.repelDistanceText);
        TextView repelCostText = findViewById(R.id.repelCostText);
        TextView repelCooldownText = findViewById(R.id.repelCooldownText); // 👈 Moved here
        Button increaseBtn = findViewById(R.id.increaseRepelBtn);
        Button decreaseBtn = findViewById(R.id.decreaseRepelBtn);
        Button buyBtn = findViewById(R.id.buyRepelBtn);

        updateRepelUI(repelDistanceText, repelCostText);

        increaseBtn.setOnClickListener(v -> {
            if (selectedRepelDistance < MAX_REPEL) {
                selectedRepelDistance += REPEL_INCREMENT;
                updateRepelUI(repelDistanceText, repelCostText);
            }
        });

        decreaseBtn.setOnClickListener(v -> {
            if (selectedRepelDistance > MIN_REPEL) {
                selectedRepelDistance -= REPEL_INCREMENT;
                updateRepelUI(repelDistanceText, repelCostText);
            }
        });

        buyBtn.setOnClickListener(v -> {
            int cost = calculateRepelCost(selectedRepelDistance);
            SharedPreferences cooldownPrefs = getSharedPreferences("PowerUpCooldowns", MODE_PRIVATE);
            long lastUsed = cooldownPrefs.getLong("repelLastUsed", 0);

            if (System.currentTimeMillis() - lastUsed < REPEL_COOLDOWN_MS) {
                Toast.makeText(this, "Snail Repel is still recharging.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (getSnailCoinBalance() >= cost) {
                updateSnailCoinBalance(getSnailCoinBalance() - cost);
                applySnailRepel(selectedRepelDistance);
                cooldownPrefs.edit().putLong("repelLastUsed", System.currentTimeMillis()).apply();

                // 🟩 START the cooldown UI here after purchase
                startSnailRepelCooldownUI(buyBtn, repelCooldownText);

                String unitMsg = useImperial ?
                        String.format(Locale.US, "Snail repelled %.0f feet!", selectedRepelDistance * 3.28084) :
                        "Snail repelled " + selectedRepelDistance + " meters!";
                Toast.makeText(this, unitMsg, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Not enough Snail Coins!", Toast.LENGTH_SHORT).show();
            }
        });
        startSnailRepelCooldownUI(buyBtn, repelCooldownText);
    }
    private void startSnailRepelCooldownUI(Button repelButton, TextView cooldownText) {
        long lastUsed = getSharedPreferences("PowerUpCooldowns", MODE_PRIVATE)
                .getLong("repelLastUsed", 0);
        long elapsed = System.currentTimeMillis() - lastUsed;
        long remaining = REPEL_COOLDOWN_MS - elapsed;

        if (remaining > 0) {
            repelButton.setEnabled(false);
            repelButton.setAlpha(0.5f);
            cooldownText.setVisibility(View.VISIBLE);

            new CountDownTimer(remaining, 1000) {
                public void onTick(long millisUntilFinished) {
                    long seconds = (millisUntilFinished / 1000) % 60;
                    long minutes = (millisUntilFinished / 1000 / 60) % 60;
                    long hours = (millisUntilFinished / 1000 / 60 / 60);
                    String timeLeft = String.format("Cooldown: %02dh %02dm %02ds", hours, minutes, seconds);

                    repelButton.setText("Recharging... " + hours + "h " + minutes + "m " + seconds + "s");
                    cooldownText.setText(timeLeft);
                }

                public void onFinish() {
                    repelButton.setEnabled(true);
                    repelButton.setAlpha(1f);
                    repelButton.setText("Buy Snail Repel");
                    cooldownText.setVisibility(View.GONE);
                }
            }.start();
        } else {
            repelButton.setEnabled(true);
            repelButton.setAlpha(1f);
            repelButton.setText("Buy Snail Repel");
            cooldownText.setVisibility(View.GONE);
        }
    }



    private void updateRepelUI(TextView distText, TextView costText) {
        if (useImperial) {
            double feet = selectedRepelDistance * 3.28084;
            distText.setText(String.format(Locale.US, "%.0fft", feet));
        } else {
            distText.setText(selectedRepelDistance + "m");
        }
        costText.setText("$" + String.format("%,d", calculateRepelCost(selectedRepelDistance)));
    }

    private int calculateRepelCost(int distance) {
        switch (distance) {
            case 50: return 25000;
            case 100: return 60000;
            case 150: return 120000;
            case 200: return 200000;
            default: return 0;
        }
    }

    private void applySnailRepel(int meters) {
        if (snailPosition == null || playerPosition == null) return;

        double bearing = getBearing(playerPosition, snailPosition);
        double distance = distanceBetween(playerPosition, snailPosition);
        double newTotalDistance = distance + meters;
        LatLng newSnailPos = moveAwayFrom(playerPosition, bearing, newTotalDistance);

        snailPosition = newSnailPos;
        if (snailMarker != null) snailMarker.setPosition(snailPosition);
    }
    private void lureSnailToRandomLocation() {
        if (mMap == null || currentPlayerLocation == null) return;
        Random random = new Random();
        double latOffset = (random.nextDouble() - 0.5) * 0.02; // ~2km range
        double lngOffset = (random.nextDouble() - 0.5) * 0.02;
        snailPosition = new LatLng(currentPlayerLocation.latitude + latOffset,
                currentPlayerLocation.longitude + lngOffset);
        if (snailMarker != null) snailMarker.setPosition(snailPosition);
    }
    private void performShellSwap() {
        if (snailPosition == null || currentPlayerLocation == null) return;

        LatLng oldSnailPos = snailPosition;
        LatLng oldPlayerPos = currentPlayerLocation;

        snailPosition = oldPlayerPos;
        if (snailMarker != null) snailMarker.setPosition(snailPosition);

        currentPlayerLocation = oldSnailPos;
        playerPosition = oldSnailPos;
        if (playerMarker != null) playerMarker.setPosition(oldSnailPos);
        if (mMap != null) mMap.animateCamera(CameraUpdateFactory.newLatLng(oldSnailPos));
    }

    private void startShellSwapCooldownUI(Button swapButton) {
        long lastUsed = getSharedPreferences("PowerUpCooldowns", MODE_PRIVATE)
                .getLong("shellSwapLastUsed", 0);
        long elapsed = System.currentTimeMillis() - lastUsed;
        long remaining = SHELL_SWAP_COOLDOWN_MS - elapsed;

        if (remaining > 0) {
            swapButton.setEnabled(false);
            swapButton.setAlpha(0.5f);

            new CountDownTimer(remaining, 1000) {
                public void onTick(long millisUntilFinished) {
                    long seconds = (millisUntilFinished / 1000) % 60;
                    long minutes = (millisUntilFinished / 1000 / 60) % 60;
                    long hours = (millisUntilFinished / 1000 / 60 / 60);
                    swapButton.setText("Recharging... " + hours + "h " + minutes + "m " + seconds + "s");
                }

                public void onFinish() {
                    swapButton.setEnabled(true);
                    swapButton.setAlpha(1f);
                    swapButton.setText("🔀 Shell Swap (15,000)");
                }
            }.start();
        } else {
            swapButton.setEnabled(true);
            swapButton.setAlpha(1f);
            swapButton.setText("🔀 Shell Swap (15,000)");
        }
    }
    private double getBearing(LatLng from, LatLng to) {
        double lat1 = Math.toRadians(from.latitude);
        double lon1 = Math.toRadians(from.longitude);
        double lat2 = Math.toRadians(to.latitude);
        double lon2 = Math.toRadians(to.longitude);
        double dLon = lon2 - lon1;

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);

        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    private LatLng moveAwayFrom(LatLng from, double bearing, double meters) {
        double R = 6371000.0; // Earth radius in meters
        double bearingRad = Math.toRadians(bearing);

        double lat1 = Math.toRadians(from.latitude);
        double lon1 = Math.toRadians(from.longitude);

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(meters / R)
                + Math.cos(lat1) * Math.sin(meters / R) * Math.cos(bearingRad));
        double lon2 = lon1 + Math.atan2(
                Math.sin(bearingRad) * Math.sin(meters / R) * Math.cos(lat1),
                Math.cos(meters / R) - Math.sin(lat1) * Math.sin(lat2));

        return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    private double distanceBetween(LatLng from, LatLng to) {
        float[] results = new float[1];
        Location.distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude, results);
        return results[0];
    }
    private void checkIfMinigameShouldTrigger() {
        SharedPreferences prefs = getSharedPreferences(MINIGAME_PREFS, MODE_PRIVATE);
        long currentTime = System.currentTimeMillis();

        long lastPlayedDate = prefs.getLong(KEY_LAST_PLAYED_DATE, 0);
        long triggerTime = prefs.getLong(KEY_TODAYS_TRIGGER_TIME, -1);
        String todaysGame = prefs.getString(KEY_TODAYS_GAME, null);

        // Get start of today
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long todayStart = today.getTimeInMillis();

        // If new day, reset trigger time and game
        if (lastPlayedDate < todayStart) {
            int randomHour = 8 + (int)(Math.random() * 14); // 8 AM to 9 PM
            int randomMinute = (int)(Math.random() * 60);

            Calendar trigger = Calendar.getInstance();
            trigger.set(Calendar.HOUR_OF_DAY, randomHour);
            trigger.set(Calendar.MINUTE, randomMinute);
            trigger.set(Calendar.SECOND, 0);
            trigger.set(Calendar.MILLISECOND, 0);

            long newTriggerTime = trigger.getTimeInMillis();

            // Randomly choose which minigame to assign
            String chosenGame = Math.random() < 0.5 ? "slime" : "coin";

            prefs.edit()
                    .putLong(KEY_TODAYS_TRIGGER_TIME, newTriggerTime)
                    .putString(KEY_TODAYS_GAME, chosenGame)
                    .putLong(KEY_LAST_PLAYED_DATE, 0) // reset play flag
                    .apply();

            triggerTime = newTriggerTime;
            todaysGame = chosenGame;
        }

        // If it's time to trigger and not already played
        if (currentTime >= triggerTime && lastPlayedDate < todayStart && todaysGame != null) {
            Intent intent;
            Class<?> activityClass;
            int requestCode;

            if (todaysGame.equals("slime")) {
                activityClass = SlimeTapMinigameActivity.class;
                intent = new Intent(this, SlimeTapMinigameActivity.class);
                requestCode = 222;
            } else {
                activityClass = CoinFlipMinigameActivity.class;
                intent = new Intent(this, CoinFlipMinigameActivity.class);
                requestCode = 333;
            }
            if (isInBackground) {
                showMinigameReadyNotification(activityClass);
            } else {
                isPlayingMinigame = true;
                startActivityForResult(intent, requestCode);
            }
            prefs.edit().putLong(KEY_LAST_PLAYED_DATE, currentTime).apply();
        }
    }

    private void zoomOutToShowSnailButKeepPlayerCentered() {
        if (mMap == null || currentPlayerLocation == null || snailPosition == null) {
            Toast.makeText(this, "Waiting for locations...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Distance in meters between player and snail
        float[] results = new float[1];
        Location.distanceBetween(
                currentPlayerLocation.latitude,
                currentPlayerLocation.longitude,
                snailPosition.latitude,
                snailPosition.longitude,
                results
        );
        float distanceMeters = results[0];

        // Calculate an appropriate zoom level based on distance
        float zoomLevel;
        if (distanceMeters < 50) {
            zoomLevel = 18f;
        } else if (distanceMeters < 100) {
            zoomLevel = 17f;
        } else if (distanceMeters < 250) {
            zoomLevel = 16f;
        } else if (distanceMeters < 500) {
            zoomLevel = 15f;
        } else if (distanceMeters < 1000) {
            zoomLevel = 14f;
        } else {
            zoomLevel = 13f;
        }

        // Animate the map, keeping player centered
        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(currentPlayerLocation, zoomLevel);
        mMap.animateCamera(update);
    }

    private void resetLocalGameState() {
        if (snailTrailPoints != null) {
            snailTrailPoints.clear();
        }

        snailPosition = null;
        gameStartTimeElapsedMillis = 0;
        totalSnailDistanceMeters = 0f;

        clearGameStatePrefs();

        if (snailDistanceText != null) {
            snailDistanceText.setText("Waiting for location...");
        }
    }

    private boolean isGameServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (GameService.class.getName().equals(service.service.getClassName())) {
                    return service.foreground; // Check if it's specifically a foreground service
                }
            }
        }
        return false;
    }

    private void spawnSnailAndStartGame() { // Renamed from spawnSnail
        spawnSnailAtRandomLocation();
        startSnailChase();
        startSnailAbilityCycle(); // 👈 Add this

        if (currentPlayerLocation == null || mMap == null) {
            Log.e(TAG_MAIN_ACTIVITY, "Cannot spawn snail, player location or map is null.");
            Toast.makeText(this, "Waiting for current location to spawn snail...", Toast.LENGTH_SHORT).show();
            return;
        }
        if (isGameServiceActive || isGameServiceRunning()) {
            Log.d(TAG_MAIN_ACTIVITY, "Game service is already active. Not respawning.");
            // Optionally, just update UI based on service state if activity is resuming
            return;
        }

        // --- Load Snail Sprite (Keep this in MainActivity for UI) ---
        loadSelectedSnailSprite();
        if (loadedSnailSpriteIdentifier == null) {
            Log.w(TAG_MAIN_ACTIVITY, "loadedSnailSpriteIdentifier is null, attempting to load it now.");
            loadSelectedSnailSprite();
        }
        int snailDrawableId = getSnailDrawableResId(loadedSnailSpriteIdentifier);
        BitmapDescriptor snailIcon = BitmapDescriptorFactory.fromResource(snailDrawableId);
        // --- End Snail Sprite ---

        // Calculate initial snail position (still needed in MainActivity to pass to service)
        String snailDistanceSetting = sharedPreferences.getString("snail_distance", "Distant: 50-100m");
        double initialDistanceMeters = getInitialSnailDistanceInMeters(snailDistanceSetting);
        double angle = new Random().nextDouble() * 2 * Math.PI;
        double latOffsetDegrees = metersToLatitudeDegrees(initialDistanceMeters * Math.sin(angle));
        double lngOffsetDegrees = metersToLongitudeDegrees(initialDistanceMeters * Math.cos(angle), currentPlayerLocation.latitude);

        // This is the initial snail position. It will be sent to the service.
        LatLng initialSnailPosition = new LatLng(
                currentPlayerLocation.latitude + latOffsetDegrees,
                currentPlayerLocation.longitude + lngOffsetDegrees
        );
        this.snailPosition = initialSnailPosition; // Keep a local copy for immediate UI update

        if (snailMarker != null) snailMarker.remove();
        snailMarker = mMap.addMarker(new MarkerOptions()
                .position(initialSnailPosition)
                .title("The Snail")
                .icon(snailIcon)
                .anchor(0.5f, 0.5f));
        Log.i(TAG_MAIN_ACTIVITY, "Initial snail marker placed at: " + initialSnailPosition);

        hasSpawnedSnail = true;
        isGameOver = false;
        gameStartTimeElapsedMillis = SystemClock.elapsedRealtime(); // Reset game timer
        totalSnailDistanceMeters = 0f; // Reset distance for new game

        // Clear any previous trail from UI
        if (snailTrailPoints == null) snailTrailPoints = new ArrayList<>();
        snailTrailPoints.clear();
        if (snailTrail != null) snailTrail.remove();
        snailTrailPoints.add(initialSnailPosition); // Add first point
        drawSnailTrail(); // Draw the initial point

        // Start the GameService
        startGameService(initialSnailPosition);
    }

    private void drawSnailTrail() {
    }


    private void startGameService(LatLng initialSnailPositionToPass) {
        if (isGameServiceRunning()) {
            Log.d(TAG_MAIN_ACTIVITY, "Attempted to start service, but it's already running.");
            // You might want to just ensure MainActivity is subscribed to updates
            return;
        }

        Intent serviceIntent = new Intent(this, GameService.class);
        if (currentPlayerLocation != null) {
            serviceIntent.putExtra("player_lat", currentPlayerLocation.latitude);
            serviceIntent.putExtra("player_lng", currentPlayerLocation.longitude);
        }
        if (initialSnailPositionToPass != null) {
            serviceIntent.putExtra("snail_lat", initialSnailPositionToPass.latitude);
            serviceIntent.putExtra("snail_lng", initialSnailPositionToPass.longitude);
        }
        // Pass game settings
        currentSnailSpeedSetting = sharedPreferences.getString("snail_speed", "Normal Chase");
        gameOverDistanceMeters = Float.parseFloat(sharedPreferences.getString("game_over_distance_meters", "5.0"));

        serviceIntent.putExtra("snail_speed_setting", currentSnailSpeedSetting);
        serviceIntent.putExtra("game_over_distance", gameOverDistanceMeters);
        serviceIntent.putExtra("game_tick_interval", gameTickIntervalMillis);


        ContextCompat.startForegroundService(this, serviceIntent);
        isGameServiceActive = true; // Local flag to indicate WE started it
        Log.d(TAG_MAIN_ACTIVITY, "GameService start requested.");
    }

    // BroadcastReceiver to get updates from GameService
    private BroadcastReceiver gameStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (GameService.ACTION_GAME_STATE_UPDATE.equals(action)) {
                if (intent.hasExtra("snail_lat") && intent.hasExtra("snail_lng")) {
                    double snailLat = intent.getDoubleExtra("snail_lat", 0);
                    double snailLng = intent.getDoubleExtra("snail_lng", 0);
                    LatLng serviceSnailPosition = new LatLng(snailLat, snailLng);

                    MainActivity.this.snailPosition = serviceSnailPosition; // Update local copy

                    if (mMap != null && snailMarker != null) {
                        snailMarker.setPosition(serviceSnailPosition);
                        // Update trail (MainActivity still manages the visual trail)
                        updateSnailTrail(serviceSnailPosition);
                    }
                    if (intent.hasExtra("player_lat") && intent.hasExtra("player_lng")) {
                        // Service can also send player location if it's the sole source
                        // For now, MainActivity's location client is primary for its own UI blue dot
                    }
                    // Update distance text using the service's view of the snail
                    if (currentPlayerLocation != null && MainActivity.this.snailPosition != null) {
                        float[] results = new float[1];
                        Location.distanceBetween(
                                currentPlayerLocation.latitude, currentPlayerLocation.longitude,
                                MainActivity.this.snailPosition.latitude, MainActivity.this.snailPosition.longitude,
                                results);
                        updateDistanceDisplay(results[0]);
                    }
                }
            } else if (GameService.ACTION_GAME_OVER.equals(action)) {
                Log.i(TAG_MAIN_ACTIVITY, "Received GAME_OVER broadcast from service.");
                // Game is over, service will stop itself. MainActivity should react.
                isGameOver = true;
                isGameServiceActive = false; // Our local flag
                // Transition to GameOverActivity
                Intent gameOverIntent = new Intent(MainActivity.this, GameOverActivity.class);
                // Pass necessary data like time survived, distance, etc.
                // This data might come from the service's broadcast or be tracked by MainActivity
                // For example, if service sent final positions:
                if (intent.hasExtra("final_snail_lat")) {
                    // You could use these to show final map state, but GameOverActivity is simpler
                }
                gameOverIntent.putExtra("TIME_SURVIVED", SystemClock.elapsedRealtime() - gameStartTimeElapsedMillis);
                gameOverIntent.putExtra("SNAIL_DISTANCE", totalSnailDistanceMeters); // This needs careful calculation if service is master
                gameOverIntent.putExtra("SNAIL_CAUGHT_PLAYER", true);

                startActivityForResult(gameOverIntent, REQUEST_CODE_GAME_OVER);
                // No need to call stopGameService() here as the service is designed to stop itself on game over.
            }
        }
    };

    private void updateDistanceDisplay(float result) {
        if (snailDistanceText == null) return;

        if (useImperial) {
            double feet = result * 3.28084;
            snailDistanceText.setText(String.format(Locale.US, "Snail: %.1f ft\nSpeed: %s", feet, currentSnailSpeedSetting));
        } else {
            snailDistanceText.setText(String.format(Locale.US, "Snail: %.1f m\nSpeed: %s", result, currentSnailSpeedSetting));
        }
    }

    private void activateShellShield() {
        long baseShieldTime = 20_000; // or number of hits
        long adjusted = (long) (baseShieldTime * getPowerUpDurationMultiplier());
        isShieldActive = true;
        shieldShieldEndTimeMs = SystemClock.elapsedRealtime() + adjusted; // track timeout
        // In chase loop: if shield active but expired -> isShieldActive = false
    }

    private void startShellShieldCooldownUI() {
        shellShieldCooldownTimer = new CountDownTimer(shellShieldCooldownMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000;
                useShieldBtn.setText("Recharging... " + secondsLeft + "s");
            }

            public void onFinish() {
                isShellShieldOnCooldown = false;
                useShieldBtn.setEnabled(true);
                useShieldBtn.setText("Use Shell Shield");
            }
        }.start();
    }

    private void startDecoyShellCooldownUI() {
        decoyShellCooldownTimer = new CountDownTimer(decoyShellCooldownMillis, 1000) {
            public void onTick(long millisUntilFinished) {
                long secondsLeft = millisUntilFinished / 1000;
                useDecoyBtn.setText("Recharging... " + secondsLeft + "s");
            }

            public void onFinish() {
                isDecoyShellOnCooldown = false;
                useDecoyBtn.setEnabled(true);
                useDecoyBtn.setText("Use Decoy Shell");
            }
        }.start();
    }

    private void addPowerUp(String key, String name) {
        int currentCount = powerUpPrefs.getInt(key, 0);
        int maxLimit = key.equals("shellShield") ? 1 : 5;

        if (currentCount >= maxLimit) {
            Toast.makeText(this, name + " is already at max (" + maxLimit + ")", Toast.LENGTH_SHORT).show();
            return;
        }

        powerUpEditor.putInt(key, currentCount + 1);
        powerUpEditor.apply();
        updatePowerUpUI();
        Toast.makeText(this, "🎁 You earned a " + name + "!", Toast.LENGTH_SHORT).show();
    }

    private void resetInventory() {
        if (powerUpEditor != null) {
            Log.d("GameOver", "Resetting inventory to 0.");
            powerUpEditor.putInt("saltBomb", 0);
            powerUpEditor.putInt("decoyShell", 0);
            powerUpEditor.putInt("shellShield", 0);
            powerUpEditor.apply(); // Apply the changes to SharedPreferences

            // Optionally, update the UI immediately if the inventory panel might be visible
            // or if other UI elements depend on these counts.
            // If the game over screen is shown immediately, this might not be strictly necessary
            // until the next game starts, but it doesn't hurt.
            updatePowerUpUI();
        } else {
            Log.e("ResetInventory", "powerUpEditor is null, cannot reset inventory.");
        }
    }

    private void activateDecoyShell() {
        long baseDuration = 15_000; // 15 seconds
        long adjusted = (long) (baseDuration * getPowerUpDurationMultiplier());
        saltBombEndTimeMs = SystemClock.elapsedRealtime() + adjusted;
        isDecoyShellActive = true;
        freezeSnailFor(adjusted);
    }

    private void activateSaltBomb() {
        long baseDuration = 15_000; // 15 seconds
        long adjusted = (long) (baseDuration * getPowerUpDurationMultiplier());
        saltBombEndTimeMs = SystemClock.elapsedRealtime() + adjusted;
        isSaltBombActive = true;
        freezeSnailFor(adjusted);
    }

    private void freezeSnailFor(long adjusted) {
    }

    private float getPowerUpDurationMultiplier() {
        SharedPreferences prefs = getSharedPreferences("GameSettings", MODE_PRIVATE);
        String speedSetting = prefs.getString("snailSpeed", "Sluggish Crawl");

        switch (speedSetting) {
            case "Sluggish Crawl":             // ~0.03 m/s
                return 2.0f;
            case "Fast Snail":                 // ~0.06 m/s
                return 1.7f;
            case "Turtle Speed":               // ~0.1 m/s
                return 1.5f;
            case "Casual Walk":                // ~1.0 m/s
                return 1.0f;
            case "Power Walk":                 // ~1.5 m/s
                return 0.9f;
            case "Jogging Snail":              // ~2.5 m/s
                return 0.75f;
            case "Running Snail":              // ~5.0 m/s
                return 0.6f;
            case "Olympic Sprinting Snail":    // ~10.4 m/s
                return 0.4f;
            case "Snail Drives Car":           // ~20 m/s
                return 0.18f;
            default:
                return 1.0f;
        }
    }

    private void pushSnailBack(double meters) {
        if (snailPosition == null || mMap == null) return;

        Random random = new Random();
        double angle = random.nextDouble() * 2 * Math.PI;
        double latOffset = metersToLatitudeDegrees(meters * Math.sin(angle));
        double lngOffset = metersToLongitudeDegrees(meters * Math.cos(angle), snailPosition.latitude);

        snailPosition = new LatLng(snailPosition.latitude + latOffset, snailPosition.longitude + lngOffset);
        if (snailMarker != null) snailMarker.setPosition(snailPosition);

        // Update trail
        snailTrailPoints.add(snailPosition);
        if (snailTrailPoints.size() > MAX_TRAIL_POINTS) snailTrailPoints.remove(0);
        if (snailTrail != null) snailTrail.setPoints(snailTrailPoints);
    }
    private void boostSnailSpeedTemporarily() {
        snailSpeedMultiplier *= 2f;
        snailSpeedBoostActive = true;
        snailSpeedBoostEndTimeMs = System.currentTimeMillis() + TWENTY_FOUR_HOURS;
    }
    private void updatePowerUpUI() {
        SharedPreferences powerUpPrefs = getSharedPreferences("PowerUpInventory", MODE_PRIVATE);
        TextView saltBombLabel = findViewById(R.id.saltBombLabel);
        TextView decoyShellLabel = findViewById(R.id.decoyLabel);  // FIXED: was mistakenly set to 'shieldLabel'
        TextView shellShieldLabel = findViewById(R.id.shieldLabel);

        Button buySaltBombBtn = findViewById(R.id.buySaltBombBtn);
        Button buyDecoyBtn = findViewById(R.id.buyDecoyBtn);
        Button buyShieldBtn = findViewById(R.id.buyShieldBtn);

        int saltBombCount = powerUpPrefs.getInt("saltBomb", 0);
        int decoyShellCount = powerUpPrefs.getInt("decoyShell", 0);
        int shellShieldCount = powerUpPrefs.getInt("shellShield", 0);

        saltBombLabel.setText("🧊 Salt Bomb (" + saltBombCount + "/5)");
        decoyShellLabel.setText("🐚 Decoy Shell (" + decoyShellCount + "/5)");
        shellShieldLabel.setText("🛡️ Shell Shield (" + shellShieldCount + "/1)");

        // Salt Bomb
        if (saltBombCount >= 5) {
            buySaltBombBtn.setEnabled(false);
            buySaltBombBtn.setAlpha(0.5f);
        } else {
            buySaltBombBtn.setEnabled(true);
            buySaltBombBtn.setAlpha(1.0f);
        }

        // Decoy Shell
        if (decoyShellCount >= 5) {
            buyDecoyBtn.setEnabled(false);
            buyDecoyBtn.setAlpha(0.5f);
        } else {
            buyDecoyBtn.setEnabled(true);
            buyDecoyBtn.setAlpha(1.0f);
        }

        // Shell Shield
        if (shellShieldCount >= 1) {
            buyShieldBtn.setEnabled(false);
            buyShieldBtn.setAlpha(0.5f);
        } else {
            buyShieldBtn.setEnabled(true);
            buyShieldBtn.setAlpha(1.0f);
        }
    }


    private void startGameService() {
        Intent serviceIntent = new Intent(this, GameService.class);
        // Pass any initial data the service needs, e.g., current snail/player positions
        if (currentPlayerLocation != null) {
            serviceIntent.putExtra("player_lat", currentPlayerLocation.latitude);
            serviceIntent.putExtra("player_lng", currentPlayerLocation.longitude);
        }
        if (snailPosition != null) {
            serviceIntent.putExtra("snail_lat", snailPosition.latitude);
            serviceIntent.putExtra("snail_lng", snailPosition.longitude);
        }
        // ... pass other necessary game state ...

        ContextCompat.startForegroundService(this, serviceIntent);
        Log.d("MainActivity", "GameService started.");
    }

    private void stopGameServiceAndReset() {
        Intent serviceIntent = new Intent(this, GameService.class);
        stopService(serviceIntent);
        Log.d("MainActivity", "GameService stopped.");
    }

    private void saveGameState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_GAME_STATE, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        SharedPreferences.Editor powerUpEditor = getSharedPreferences("PowerUps", MODE_PRIVATE).edit();
        powerUpEditor.putInt("saltBombCount", saltBombCount);
        powerUpEditor.putInt("shellShieldCount", shellShieldCount);
        powerUpEditor.putInt("decoyShellCount", decoyShellCount);
        powerUpEditor.apply();

        if (hasSpawnedSnail && !isGameOver) { // Only save active game state
            if (snailPosition != null) {
                editor.putLong(KEY_SNAIL_LAT_BEFORE_PAUSE, Double.doubleToRawLongBits(snailPosition.latitude));
                editor.putLong(KEY_SNAIL_LNG_BEFORE_PAUSE, Double.doubleToRawLongBits(snailPosition.longitude));
            }
            if (currentPlayerLocation != null) { // This becomes playerPositionBeforePause effectively
                editor.putLong(KEY_PLAYER_LAT_BEFORE_PAUSE, Double.doubleToRawLongBits(currentPlayerLocation.latitude));
                editor.putLong(KEY_PLAYER_LNG_BEFORE_PAUSE, Double.doubleToRawLongBits(currentPlayerLocation.longitude));
            }
            editor.putLong(KEY_TIME_PAUSED_ELAPSED, SystemClock.elapsedRealtime());
            editor.putFloat(KEY_SNAIL_DISTANCE_BEFORE_PAUSE, totalSnailDistanceMeters);
            editor.putLong(KEY_GAME_START_TIME_ELAPSED, gameStartTimeElapsedMillis);
            editor.putBoolean(KEY_SNAIL_HAS_SPAWNED_ON_PAUSE, true);
            editor.putBoolean(KEY_HAS_SAVED_GAME, true);
            Log.d("GameState", "Saving game state: Snail@" + snailPosition + ", Player@" + currentPlayerLocation + ", TimePaused: " + SystemClock.elapsedRealtime());
        } else {
            editor.putBoolean(KEY_SNAIL_HAS_SPAWNED_ON_PAUSE, false); // No active game to save
            editor.putBoolean(KEY_HAS_SAVED_GAME, false);
            Log.d("GameState", "No active game, saving minimal state (spawned=false).");
        }
        editor.apply();
    }

    private void loadGameState() {
        SharedPreferences prefs = getSharedPreferences(PREFS_GAME_STATE, MODE_PRIVATE);
        boolean snailWasSpawnedOnPause = prefs.getBoolean(KEY_SNAIL_HAS_SPAWNED_ON_PAUSE, false);

        if (snailWasSpawnedOnPause) {
            double snailLat = Double.longBitsToDouble(prefs.getLong(KEY_SNAIL_LAT_BEFORE_PAUSE, 0));
            double snailLng = Double.longBitsToDouble(prefs.getLong(KEY_SNAIL_LNG_BEFORE_PAUSE, 0));
            snailPositionBeforePause = new LatLng(snailLat, snailLng);
            snailPosition = snailPositionBeforePause;
            hasSpawnedSnail = true;

            double playerLat = Double.longBitsToDouble(prefs.getLong(KEY_PLAYER_LAT_BEFORE_PAUSE, 0));
            double playerLng = Double.longBitsToDouble(prefs.getLong(KEY_PLAYER_LNG_BEFORE_PAUSE, 0));
            playerPositionBeforePause = new LatLng(playerLat, playerLng);
            if (currentPlayerLocation == null) {
                currentPlayerLocation = playerPositionBeforePause;
                playerPosition = playerPositionBeforePause;
            }

            timePausedElapsedMillis = prefs.getLong(KEY_TIME_PAUSED_ELAPSED, 0);
            totalSnailDistanceBeforePause = prefs.getFloat(KEY_SNAIL_DISTANCE_BEFORE_PAUSE, 0f);
            gameStartTimeElapsedMillis = prefs.getLong(KEY_GAME_START_TIME_ELAPSED, 0L);

            // ✅ Restore snail marker
            if (mMap != null) {
                if (snailMarker != null) snailMarker.remove();
                snailMarker = mMap.addMarker(new MarkerOptions()
                        .position(snailPosition)
                        .title("Snail"));
            }

            // ✅ Restore power-up counts
            SharedPreferences powerUpPrefs = getSharedPreferences("PowerUps", MODE_PRIVATE);
            int saltBombCount = powerUpPrefs.getInt("saltBomb", 0);
            int shellShieldCount = powerUpPrefs.getInt("shellShield", 0);
            int decoyShellCount = powerUpPrefs.getInt("decoyShell", 0);


            // Save to variables or update UI if needed
            this.saltBombCount = saltBombCount;
            this.shellShieldCount = shellShieldCount;
            this.decoyShellCount = decoyShellCount;

            updatePowerUpUI(); // if you have this method

            Log.d("GameState", "Loaded game + power-ups: Salt=" + saltBombCount + ", Shield=" + shellShieldCount);

            // Determine how long the game was paused and move the snail
            long elapsed = SystemClock.elapsedRealtime() - timePausedElapsedMillis;
            if (elapsed > 0) {
                recalculateSnailPositionAfterPause(elapsed);
                clearGameStatePrefs();
                startSnailChase();
            }
        } else {
            Log.d("GameState", "No saved game state to load.");
        }
    }


    private void clearGameStatePrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_GAME_STATE, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.putBoolean(KEY_HAS_SAVED_GAME, false);
        editor.apply();
        // Also clear the in-memory variables related to pause state
        snailPositionBeforePause = null;
        playerPositionBeforePause = null;
        timePausedElapsedMillis = 0;
        totalSnailDistanceBeforePause = 0f;
        // gameStartTimeElapsedMillis is part of overall game, reset in resetGame()
        Log.d("GameState", "Cleared persisted game state from SharedPreferences.");
    }

    public void onStartNewGameClick(View view) {
        // 1. Reset power-ups BEFORE starting MainActivity

        SharedPreferences powerUpPrefs = getSharedPreferences("PowerUpInventory", MODE_PRIVATE);
        SharedPreferences.Editor powerUpEditor = powerUpPrefs.edit();
        powerUpEditor.clear();
        powerUpEditor.apply(); //

        Log.d("MainMenuActivity", "Power-ups reset for new game.");

        // 2. Then start MainActivity
        Intent intent = new Intent(this, MainActivity.class);
        // You might want to pass a flag to MainActivity to indicate it's a fresh new game,
        // although resetting prefs here is often sufficient.
        // intent.putExtra("NEW_GAME_FLAG", true);
        startActivity(intent);
    }

    private void resetPowerUps() {
        // Assuming powerUpPrefs and powerUpEditor are initialized as member variables
        // If not, you'll need to get an instance of SharedPreferences here.
        // SharedPreferences powerUpPrefs = getSharedPreferences("PowerUpInventory", MODE_PRIVATE);
        // SharedPreferences.Editor powerUpEditor = powerUpPrefs.edit();

        if (powerUpEditor != null) {
            powerUpEditor.putInt("saltBomb", 0);
            powerUpEditor.putInt("decoyShell", 0);
            powerUpEditor.putInt("shellShield", 0);
            // Add any other power-up keys you have
            powerUpEditor.apply(); // Or commit() if you need to know the result immediately

            Log.d("GameReset", "Power-ups have been reset to 0.");

            // Optionally, update the UI immediately if the inventory panel is visible
            // or if other UI elements display power-up counts.
            // updatePowerUpUI(); // If this method exists and is accessible
        } else {
            Log.e("GameReset", "powerUpEditor is null. Cannot reset power-ups.");
        }
    }

    private float getSnailMetersPerSecond(String speedLabel) {
        switch (speedLabel) {
            case "Sluggish Crawl":
                return 0.03f;
            case "Fast Snail":
                return 0.06f;
            case "Turtle Speed":
                return 0.1f;
            case "Casual Walk":
                return 1.0f;
            case "Power Walk":
                return 1.5f;
            case "Jogging Snail":
                return 2.5f;
            case "Running Snail":
                return 5.0f;
            case "Olympic Sprinting Snail":
                return 10.4f;
            case "Snail Drives Car":
                return 40.0f;
            default:
                return 0.03f;
        }
    }


    private double metersToLatitudeDegrees(double meters) {
        return meters / 111139.0;
    } // Approx meters per degree latitude

    private double metersToLongitudeDegrees(double meters, double latitude) {
        return meters / (111139.0 * Math.cos(Math.toRadians(latitude)));
    }

    private double getInitialSnailDistanceInMeters(String distanceLabel) {
        Random rand = new Random();
        double min, max;

        switch (distanceLabel) {
            case "Very Close: 5–25m / 16–80ft":
                min = 5;
                max = 25;
                break;
            case "Close: 10–50m / 30–160ft":
                min = 10;
                max = 50;
                break;
            case "Distant: 50–100m / 160–330ft":
                min = 50;
                max = 100;
                break;
            case "Far: 100–200m / 0.06–0.12mi":
                min = 100;
                max = 200;
                break;
            case "Very Far: 200–400m / 0.12–0.25mi":
                min = 200;
                max = 400;
                break;
            case "Extreme: 400–800m / 0.25–0.50mi":
                min = 400;
                max = 800;
                break;
            default:
                min = 50;
                max = 100;
        }

        return min + (max - min) * rand.nextDouble();
    }


    private void spawnSnail(String snailDistanceSetting) {
        if (currentPlayerLocation == null || mMap == null) {
            Log.e("SnailSpawn", "Cannot spawn snail, player location or map is null.");
            return;
        }

        // --- Important: Ensure the sprite identifier is loaded BEFORE this point ---
        // If loadSelectedSnailSprite() is called in onCreate/onResume, loadedSnailSpriteIdentifier should be up-to-date.
        // If not, call it:
        // loadSelectedSnailSprite(); // Or ensure it's loaded appropriately elsewhere before spawnSnail is called

        if (loadedSnailSpriteIdentifier == null) { // Defensive check
            Log.w("SnailSpawn", "loadedSnailSpriteIdentifier is null, attempting to load it now.");
            Log.w("MainActivity_SpawnSnail", "loadedSnailSpriteIdentifier is null before getting Res ID. Forcing reload.");
            loadSelectedSnailSprite();
        }
        Log.d("MainActivity_SpawnSnail", "Using loadedSnailSpriteIdentifier: '" + loadedSnailSpriteIdentifier + "' for spawning.");
        int snailDrawableId = getSnailDrawableResId(loadedSnailSpriteIdentifier);
        Log.d("MainActivity_SpawnSnail", "Drawable ID for spawn: " + snailDrawableId);

        double initialDistanceMeters = getInitialSnailDistanceInMeters(snailDistanceSetting);
        double angle = new Random().nextDouble() * 2 * Math.PI; // Random angle in radians
        double latOffsetDegrees = metersToLatitudeDegrees(initialDistanceMeters * Math.sin(angle));
        double lngOffsetDegrees = metersToLongitudeDegrees(initialDistanceMeters * Math.cos(angle), currentPlayerLocation.latitude);
        snailPosition = new LatLng(
                currentPlayerLocation.latitude + latOffsetDegrees,
                currentPlayerLocation.longitude + lngOffsetDegrees);
        BitmapDescriptor snailIcon = null;
        try {
            snailIcon = BitmapDescriptorFactory.fromResource(snailDrawableId);
        } catch (Resources.NotFoundException e) {
            Log.e("MainActivity_SpawnSnail", "RESOURCE NOT FOUND for ID: " + snailDrawableId + " when creating BitmapDescriptor. Identifier was: '" + loadedSnailSpriteIdentifier + "'", e);
            // Fallback to a guaranteed default to prevent crash
            snailIcon = BitmapDescriptorFactory.fromResource(R.drawable.snail); // Ensure R.drawable.snail exists
            Log.w("MainActivity_SpawnSnail", "Fell back to default snail icon due to resource not found.");
        }


        if (snailMarker != null) {
            snailMarker.remove();
            snailMarker = null; // Good practice to nullify after removal
            Log.d("SnailSpawn", "Previous snailMarker removed.");
        }
        if (snailMarker != null) { /* ... */ }
        if (mMap != null && snailIcon != null) { // Check snailIcon is not null
            snailMarker = mMap.addMarker(new MarkerOptions()
                    .position(snailPosition)
                    .title("The Snail")
                    .icon(snailIcon) // Use the dynamically loaded icon
                    .anchor(0.5f, 0.5f));
            Log.i("MainActivity_SpawnSnail", "Snail marker created/updated with icon for identifier: '" + loadedSnailSpriteIdentifier + "'");
            Log.i("MainActivity_SpawnSnail", "New snail marker created at: " + snailPosition);
        } else {
            Log.e("MainActivity_SpawnSnail", "Could not set snail marker icon. Map ready: " + (mMap != null) + ", Icon valid: " + (snailIcon != null));
            Log.e("MainActivity_SpawnSnail", "Could not create new snail marker. Map/Position/Icon might be null.");

        }

        if (snailTrail != null) {
            snailTrail.remove();
        }
        snailTrailPoints.clear();
        if (snailPosition != null) {
            snailTrailPoints.add(new LatLng(snailPosition.latitude, snailPosition.longitude)); // Start trail at spawn
        }
        PolylineOptions polylineOptions = new PolylineOptions()
                .color(Color.argb(180, 255, 100, 0)) // Orange-ish, semi-transparent
                .width(12)
                .addAll(snailTrailPoints);
        snailTrail = mMap.addPolyline(polylineOptions);

        gameStartTimeElapsedMillis = SystemClock.elapsedRealtime(); // Use elapsedRealtime for game start
        totalSnailDistanceMeters = 0f;

        hasSpawnedSnail = true; // Set this after successful spawn
        Log.d("SnailSpawn", "Snail spawned at: " + snailPosition + " approx " + initialDistanceMeters + "m away with sprite: " + loadedSnailSpriteIdentifier + " (Res ID: " + snailDrawableId + "). Game timer started.");
    }

    private void updateSnailIconOnResume() {
        if (mMap == null || snailMarker == null || !hasSpawnedSnail) {
            return; // Nothing to update or map not ready

        }
        SharedPreferences prefs = getSharedPreferences("SnailGameState", MODE_PRIVATE);
        if (prefs.contains("snail_lat") && prefs.contains("snail_lng")) {
            double lat = Double.parseDouble(prefs.getString("snail_lat", ""));
            double lng = Double.parseDouble(prefs.getString("snail_lng", ""));
            snailPosition = new LatLng(lat, lng);
            hasSpawnedSnail = true;
            Log.d("onCreate", "Restored snail from saved state at: " + lat + ", " + lng);
            if (mMap != null) {
                snailMarker = mMap.addMarker(new MarkerOptions()
                        .position(snailPosition)
                        .title("Snail"));
            }
            Log.d("GameState", "Snail restored at: " + lat + ", " + lng);
        }

        Log.d("MainActivity_UpdateIcon", "Attempting to update snail icon on resume. Current loaded identifier: '" + loadedSnailSpriteIdentifier + "'");

        // Ensure the latest preference is loaded
        loadSelectedSnailSprite();
        Log.d("MainActivity_UpdateIcon", "After reload, identifier is: '" + loadedSnailSpriteIdentifier + "'");


        int snailDrawableId = getSnailDrawableResId(loadedSnailSpriteIdentifier);
        BitmapDescriptor snailIcon = null;
        try {
            snailIcon = BitmapDescriptorFactory.fromResource(snailDrawableId);
        } catch (Resources.NotFoundException e) {
            Log.e("MainActivity_UpdateIcon", "RESOURCE NOT FOUND for ID: " + snailDrawableId + " when creating BitmapDescriptor for update. Identifier was: '" + loadedSnailSpriteIdentifier + "'", e);
            snailIcon = BitmapDescriptorFactory.fromResource(R.drawable.snail); // Fallback
            Log.w("MainActivity_UpdateIcon", "Fell back to default snail icon for update.");
        }

        if (snailMarker != null && snailIcon != null) {
            snailMarker.setIcon(snailIcon);
            Log.i("MainActivity_UpdateIcon", "Snail icon updated on marker for identifier: '" + loadedSnailSpriteIdentifier + "'");
        } else {
            Log.w("MainActivity_UpdateIcon", "Could not update snail marker icon. Marker exists: " + (snailMarker != null) + ", Icon valid: " + (snailIcon != null));
        }
    }
// Inside MainActivity.java

    private void handleRevive() {
        if (snailPosition == null || currentPlayerLocation == null || mMap == null) {
            Log.e("Revive", "Cannot revive, critical game elements are null.");
            resetGame(); // Fallback to a normal reset if revive state is bad
            return;
        }

        Log.d("Revive", "Player chose to revive by watching an ad.");

        double pushBackDistanceMeters = 100.0;
        Random random = new Random();
        double angle = random.nextDouble() * 2 * Math.PI; // Random angle in radians

        // Option 1: Push snail back from ITS CURRENT position
        // This is simpler and directly "punishes" the snail
        double latOffsetDegrees = metersToLatitudeDegrees(pushBackDistanceMeters * Math.sin(angle));
        double lngOffsetDegrees = metersToLongitudeDegrees(pushBackDistanceMeters * Math.cos(angle), snailPosition.latitude); // Use snail's latitude for longitude calc

        LatLng newSnailPositionAfterRevive = new LatLng(
                snailPosition.latitude + latOffsetDegrees, // Add offset to push it away
                snailPosition.longitude + lngOffsetDegrees
        );

        // Option 2: Push snail back from PLAYER'S CURRENT position
        // This gives the player more breathing room directly.
    /*
    double latOffsetDegrees = metersToLatitudeDegrees(pushBackDistanceMeters * Math.sin(angle));
    double lngOffsetDegrees = metersToLongitudeDegrees(pushBackDistanceMeters * Math.cos(angle), currentPlayerLocation.latitude);

    LatLng newSnailPositionAfterRevive = new LatLng(
            currentPlayerLocation.latitude + latOffsetDegrees, // Calculate from player
            currentPlayerLocation.longitude + lngOffsetDegrees
    );
    // You might want to ensure the snail isn't pushed *too* far if it was already far from the player.
    // Or, you could calculate the snail's new position relative to the vector FROM player TO snail, and then extend it.
    // For simplicity, pushing from snail's current position (Option 1) is often fine.
    */


        Log.d("Revive", "Old Snail Position: " + snailPosition);
        snailPosition = newSnailPositionAfterRevive;
        Log.d("Revive", "New Snail Position after push back: " + snailPosition);

        if (snailMarker != null) {
            snailMarker.setPosition(snailPosition);
        } else { // Should not happen if game was just over, but as a fallback
            BitmapDescriptor snailIcon = BitmapDescriptorFactory.fromResource(R.drawable.snail);
            snailMarker = mMap.addMarker(new MarkerOptions()
                    .position(snailPosition).title("The Snail").icon(snailIcon).anchor(0.5f, 0.5f));
        }

        // Add a point to the trail indicating the "push"
        // For simplicity, just add the new point. A more complex visual could show a jump.
        if (snailTrailPoints != null && snailTrail != null) {
            snailTrailPoints.add(new LatLng(snailPosition.latitude, snailPosition.longitude));
            if (snailTrailPoints.size() > MAX_TRAIL_POINTS) {
                snailTrailPoints.remove(0);
            }
            snailTrail.setPoints(snailTrailPoints);
        }


        isGameOver = false; // Reset game over flag

        // Update UI
        if (snailDistanceText != null && currentPlayerLocation != null) {
            float[] distanceResults = new float[1];
            Location.distanceBetween(
                    currentPlayerLocation.latitude, currentPlayerLocation.longitude,
                    snailPosition.latitude, snailPosition.longitude,
                    distanceResults);
            if (useImperial) {
                double feet = distanceResults[0] * 3.28084;
                snailDistanceText.setText(String.format(Locale.US, "Snail: %.1f ft\nSpeed: %s", feet, currentSnailSpeedSetting));
            } else {
                snailDistanceText.setText(String.format(Locale.US, "Snail: %.1f m\nSpeed: %s", distanceResults[0], currentSnailSpeedSetting));
            }
        } else if (snailDistanceText != null) {
            snailDistanceText.setText("Snail pushed back!");
        }

        Toast.makeText(this, "Snail pushed back! Run!", Toast.LENGTH_LONG).show();

        // Persisted game state was cleared by triggerGameOver.
        // For revive, we are continuing, so we don't want it cleared yet.
        // If the game is paused AFTER revive, saveGameState() will save the new state.

        startSnailChase(); // Resume the chase!
    }

    private void setupLocationCallback(String snailDistanceSetting) {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                checkBeaconCapture();
                if (locationResult == null || isGameOver) return;
                for (Location location : locationResult.getLocations()) {
                    currentPlayerLocation = new LatLng(location.getLatitude(), location.getLongitude());
                    playerPosition = currentPlayerLocation; // ✅ Add this line
                    long now = System.currentTimeMillis();
                    if (lastSpeedCheckPosition == null) {
                        lastSpeedCheckPosition = currentPlayerLocation;
                        lastSpeedCheckTimeMs = now;
                        lastMovementTimeMs = now;
                    }
                    if (playerMarker == null && mMap != null) {
                        playerMarker = mMap.addMarker(new MarkerOptions()
                                .position(currentPlayerLocation)
                                .title("You"));
                    } else if (playerMarker != null) {
                        playerMarker.setPosition(currentPlayerLocation); // keep updating it
                    }
                    if (lastRewardCheckLocation != null && currentPlayerLocation != null) {
                        float[] result = new float[1];
                        Location.distanceBetween(
                                lastRewardCheckLocation.latitude, lastRewardCheckLocation.longitude,
                                currentPlayerLocation.latitude, currentPlayerLocation.longitude,
                                result);
                        float distance = result[0];
                        distanceSinceLastReward += distance;

                    }
                    lastRewardCheckLocation = currentPlayerLocation;
                    if (mMap != null && !hasSpawnedSnail && currentPlayerLocation != null) {
                        // If resuming from a killed state and game state was loaded, snail might already be "spawned" conceptually
                        // Check if snailPosition is already set from loaded state
                        if (hasSpawnedSnail || snailPosition != null) {
                            // Do nothing — snail already exists from saved state
                            Log.d("SnailSpawn", "Snail already exists. Skipping re-spawn.");
                        } else {
                            spawnSnail(snailDistanceSetting); // Only for new games
                            startSnailChase();
                        }
                    }
                    if (mMap != null && currentPlayerLocation != null) {
                        if (isFollowingPlayer) {
                            mMap.animateCamera(CameraUpdateFactory.newLatLng(currentPlayerLocation));
                        }

                        if (!hasCenteredOnce) {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPlayerLocation, 18f));
                            hasCenteredOnce = true;
                        }
                    }
                }
            }
        };
    }
    private void checkAndLaunchHoldMinigame() {
        SharedPreferences prefs = getSharedPreferences(HOLD_MINIGAME_PREFS, MODE_PRIVATE);
        long lastPlayed = prefs.getLong(KEY_LAST_HOLD_PLAYED_DATE, 0);
        long now = System.currentTimeMillis();

        if (now - lastPlayed >= TWENTY_FOUR_HOURS) {
            double random = Math.random(); // 0.0 to 1.0
            if (random < 0.25) { // 25% chance to play
                Log.d("HoldMinigame", "Random trigger: launching Hold minigame");
                prefs.edit().putLong(KEY_LAST_HOLD_PLAYED_DATE, now).apply();

                Intent intent = new Intent(MainActivity.this, HoldToSurviveMinigameActivity.class);
                isPlayingMinigame = true;
                startActivityForResult(intent, REQUEST_CODE_HOLD);
            } else {
                Log.d("HoldMinigame", "Random trigger skipped for today.");
            }
        }
    }

    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }
        LocationRequest locationRequest = LocationRequest.create(); // Builder pattern is new way
        locationRequest.setInterval(250); // Desired interval for active location updates
        locationRequest.setFastestInterval(250); // Fastest interval if available from other apps
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }
    // Add this method to your MainActivity.java
    private void stopLocationUpdates() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }
    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE
            );
        } else {
            startGameService(); // Or whatever starts GameService
        }
    }

    private void resetGame() {
        Log.d("ResetGame", "Resetting game state and UI for a new game.");

        isGameOver = false;
        hasSpawnedSnail = false;
        hasCenteredOnce = false;
        lastRewardCheckLocation = null;
        distanceSinceLastReward = 0f;

        snailHandler.removeCallbacksAndMessages(null);

        if (snailMarker != null) {
            snailMarker.remove();
            snailMarker = null;
        }
        if (snailTrail != null) {
            snailTrail.remove();
            snailTrail = null;
        }

        resetLocalGameState(); // <-- now calling it here properly

        Log.d("ResetGame", "Game reset. Waiting for location to re-spawn snail via onLocationResult.");
    }

    private void startSnailChase() {
        snailHandler.removeCallbacksAndMessages(null); // Ensure no duplicates
        Log.d("SnailChaseLogic", "startSnailChase called. isGameOver: " + isGameOver + ", SnailPos: " + snailPosition);
        if (isGameOver || snailPosition == null) { // Also check if snailPosition is null
            if (snailPosition == null && hasSpawnedSnail && !isGameOver) {
                Log.e("SnailChaseLogic", "Snail position is null but chase was started! This shouldn't happen.");
                // Potentially try to re-initialize or error out.

            }
            return;
        }


        snailHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (shellSplitActive) {
                    handleShellSplit(this);
                    return;
                }
                long now = System.currentTimeMillis();

                if (snailSpeedBoostActive && now > snailSpeedBoostEndTimeMs) {
                    snailSpeedMultiplier /= 2f;
                    snailSpeedBoostActive = false;
                }
                if (snailInactivityBoostActive && now > snailInactivityBoostEndTimeMs) {
                    snailSpeedMultiplier /= INACTIVITY_SPEED_MULTIPLIER;
                    snailInactivityBoostActive = false;
                }

                if (lastSpeedCheckPosition != null && currentPlayerLocation != null) {
                    float[] distRes = new float[1];
                    Location.distanceBetween(lastSpeedCheckPosition.latitude, lastSpeedCheckPosition.longitude,
                            currentPlayerLocation.latitude, currentPlayerLocation.longitude, distRes);
                    float elapsedSec = (now - lastSpeedCheckTimeMs) / 1000f;
                    float playerSpeed = elapsedSec > 0 ? distRes[0] / elapsedSec : 0f;
                    if (playerSpeed >= INACTIVITY_SPEED_THRESHOLD_MPS) {
                        lastMovementTimeMs = now;
                    }
                } else if (lastSpeedCheckPosition == null && currentPlayerLocation != null) {
                    lastMovementTimeMs = now;
                }
                lastSpeedCheckTimeMs = now;
                lastSpeedCheckPosition = currentPlayerLocation;

                if (!snailInactivityBoostActive && !snailSpeedBoostActive &&
                        now - lastMovementTimeMs >= INACTIVITY_DURATION_MS && !isInSleepHours()) {
                    snailSpeedMultiplier *= INACTIVITY_SPEED_MULTIPLIER;
                    snailInactivityBoostActive = true;
                    snailInactivityBoostEndTimeMs = now + INACTIVITY_BOOST_DURATION_MS;
                    Toast.makeText(MainActivity.this, "Why did you stop?", Toast.LENGTH_SHORT).show();
                }
                float snailMetersPerSecond = getSnailMetersPerSecond(currentSnailSpeedSetting) * snailSpeedMultiplier;
                float snailMetersPerMillisecond = snailMetersPerSecond / 1000f;
                float snailMoveStepPerUpdate = snailMetersPerMillisecond * updateIntervalMs / 111_111f;

                LatLng target = isDecoyActive && SystemClock.elapsedRealtime() < decoyEndTimeMs
                        ? decoyPosition
                        : currentPlayerLocation;
                if (isGameOver || currentPlayerLocation == null || snailPosition == null || mMap == null) {
                    return;

                }
                if (isSaltBombActive && SystemClock.elapsedRealtime() < saltBombEndTimeMs) {
                    // Skip this frame; snail is frozen
                    snailHandler.postDelayed(this, updateIntervalMs);
                    return;
                } else if (isSaltBombActive) {
                    isSaltBombActive = false; // Salt bomb expired
                    Toast.makeText(MainActivity.this, "Salt Bomb wore off!", Toast.LENGTH_SHORT).show();
                }

                LatLng currentSnailPosBeforeMove = new LatLng(snailPosition.latitude, snailPosition.longitude);

                float[] distanceToPlayerResults = new float[1];
                Location.distanceBetween(
                        currentPlayerLocation.latitude, currentPlayerLocation.longitude,
                        snailPosition.latitude, snailPosition.longitude,
                        distanceToPlayerResults);
                float distanceToPlayerMeters = distanceToPlayerResults[0];

                float gameOverDistanceMeters = 2.0f;
                if (distanceToPlayerMeters < gameOverDistanceMeters) {
                    if (isShieldActive) {
                        isShieldActive = false;
                        Toast.makeText(MainActivity.this, "Shell Shield blocked the hit!", Toast.LENGTH_SHORT).show();

                        pushSnailBack(30);  // Push the snail back 30 meters
                        startSnailChase();  // Resume chase loop
                        return;             // Skip the rest of this tick
                    } else {
                        long timeTaken = SystemClock.elapsedRealtime() - gameStartTimeElapsedMillis;
                        triggerGameOver(timeTaken, totalSnailDistanceMeters, "The snail caught you!");
                        return;
                    }
                }

                double dxTotal = target.latitude - snailPosition.latitude;
                double dyTotal = target.longitude - snailPosition.longitude;
                double totalDistanceDegrees = Math.sqrt(dxTotal * dxTotal + dyTotal * dyTotal);

                if (totalDistanceDegrees > 0) { // Avoid division by zero
                    double normDx = dxTotal / totalDistanceDegrees;
                    double normDy = dyTotal / totalDistanceDegrees;
                    double moveX, moveY;

                    if (totalDistanceDegrees < snailMoveStepPerUpdate) { // Snail will reach player this tick
                        moveX = dxTotal;
                        moveY = dyTotal;
                    } else {
                        moveX = normDx * snailMoveStepPerUpdate;
                        moveY = normDy * snailMoveStepPerUpdate;
                    }
                    snailPosition = new LatLng(snailPosition.latitude + moveX, snailPosition.longitude + moveY);
                }
                // else snail is already at player's location (should have been caught above)

                if (snailPosition != null) { // Recalculate distance moved this step
                    float[] distMovedThisStep = new float[1];
                    Location.distanceBetween(
                            currentSnailPosBeforeMove.latitude, currentSnailPosBeforeMove.longitude,
                            snailPosition.latitude, snailPosition.longitude,
                            distMovedThisStep
                    );
                    if (!Float.isNaN(distMovedThisStep[0]) && distMovedThisStep[0] > 0) {
                        totalSnailDistanceMeters += distMovedThisStep[0];
                    }
                }

                if (snailMarker != null && snailPosition != null)
                    snailMarker.setPosition(snailPosition);

                // Update Trail
                if (snailPosition != null && snailTrailPoints != null && snailTrail != null) {
                    // Check if snail has actually moved a tiny bit to avoid duplicate points if precision is an issue
                    boolean hasMovedSignificantly = snailTrailPoints.isEmpty() ||
                            (Math.abs(snailTrailPoints.get(snailTrailPoints.size() - 1).latitude - snailPosition.latitude) > 0.0000001 ||
                                    Math.abs(snailTrailPoints.get(snailTrailPoints.size() - 1).longitude - snailPosition.longitude) > 0.0000001);

                    if (hasMovedSignificantly) {
                        snailTrailPoints.add(new LatLng(snailPosition.latitude, snailPosition.longitude));
                        if (snailTrailPoints.size() > MAX_TRAIL_POINTS) snailTrailPoints.remove(0);
                        snailTrail.setPoints(snailTrailPoints);
                    }
                }

                if (snailDistanceText != null) {
                    String speedDebugText = String.format("Speed: %s", currentSnailSpeedSetting);
                    if (useImperial) {
                        double feet = distanceToPlayerMeters * 3.28084;
                        snailDistanceText.setText(String.format(Locale.US, "Snail: %.1f ft\n%s", feet, speedDebugText));
                    } else {
                        snailDistanceText.setText(String.format(Locale.US, "Snail: %.1f m\n%s", distanceToPlayerMeters, speedDebugText));
                    }
                }

                if (!isGameOver && snailHandler != null) {
                    snailHandler.postDelayed(this, updateIntervalMs);
                }
            }
        }, 0); // Start immediately for the first run
    }


    private void recalculateSnailPositionAfterPause(long elapsedMillisWhilePaused) {
        if (snailPositionBeforePause == null || playerPositionBeforePause == null || !hasSpawnedSnail) {
            Log.d("RecalculateSnail", "Not enough data to recalculate snail position.");
            clearGameStatePrefs(); // Clear stale data
            return;
        }

        LatLng effectivePlayerLocation = currentPlayerLocation != null ? currentPlayerLocation : playerPositionBeforePause;

        Log.d("RecalculateSnail", "Before recalc: Snail was at " + snailPositionBeforePause + ", Player was at " + playerPositionBeforePause);
        Log.d("RecalculateSnail", "Current player position: " + effectivePlayerLocation);

        // Snail speed in degrees per MILLISECOND for this calculation
        float snailMetersPerSecond = getSnailMetersPerSecond(currentSnailSpeedSetting) * snailSpeedMultiplier;
        float snailMetersPerMillisecond = snailMetersPerSecond / 1000f;
        float snailDegreesPerMillisecond = snailMetersPerMillisecond / 111_111f;


        double distanceSnailCouldTravelDegrees = snailDegreesPerMillisecond * elapsedMillisWhilePaused;
        float distanceSnailCouldTravelMeters = 0f; // For updating total distance

        Log.d("RecalculateSnail", "Snail speed: " + snailDegreesPerMillisecond * 1000 + " deg/s. Elapsed: " + elapsedMillisWhilePaused + "ms. Max travel: " + distanceSnailCouldTravelDegrees + " deg.");

        double dxTotalToOldPlayer = playerPositionBeforePause.latitude - snailPositionBeforePause.latitude;
        double dyTotalToOldPlayer = playerPositionBeforePause.longitude - snailPositionBeforePause.longitude;
        double totalDistanceToOldPlayerPosDegrees = Math.sqrt(dxTotalToOldPlayer * dxTotalToOldPlayer + dyTotalToOldPlayer * dyTotalToOldPlayer);

        LatLng newSnailPosition;
        List<LatLng> jumpTrailPoints = new ArrayList<>();
        jumpTrailPoints.add(new LatLng(snailPositionBeforePause.latitude, snailPositionBeforePause.longitude)); // Start of the jump

        if (totalDistanceToOldPlayerPosDegrees == 0) { // Snail was already at the player's last known location (unlikely if game wasn't over)
            newSnailPosition = new LatLng(snailPositionBeforePause.latitude, snailPositionBeforePause.longitude);
            // No distance traveled during pause in this specific edge case
        } else if (distanceSnailCouldTravelDegrees >= totalDistanceToOldPlayerPosDegrees) {
            // Snail could have reached or passed the player's *last known position during pause*
            newSnailPosition = new LatLng(playerPositionBeforePause.latitude, playerPositionBeforePause.longitude);
            float[] distResults = new float[1];
            Location.distanceBetween(snailPositionBeforePause.latitude, snailPositionBeforePause.longitude,
                    newSnailPosition.latitude, newSnailPosition.longitude, distResults);
            distanceSnailCouldTravelMeters = distResults[0];
            Log.d("RecalculateSnail", "Snail reached/passed player's PAUSED position. Jumped " + distanceSnailCouldTravelMeters + "m.");
        } else {
            // Snail moves along the straight line towards player's last known position
            double normDx = dxTotalToOldPlayer / totalDistanceToOldPlayerPosDegrees;
            double normDy = dyTotalToOldPlayer / totalDistanceToOldPlayerPosDegrees;
            newSnailPosition = new LatLng(
                    snailPositionBeforePause.latitude + (normDx * distanceSnailCouldTravelDegrees),
                    snailPositionBeforePause.longitude + (normDy * distanceSnailCouldTravelDegrees)
            );
            float[] distResults = new float[1];
            Location.distanceBetween(snailPositionBeforePause.latitude, snailPositionBeforePause.longitude,
                    newSnailPosition.latitude, newSnailPosition.longitude, distResults);
            distanceSnailCouldTravelMeters = distResults[0];
            Log.d("RecalculateSnail", "Snail moved along line to: " + newSnailPosition + ". Jumped " + distanceSnailCouldTravelMeters + "m.");
        }

        // Add interpolated points for the trail during the jump
        double jumpDx = newSnailPosition.latitude - snailPositionBeforePause.latitude;
        double jumpDy = newSnailPosition.longitude - snailPositionBeforePause.longitude;
        double totalJumpDistanceDegrees = Math.sqrt(jumpDx * jumpDx + jumpDy * jumpDy);

        if (totalJumpDistanceDegrees > INTERPOLATION_STEP_DEGREES) { // Only interpolate if it's a significant jump
            int steps = (int) (totalJumpDistanceDegrees / INTERPOLATION_STEP_DEGREES);
            for (int i = 1; i <= steps; i++) {
                double fraction = (double) i / steps;
                jumpTrailPoints.add(new LatLng(
                        snailPositionBeforePause.latitude + (jumpDx * fraction),
                        snailPositionBeforePause.longitude + (jumpDy * fraction)
                ));
            }
        }
        if (!jumpTrailPoints.get(jumpTrailPoints.size() - 1).equals(newSnailPosition)) { // Ensure final position is added
            jumpTrailPoints.add(new LatLng(newSnailPosition.latitude, newSnailPosition.longitude));
        }


        // Update the global snailPosition and total distance
        snailPosition = newSnailPosition;
        totalSnailDistanceMeters = totalSnailDistanceBeforePause + distanceSnailCouldTravelMeters; // Add jumped distance

        if (snailMarker != null) {
            snailMarker.setPosition(snailPosition);
        } else if (mMap != null) { // If marker was somehow lost (e.g. activity recreation issue)
            BitmapDescriptor snailIcon = BitmapDescriptorFactory.fromResource(R.drawable.snail);
            snailMarker = mMap.addMarker(new MarkerOptions()
                    .position(snailPosition).title("The Snail").icon(snailIcon).anchor(0.5f, 0.5f));
        }

        // Update trail with the jump points
        if (snailTrailPoints != null && snailTrail != null) {
            for (LatLng trailPoint : jumpTrailPoints) {
                if (snailTrailPoints.isEmpty() ||
                        (Math.abs(snailTrailPoints.get(snailTrailPoints.size() - 1).latitude - trailPoint.latitude) > 0.0000001 ||
                                Math.abs(snailTrailPoints.get(snailTrailPoints.size() - 1).longitude - trailPoint.longitude) > 0.0000001)) {
                    snailTrailPoints.add(trailPoint);
                    if (snailTrailPoints.size() > MAX_TRAIL_POINTS) {
                        snailTrailPoints.remove(0);
                    }
                }
            }
            snailTrail.setPoints(snailTrailPoints);
        }

        // --- IMPORTANT: Check for Game Over based on CURRENT player location ---
        float[] distanceToCurrentPlayerResults = new float[1]; // This is the array you want to use
        Location.distanceBetween(
                effectivePlayerLocation.latitude, effectivePlayerLocation.longitude,
                snailPosition.latitude, snailPosition.longitude,
                distanceToCurrentPlayerResults);
        float distanceToCurrentPlayerMeters = distanceToCurrentPlayerResults[0];

        Log.d("RecalculateSnail", "After recalc, snail is at " + snailPosition + ". Distance to CURRENT player (" + effectivePlayerLocation + "): " + distanceToCurrentPlayerMeters + "m");

        if (distanceToCurrentPlayerMeters < gameOverDistanceMeters && !isGameOver) {
            long totalTimeElapsedSinceGameStart = SystemClock.elapsedRealtime() - gameStartTimeElapsedMillis;
            triggerGameOver(totalTimeElapsedSinceGameStart, totalSnailDistanceMeters, "Snail got you (during pause)!");
        }

        // Pause state has been consumed, clear it.
        // It will be re-saved if the game goes into onPause again.
        // clearGameStatePrefs(); // No, onResume will handle clearing if game doesn't end.
    }
// In MainActivity.java

    private static final String DEFAULT_SNAIL_SPRITE_ID_MAIN = "snail_classic"; // Consistent default

    private BitmapDescriptor getSnailIcon() {
        SharedPreferences settingsPrefs = getSharedPreferences("GameSettings", MODE_PRIVATE);
        // Use the same default identifier as in SettingsActivity
        String spriteIdentifier = settingsPrefs.getString("selectedSnailSprite", DEFAULT_SNAIL_SPRITE_ID_MAIN);

        int drawableId;
        // The identifiers used in Settings (e.g., "snail_classic", "snail_spoon")
        // MUST match these case statements.
        switch (spriteIdentifier) {
            case "snail_spoon": // Matches identifier in SnailSprite("Spoon Snail", "snail_spoon", ...)
                drawableId = R.drawable.spoon; // The gameSpriteResId for spoon
                break;
            case "snail_classic": // Matches identifier in SnailSprite("Classic Snail", "snail_classic", ...)
            default:
                drawableId = R.drawable.snail; // The gameSpriteResId for classic snail
                break;
        }
        // You might want to cache BitmapDescriptors if they are created frequently
        return BitmapDescriptorFactory.fromResource(drawableId);
    }

    private void updateSnailTrail(LatLng newPoint) {
        if (snailTrailPoints == null || snailTrail == null) return;

        // Avoid duplicates
        boolean hasMovedSignificantly = snailTrailPoints.isEmpty()
                || (Math.abs(snailTrailPoints.get(snailTrailPoints.size() - 1).latitude - newPoint.latitude) > 0.0000001
                || Math.abs(snailTrailPoints.get(snailTrailPoints.size() - 1).longitude - newPoint.longitude) > 0.0000001);

        if (hasMovedSignificantly) {
            snailTrailPoints.add(newPoint);
            if (snailTrailPoints.size() > MAX_TRAIL_POINTS) {
                snailTrailPoints.remove(0);
            }
            snailTrail.setPoints(snailTrailPoints);
        }
    }

    private void updateSnailMarker() {
        if (mMap != null && snailPosition != null) {
            if (snailMarker == null) {
                snailMarker = mMap.addMarker(new MarkerOptions()
                        .position(snailPosition)
                        .title("Snail")
                        .icon(getSnailIcon()) // Use the dynamic icon
                        .anchor(0.5f, 0.5f));
            } else {
                // Optional: Log if map or position is not ready when this is called
                if (mMap == null) {
                    Log.w("UpdateSnailMarker", "mMap is null, cannot update snail marker.");
                }
                if (snailPosition == null) {
                    Log.w("UpdateSnailMarker", "snailPosition is null, cannot update snail marker.");
                }
                snailMarker.setPosition(snailPosition);
            }
        }
    }

    private void triggerGameOver(long timeTakenMillis, float distanceTraveledMeters, String message) {
        if (isGameOver) return; // Prevent multiple triggers

        isGameOver = true;
        snailHandler.removeCallbacksAndMessages(null); // Stop any active chase

        Log.d("GameOver", message + " Time: " + timeTakenMillis + "ms, Snail Distance: " + distanceTraveledMeters + "m");

        Intent gameOverIntent = new Intent(MainActivity.this, GameOverActivity.class);
        gameOverIntent.putExtra(GameOverActivity.EXTRA_TIME_TAKEN, timeTakenMillis);
        gameOverIntent.putExtra(GameOverActivity.EXTRA_DISTANCE_TRAVELED, distanceTraveledMeters);
        startActivityForResult(gameOverIntent, REQUEST_CODE_GAME_OVER);

        if (snailDistanceText != null) {
            float distDisplay = useImperial ? (float) (distanceTraveledMeters * 3.28084) : distanceTraveledMeters;
            String unit = useImperial ? "ft" : "m";
            snailDistanceText.setText(String.format(Locale.US, "%s\nTime: %02d:%02ds\nDist: %.1f%s",
                    message.toUpperCase(),
                    (timeTakenMillis / (1000 * 60)) % 60,
                    (timeTakenMillis / 1000) % 60,
                    distDisplay, unit));
        }
        clearGameStatePrefs(); // Game is over, clear any persisted state
    }


    @Override
    protected void onStart() { // Or onResume
        super.onStart();
        Log.d(TAG_MAIN_ACTIVITY, "onStart called");
        // Activity is visible again
        isInBackground = false;
        IntentFilter filter = new IntentFilter();
        filter.addAction(GameService.ACTION_GAME_STATE_UPDATE);
        filter.addAction(GameService.ACTION_GAME_OVER);
        LocalBroadcastManager.getInstance(this).registerReceiver(gameStateReceiver, filter);
        LocalBroadcastManager.getInstance(this).registerReceiver(gameStateReceiver,
                new IntentFilter("GAME_STATE_UPDATE"));
        if (isGameServiceRunning()) {
            isGameServiceActive = true; // Acknowledge service is running
            Log.d(TAG_MAIN_ACTIVITY, "Activity starting, GameService already running. Will listen for updates.");
        } else if (hasSpawnedSnail && !isGameOver && snailPosition != null /* and some state indicates game should be running */)
            // This is a tricky case: Activity thinks a game was on, but service isn't running.
            // This could happen if service was killed and didn't restart, or state is inconsistent.
            // For now, let's assume if service isn't running, we might need to re-initiate.
            // Consider prompting user or trying to restore from saved service state if more robust.
            Log.w(TAG_MAIN_ACTIVITY, "Activity state suggests game, but service not running. Consider re-spawning.");
    }

    @Override
    protected void onStop() { // Or onPause
        super.onStop();
        Log.d(TAG_MAIN_ACTIVITY, "onStop called");
        // Mark that the activity is no longer in the foreground
        isInBackground = true;
        LocalBroadcastManager.getInstance(this).unregisterReceiver(gameStateReceiver);
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnCameraMoveStartedListener(reason -> {
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                isFollowingPlayer = false;
            }
        });
        checkAndActivateNightMode();
        if (snailPosition != null) {
            updateSnailMarker(); // restore snail
        }
        // mMap.getUiSettings().setCompassEnabled(true); // Optional: show compass
        if (snailPosition != null && snailMarker == null) {
            Log.d("SnailDebug", "Recreating snail marker at: " + snailPosition);
            snailMarker = mMap.addMarker(new MarkerOptions()
                    .position(snailPosition)
                    .title("Snail")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))); // or use your custom icon
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
            // If game state was loaded and snail should be active, place it
            if (hasSpawnedSnail && snailPosition != null) { // Snail was spawned but marker lost (e.g. config change)
                updateSnailMarker();
            }
            if (hasSpawnedSnail && snailTrailPoints != null && !snailTrailPoints.isEmpty() && snailTrail == null) { // Trail lost
                PolylineOptions polylineOptions = new PolylineOptions()
                        .color(Color.argb(180, 255, 100, 0)).width(12).addAll(snailTrailPoints);
                snailTrail = mMap.addPolyline(polylineOptions);
            }

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGameService();
            } else {
                Toast.makeText(this, "Location permission required to play", Toast.LENGTH_LONG).show();
            }
        }
    }

    private int getSnailDrawableResId(String identifier) {
        Log.d("MainActivity_GetResId", "Attempting to get drawable resource ID for identifier: '" + identifier + "'");

        if (identifier == null) {
            identifier = DEFAULT_SNAIL_SPRITE_IDENTIFIER;
        }

        // First, try direct lookup
        int resourceId = getResources().getIdentifier(identifier, "drawable", getPackageName());

        // If not found, use manual fallback mapping
        if (resourceId == 0) {
            switch (identifier) {
                case "snail_spoon":
                    resourceId = R.drawable.spoon; // maps to spoon.png
                    break;
                case "snail_classic":
                    resourceId = R.drawable.snail;
                    break;
                case "snail_gun":
                    resourceId = R.drawable.snail_gun;
                    break;
                case "snail_scissors":
                    resourceId = R.drawable.snail_scissors;
                    break;
                case "snail_zombie":
                    resourceId = R.drawable.snail_zombie;
                    break;
                // Add more custom mappings here if needed
                default:
                    Log.w("MainActivity_GetResId", "Unknown snail sprite identifier: '" + identifier + "'. Using default drawable.");
                    resourceId = R.drawable.snail;
                    break;
            }
        }

        Log.i("MainActivity_GetResId", "Resolved identifier '" + identifier + "' to drawable resource ID: " + resourceId);
        return resourceId;
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG_MAIN_ACTIVITY, "onPause called");
        SharedPreferences.Editor editor = getSharedPreferences("GameSettings", MODE_PRIVATE).edit();
        editor.putBoolean("vibration", false);
        editor.apply();

        loadSelectedSnailSprite();

        // Persist the full game state including player/snail positions and the
        // current elapsed realtime value so we can properly resume later.
        saveGameState();

        if (snailMarker != null && hasSpawnedSnail) {
            updateSnailIcon(); // Optional: refresh the sprite icon while paused
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(gameOverReceiver,
                new IntentFilter("GAME_OVER"));
        stopLocationUpdates();
    }


    private final BroadcastReceiver gameOverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Show death screen, restart, etc.
            Toast.makeText(context, "You were caught!", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG_MAIN_ACTIVITY, "onResume called");
        // Activity returned to foreground
        isInBackground = false;
        // Minigame activities have ended when we return here
        isPlayingMinigame = false;
        SharedPreferences.Editor editor = getSharedPreferences("GameSettings", MODE_PRIVATE).edit();
        editor.putBoolean("vibration", true);
        editor.apply();
        // Load and update the snail sprite if it exists
        loadSelectedSnailSprite();
        if (snailMarker != null && hasSpawnedSnail) {
            updateSnailIcon();
        }
        TextView coinBalanceText = findViewById(R.id.coinBalanceText);
        int balance = getSharedPreferences("SnailCoins", MODE_PRIVATE).getInt("coin_balance", 0);
        coinBalanceText.setText("🪙 " + balance);
        // Register game over receiver
        LocalBroadcastManager.getInstance(this).registerReceiver(gameOverReceiver,
                new IntentFilter("GAME_OVER"));
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates();
        }

        if (currentPlayerLocation != null && snailPosition != null) {
            float[] results = new float[1];
            Location.distanceBetween(
                    currentPlayerLocation.latitude, currentPlayerLocation.longitude,
                    snailPosition.latitude, snailPosition.longitude,
                    results);
            updateDistanceDisplay(results[0]);
        }
    }

    private void loadSelectedSnailSprite() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        loadedSnailSpriteIdentifier = prefs.getString(KEY_SELECTED_SNAIL_SPRITE, DEFAULT_SNAIL_SPRITE_IDENTIFIER);
        Log.d("MainActivity_LoadSprite", "Attempting to load sprite with Key: " + KEY_SELECTED_SNAIL_SPRITE + ", Default: " + DEFAULT_SNAIL_SPRITE_IDENTIFIER);
        Log.i("MainActivity_LoadSprite", "Loaded sprite identifier from SharedPreferences: '" + loadedSnailSpriteIdentifier + "'");
    }

        @Override
        protected void onDestroy() {
            Log.d(TAG_MAIN_ACTIVITY, "onDestroy called");
            if (saltBombCooldownTimer != null) {
                saltBombCooldownTimer.cancel();
            }

            // ✅ Restore vibration when main game ends
            SharedPreferences.Editor editor = getSharedPreferences("GameSettings", MODE_PRIVATE).edit();
            editor.putBoolean("vibration", true);
            editor.apply();
            stopLocationUpdates();

            super.onDestroy();
        }



    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (!isPlayingMinigame) {
            showSnailMinigameNotification();
        }
    }

    private void showSnailMinigameNotification() {
        String channelId = "snail_warning_channel";
        String channelName = "Snail Warnings";

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.snail) // use your snail icon
                .setContentTitle("The Snail Wants to Play")
                .setContentText("Return to the game before it gets closer...")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setSilent(true)
                .build();

        notificationManager.notify(1001, notification);
    }
    // Show a notification that launches a specific minigame when tapped
    private void showMinigameReadyNotification(Class<?> activityClass) {
        String channelId = "snail_channel_id";
        String channelName = "Snail Alerts";

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    channelName,
                    NotificationManager.IMPORTANCE_HIGH
            );
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, activityClass);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.snail)
                .setContentTitle("The Snail Wants to Play")
                .setContentText("Tap to start the minigame!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();

        notificationManager.notify(1002, notification);
    }
    private void pushSnailBack() {
        double meters = 25 + Math.random() * 50; // Push back 20–40 meters
        Log.d("Minigame", "Pushing snail back by " + meters + " meters");
        moveSnailBy(-meters);
    }

    private void bringSnailCloser() {
        if (snailPosition == null || playerPosition == null) return;

        double metersToMove = 75 + Math.random() * 50; // 75–120m

        // Calculate current distance between snail and player in meters
        float[] results = new float[1];
        Location.distanceBetween(
                snailPosition.latitude, snailPosition.longitude,
                playerPosition.latitude, playerPosition.longitude,
                results
        );
        double currentDistance = results[0]; // in meters

        if (currentDistance <= 10.0) {
            Log.d("Minigame", "Snail already too close. Staying at 10m.");
            return; // already too close, do nothing
        }

        double allowedDistance = currentDistance - 10.0;
        double actualMove = Math.min(metersToMove, allowedDistance);

        Log.d("Minigame", "Bringing snail closer by " + actualMove + " meters (capped)");
        moveSnailBy(actualMove); // Positive = toward player
    }
    private void moveSnailBy(double deltaMeters) {
        if (snailPosition == null || playerPosition == null) {
            Log.w("Minigame", "Snail or player position not set — skipping movement.");
            return;
        }

        double lat1 = snailPosition.latitude;
        double lng1 = snailPosition.longitude;
        double lat2 = playerPosition.latitude;
        double lng2 = playerPosition.longitude;

        double deltaLat = lat2 - lat1;
        double deltaLng = lng2 - lng1;
        double distance = Math.sqrt(deltaLat * deltaLat + deltaLng * deltaLng);
        if (distance == 0) return;

        double unitLat = deltaLat / distance;
        double unitLng = deltaLng / distance;
        double metersToDegrees = deltaMeters / 111139.0;

        double newLat = lat1 + (unitLat * metersToDegrees);
        double newLng = lng1 + (unitLng * metersToDegrees);

        snailPosition = new LatLng(newLat, newLng);

        if (snailMarker != null) {
            snailMarker.setPosition(snailPosition); // ✅ Update visual position
        } else {
            Log.w("Minigame", "Snail marker is null — cannot update visual position.");
        }

        Log.d("Minigame", "Snail moved to: " + snailPosition);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        LinearLayout quitConfirmPanel = findViewById(R.id.quitConfirmPanel);
        if (quitConfirmPanel.getVisibility() == View.VISIBLE) {
            quitConfirmPanel.setVisibility(View.GONE);
        } else {
            quitConfirmPanel.setVisibility(View.VISIBLE);
        }
        // Don’t call super.onBackPressed()
    }


    private void saveGameStateForContinue() {
        if (playerPosition == null || snailPosition == null) return;

        SharedPreferences.Editor editor = getSharedPreferences("SnailGameState", MODE_PRIVATE).edit();
        editor.putLong("player_lat_before_pause", Double.doubleToLongBits(playerPosition.latitude));
        editor.putLong("player_lng_before_pause", Double.doubleToLongBits(playerPosition.longitude));
        editor.putLong("snail_lat_before_pause", Double.doubleToLongBits(snailPosition.latitude));
        editor.putLong("snail_lng_before_pause", Double.doubleToLongBits(snailPosition.longitude));
        editor.apply();

        Log.d("GameState", "Saved for continue: Player@" + playerPosition + " Snail@" + snailPosition);
    }

    private void startSnailAbilityCycle() {
        snailAbilityRunnable = new Runnable() {
            @Override
            public void run() {
                triggerRandomSnailAbility();
                snailAbilityHandler.postDelayed(this, ABILITY_INTERVAL_MS);
            }
        };
        snailAbilityHandler.postDelayed(snailAbilityRunnable, ABILITY_INTERVAL_MS);
    }
    private void clearPowerUpInventory() {
        SharedPreferences.Editor editor = getSharedPreferences("PowerUpInventory", MODE_PRIVATE).edit();
        editor.clear();  // 🔥 Clears saltBomb, decoyShell, shellShield counts
        editor.apply();
    }
    private void triggerRandomSnailAbility() {
        Random rand = new Random();
        int abilityIndex = rand.nextInt(4); // 0 = teleport, 1 = decoy, 2 = vanish, 3 = shell split

        switch (abilityIndex) {
            case 0:
                teleportSnailRandomly();
                break;
            case 1:
                spawnFakeSnail();
                break;
            case 2:
                vanishSnailTemporarily();
                break;
            case 3:
                triggerShellSplit();
                break;
        }
    }
    private void teleportSnailRandomly() {
        if (snailPosition == null) return;

        Random rand = new Random();
        double meters = 40 + rand.nextInt(21); // 40–60 meters
        double angle = rand.nextDouble() * 2 * Math.PI;

        double latOffset = metersToLatitudeDegrees(meters * Math.sin(angle));
        double lngOffset = metersToLongitudeDegrees(meters * Math.cos(angle), snailPosition.latitude);

        snailPosition = new LatLng(snailPosition.latitude + latOffset, snailPosition.longitude + lngOffset);
        if (snailMarker != null) snailMarker.setPosition(snailPosition);
        Toast.makeText(this, "The snail teleported...", Toast.LENGTH_SHORT).show();
    }
    private void spawnFakeSnail() {
        if (playerPosition == null || mMap == null) return;

        Random rand = new Random();
        double meters = 60 + rand.nextInt(41); // 60–100 meters
        double angle = rand.nextDouble() * 2 * Math.PI;

        double latOffset = metersToLatitudeDegrees(meters * Math.sin(angle));
        double lngOffset = metersToLongitudeDegrees(meters * Math.cos(angle), playerPosition.latitude);

        LatLng startFakePos = new LatLng(
                playerPosition.latitude + latOffset,
                playerPosition.longitude + lngOffset
        );

        SharedPreferences prefs = getSharedPreferences("GameSettings", MODE_PRIVATE);
        String selectedSpriteKey = prefs.getString("selectedSnailSprite", "snail_classic");
        int spriteResId = getResources().getIdentifier(selectedSpriteKey, "drawable", getPackageName());

        if (spriteResId == 0) {
            Log.w("FakeSnail", "Unknown sprite key: " + selectedSpriteKey + ". Using fallback.");
            spriteResId = R.drawable.snail;
        }

        Drawable drawable = ContextCompat.getDrawable(this, spriteResId);
        if (!(drawable instanceof BitmapDrawable)) {
            Log.e("FakeSnail", "Selected sprite is not a bitmap drawable.");
            return;
        }

        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 80, 80, false);

        if (fakeSnailMarker != null) {
            fakeSnailMarker.remove();
        }

        fakeSnailMarker = mMap.addMarker(new MarkerOptions()
                .position(startFakePos)
                .icon(BitmapDescriptorFactory.fromBitmap(scaledBitmap))
                .title("???"));

        // 🚫 HIDE the real snail during decoy
        if (snailMarker != null) {
            snailMarker.setVisible(false);
        }

        Toast.makeText(this, "A fake snail appeared...", Toast.LENGTH_SHORT).show();

        fakeSnailStartTime = System.currentTimeMillis();
        fakeSnailHandler = new Handler();
        fakeSnailRunnable = new Runnable() {
            @Override
            public void run() {
                if (fakeSnailMarker == null || playerPosition == null) return;

                long elapsed = System.currentTimeMillis() - fakeSnailStartTime;
                if (elapsed > FAKE_SNAIL_DURATION_MS) {
                    // ⏱️ Time's up — remove fake and show real snail again
                    fakeSnailMarker.remove();
                    fakeSnailMarker = null;

                    if (snailMarker != null) {
                        snailMarker.setVisible(true); // 👀 Reveal real snail again
                    }
                    return;
                }

                LatLng current = fakeSnailMarker.getPosition();
                LatLng target = playerPosition;

                LatLng next = moveTowards(current, target, FAKE_SNAIL_MOVE_METERS_PER_SECOND * 0.25f);
                fakeSnailMarker.setPosition(next);

                fakeSnailHandler.postDelayed(this, 250);
            }
        };

        fakeSnailHandler.post(fakeSnailRunnable);
    }

    private LatLng moveTowards(LatLng from, LatLng to, double stepMeters) {
        double distance = distanceBetween(from.latitude, from.longitude, to.latitude, to.longitude);
        if (distance < stepMeters || distance == 0) return to;

        double fraction = stepMeters / distance;

        double newLat = from.latitude + (to.latitude - from.latitude) * fraction;
        double newLng = from.longitude + (to.longitude - from.longitude) * fraction;

        return new LatLng(newLat, newLng);
    }


    private void vanishSnailTemporarily() {
        if (snailMarker == null) return;

        snailMarker.setVisible(false);
        snailInvisible = true;

        Toast.makeText(this, "The snail has vanished...", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(() -> {
            if (snailMarker != null) snailMarker.setVisible(true);
            snailInvisible = false;
            Toast.makeText(this, "The snail reappeared...", Toast.LENGTH_SHORT).show();
        }, 20000); // 20 seconds
    }
    private void triggerShellSplit() {
        if (shellSplitActive || snailPosition == null || mMap == null) return;

        shellSplitActive = true;
        shellSplitEndTimeMs = SystemClock.elapsedRealtime() + SHELL_SPLIT_DURATION_MS;

        int spriteResId = getResources().getIdentifier(loadedSnailSpriteIdentifier, "drawable", getPackageName());
        if (spriteResId == 0) spriteResId = R.drawable.snail;
        Drawable drawable = ContextCompat.getDrawable(this, spriteResId);
        if (!(drawable instanceof BitmapDrawable)) return;
        Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 2, bitmap.getHeight() / 2, false);
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(scaled);

        double offsetMeters = 5.0;
        double angle = Math.random() * 2 * Math.PI;
        double latOffset = metersToLatitudeDegrees(offsetMeters * Math.sin(angle));
        double lngOffset = metersToLongitudeDegrees(offsetMeters * Math.cos(angle), snailPosition.latitude);

        splitPos1 = new LatLng(snailPosition.latitude + latOffset, snailPosition.longitude + lngOffset);
        splitPos2 = new LatLng(snailPosition.latitude - latOffset, snailPosition.longitude - lngOffset);

        splitMarker1 = mMap.addMarker(new MarkerOptions().position(splitPos1).icon(icon).title("Split Snail"));
        splitMarker2 = mMap.addMarker(new MarkerOptions().position(splitPos2).icon(icon).title("Split Snail"));

        if (snailMarker != null) snailMarker.setVisible(false);
    }

    private LatLng moveTowardsWithOffset(LatLng from, LatLng to, double stepMeters, double angleOffsetRad) {
        double angle = Math.atan2(to.latitude - from.latitude, to.longitude - from.longitude);
        angle += angleOffsetRad;

        double latOffset = metersToLatitudeDegrees(stepMeters * Math.sin(angle));
        double lngOffset = metersToLongitudeDegrees(stepMeters * Math.cos(angle), from.latitude);
        return new LatLng(from.latitude + latOffset, from.longitude + lngOffset);
    }

    private void handleShellSplit(Runnable continuation) {
        LatLng target = isDecoyActive && SystemClock.elapsedRealtime() < decoyEndTimeMs
                ? decoyPosition
                : currentPlayerLocation;

        float baseSpeed = getSnailMetersPerSecond(currentSnailSpeedSetting) * snailSpeedMultiplier;
        double step = baseSpeed * updateIntervalMs / 1000.0 / 2.0; // half speed

        splitPos1 = moveTowardsWithOffset(splitPos1, target, step, Math.toRadians(10));
        splitPos2 = moveTowardsWithOffset(splitPos2, target, step, Math.toRadians(-10));

        if (splitMarker1 != null) splitMarker1.setPosition(splitPos1);
        if (splitMarker2 != null) splitMarker2.setPosition(splitPos2);

        if (SystemClock.elapsedRealtime() > shellSplitEndTimeMs) {
            if (isSaltBombActive) {
                if (splitMarker1 != null) splitMarker1.remove();
                if (splitMarker2 != null) splitMarker2.remove();
                if (snailMarker != null) snailMarker.remove();
                snailMarker = null;
            } else {
                snailPosition = new LatLng(
                        (splitPos1.latitude + splitPos2.latitude) / 2,
                        (splitPos1.longitude + splitPos2.longitude) / 2);
                if (snailMarker == null) {
                    BitmapDescriptor snailIcon = getSnailBitmapDescriptor(loadedSnailSpriteIdentifier);
                    snailMarker = mMap.addMarker(new MarkerOptions().position(snailPosition).icon(snailIcon).title("Snail"));
                } else {
                    snailMarker.setPosition(snailPosition);
                    snailMarker.setVisible(true);
                }
                if (splitMarker1 != null) splitMarker1.remove();
                if (splitMarker2 != null) splitMarker2.remove();
            }
            shellSplitActive = false;
        }

        if (!isGameOver && snailHandler != null) {
            snailHandler.postDelayed(continuation, updateIntervalMs);
        }
    }
    // Inside MainActivity.java
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        SharedPreferences prefs = getSharedPreferences(MINIGAME_PREFS, MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_PLAYED_DATE, System.currentTimeMillis()).apply();
        // We have returned from another activity; any minigame is finished
        isPlayingMinigame = false;
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("MainActivity", "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);
        if (requestCode == REQUEST_CODE_HOLD && data != null) {
            boolean success = data.getBooleanExtra("minigameSuccess", false);
            if (success) {
                pushSnailBack(); // or pushSnailBack(40–60)
            } else {
                bringSnailCloser(); // or bringSnailCloser(60–100)
            }
        }
        // Handle Game Over result
        if (requestCode == REQUEST_CODE_GAME_OVER) {
            switch (resultCode) {
                case GameOverActivity.RESULT_RETRY:
                    Log.d("MainActivity", "Retrying game...");
                    resetGame();
                    break;

                case GameOverActivity.RESULT_REVIVE:
                    Log.d("MainActivity", "Player Revived!");
                    handleRevive(); // Call the revive handler
                    break;

                case GameOverActivity.RESULT_MAIN_MENU:
                default:
                    Log.d("MainActivity", "Game Over, navigating to Main Menu. Result: " + resultCode);
                    clearGameStatePrefs();
                    Intent intent = new Intent(MainActivity.this, MainMenuActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                    break;
            }
        }

        // ✅ Handle Minigame Result (should not be inside the GameOver block!)
        if ((requestCode == 222 || requestCode == 333) && data != null) {
            boolean success = data.getBooleanExtra("minigameSuccess", false);
            Log.d("SnailDebug", "snailPosition = " + snailPosition);
            Log.d("SnailDebug", "playerPosition = " + playerPosition);
            Log.d("SnailDebug", "snailMarker = " + snailMarker);

            if (requestCode == 222) {
                if (success) {
                    pushSnailBack();
                } else {
                    bringSnailCloser();
                }

            } else if (requestCode == 333) {
                if (success) {
                    moveSnailBy(-25);
                } else {
                    if (snailPosition == null || playerPosition == null) {
                        Log.w("MainActivity", "Cannot move snail closer: snailPosition or playerPosition is null.");
                        return;
                    }

                    float[] results = new float[1];
                    Location.distanceBetween(
                            snailPosition.latitude, snailPosition.longitude,
                            playerPosition.latitude, playerPosition.longitude,
                            results
                    );
                    double currentDistance = results[0];
                    double allowedMove = Math.max(0, currentDistance - 10.0);
                    moveSnailBy(Math.min(50, allowedMove));
                }
            }

            prefs.edit().putLong(KEY_LAST_PLAYED_DATE, System.currentTimeMillis()).apply();
        }


    }
}
// Also, when the game is won/lost in triggerGameOver, it calls clearGameStatePrefs.
// resetGame will also call it, ensuring a clean slate for retry.