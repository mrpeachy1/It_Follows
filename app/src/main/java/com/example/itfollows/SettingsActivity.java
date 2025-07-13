package com.example.itfollows;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
// Import Spinner and ArrayAdapter if you switched to Spinners
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner; // Import Spinner
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.content.Context;
// Remove AutoCompleteTextView if no longer used, or keep if used elsewhere
// import android.widget.AutoCompleteTextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.RecyclerView;
import com.example.itfollows.models.SnailSprite;

public class SettingsActivity extends AppCompatActivity {

    private SeekBar volumeSlider;
    // Change to Spinners if your layout uses them
    private Spinner speedDropdown;
    private Spinner distanceDropdown;
    private Spinner measurementUnitDropdown;
    private Button applyButton;
    private SnailSpriteAdapter snailSpriteAdapter;
    private List<SnailSprite> spriteList;
    private String currentSelectedSpriteIdentifier;
    private static final String PREFS_NAME = "GameSettings"; // Ensure this matches MainActivity
    public static final String KEY_SELECTED_SNAIL_SPRITE = "selectedSnailSprite";
    // Update options if they are different for Spinners
    private final String[] speedOptions = {"Sluggish Crawl","Fast Snail","Turtle Speed","Casual Walk","Power Walk","Jogging Snail","Running Snail","Olympic Sprinting Snail","Snail Drives Car"};
    private final String[] distanceOptions = {
            "Very Close: 5–25m / 16–80ft",       // For slow speeds like Sluggish Crawl / Fast Snail
            "Close: 10–50m / 30–160ft",          // For Normal Snail and Turtle speeds
            "Distant: 50–100m / 160–330ft",      // For casual walk to jogging
            "Far: 100–200m / 0.06–0.12mi",       // For jogging to running speeds
            "Very Far: 200–400m / 0.12–0.25mi",  // For Olympic Sprinter-level chase
            "Extreme: 400–800m / 0.25–0.50mi",     // For Car Speed — gives players a head start
    };
    private final String[] unitOptions = {"Metric", "Imperial"};
    public static final String DEFAULT_SNAIL_SPRITE_IDENTIFIER = "snail_classic";
    public static final String KEY_MEASUREMENT_UNIT = "measurementUnit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageView glow = findViewById(R.id.panelGlow);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.glow_pulse);
        glow.startAnimation(pulse);

        MusicManager.start(this);

        volumeSlider = findViewById(R.id.volumeSlider);
        speedDropdown = findViewById(R.id.snailSpeedDropdown);
        distanceDropdown = findViewById(R.id.snailDistanceDropdown);
        measurementUnitDropdown = findViewById(R.id.measurementUnitDropdown);
        applyButton = findViewById(R.id.applyButton);
        LinearLayout snailSpriteContainer = findViewById(R.id.snailSpriteContainer);

        // Setup dropdowns
        setupSpinner(speedDropdown, speedOptions);
        setupSpinner(distanceDropdown, distanceOptions);
        setupSpinner(measurementUnitDropdown, unitOptions);

        // Load sprite options
        spriteList = new ArrayList<>();
        setupSnailSpritesList();

        // Load saved sprite identifier
        SharedPreferences gameSettingsPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentSelectedSpriteIdentifier = gameSettingsPrefs.getString(
                "selectedSnailSprite",
                DEFAULT_SNAIL_SPRITE_IDENTIFIER
        );
        Log.d("SettingsActivity", "Initial loaded sprite: " + currentSelectedSpriteIdentifier);

        // Build horizontal sprite list using list_item_snail_sprite.xml
        snailSpriteContainer.removeAllViews();

        for (SnailSprite sprite : spriteList) {
            View spriteItem = LayoutInflater.from(this).inflate(R.layout.list_item_snail_sprite, snailSpriteContainer, false);

            ImageView image = spriteItem.findViewById(R.id.spriteImageView);
            TextView label = spriteItem.findViewById(R.id.spriteNameTextView);
            ImageView selectionIndicator = spriteItem.findViewById(R.id.selectionIndicator);

            image.setImageResource(sprite.thumbnailResId);
            label.setText(sprite.name);

            // Show indicator only if it's the selected sprite
            if (sprite.identifier.equals(currentSelectedSpriteIdentifier)) {
                selectionIndicator.setVisibility(View.VISIBLE);
            } else {
                selectionIndicator.setVisibility(View.GONE);
            }

            spriteItem.setOnClickListener(v -> {
                currentSelectedSpriteIdentifier = sprite.identifier;

                // Hide all indicators first
                for (int i = 0; i < snailSpriteContainer.getChildCount(); i++) {
                    View child = snailSpriteContainer.getChildAt(i);
                    ImageView indicator = child.findViewById(R.id.selectionIndicator);
                    if (indicator != null) indicator.setVisibility(View.GONE);
                }

                // Show selection for the clicked sprite
                selectionIndicator.setVisibility(View.VISIBLE);
            });

            snailSpriteContainer.addView(spriteItem);
        }

        // Load other settings (volume, dropdowns)
        loadPreferences();

        applyButton.setOnClickListener(v -> {
            saveSettings(); // saves volume, dropdowns, and currentSelectedSpriteIdentifier
            MusicManager.setVolume(this);
            Toast.makeText(this, "Settings applied", Toast.LENGTH_SHORT).show();
            finish(); // Go back to previous screen
        });
    }

    private void setupSnailSpritesList() {
        spriteList.clear(); // Clear before adding, in case this method is called multiple times
        spriteList.add(new SnailSprite(
                "Knife Snail",    // User-friendly name
                "snail_classic",    // Unique identifier
                R.drawable.snail,   // Thumbnail resource ID (e.g., snail.png)
                R.drawable.snail    // Game sprite resource ID (can be the same or different)
        ));
        spriteList.add(new SnailSprite(
                "Spoon Snail",      // User-friendly name
                "snail_spoon",      // Unique identifier
                R.drawable.spoon,   // Thumbnail resource ID (e.g., spoon.png)
                R.drawable.spoon    // Game sprite resource ID
        ));
        spriteList.add(new SnailSprite(
                "Zombie Snail",      // User-friendly name
                "snail_zombie",      // Unique identifier
                R.drawable.snail_zombie,   // Thumbnail resource ID (e.g., spoon.png)
                R.drawable.snail_zombie    // Game sprite resource ID
        ));
        spriteList.add(new SnailSprite(
                "Scissors Snail",      // User-friendly name
                "snail_scissors",      // Unique identifier
                R.drawable.snail_scissors,   // Thumbnail resource ID (e.g., spoon.png)
                R.drawable.snail_scissors    // Game sprite resource ID
        ));
        spriteList.add(new SnailSprite(
                "Gun Snail",      // User-friendly name
                "snail_gun",      // Unique identifier
                R.drawable.snail_gun,   // Thumbnail resource ID (e.g., spoon.png)
                R.drawable.snail_gun    // Game sprite resource ID
        ));
        // Add more SnailSprite objects here for more choices
        Log.d("SettingsActivity", "setupSnailSpritesList: " + spriteList.size() + " sprites added.");
    }

    private void setupSnailSpriteRecyclerView() {
        if (spriteList == null || spriteList.isEmpty()) {
            Log.e("SettingsActivity", "Sprite list is empty or null. Cannot setup RecyclerView.");
            return;
        }
        // The adapter needs the initially selected identifier to highlight correctly
        snailSpriteAdapter = new SnailSpriteAdapter(this, spriteList, currentSelectedSpriteIdentifier, sprite -> {
            // When an item is clicked in the RecyclerView, update our temporary selection
            currentSelectedSpriteIdentifier = sprite.identifier;
            Log.d("SettingsActivity", "Sprite selected: " + currentSelectedSpriteIdentifier);
            Log.d("SettingsActivity_SpriteSelect", "Sprite selected in RecyclerView. Identifier: " + sprite.identifier);
            // The adapter itself should handle visual updates (highlighting)
        });

        // Use GridLayoutManager for a grid layout, adjust '3' for the number of columns
        Log.d("SettingsActivity", "RecyclerView adapter set.");
    }

    private void setupSpinner(Spinner spinner, String[] options) {
        if (spinner == null) {
            Log.e("SettingsActivity", "Spinner is null, cannot setup.");
            return;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_white, // Ensure this layout exists
                options
        );
        // Ensure this layout also exists
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
    }
    // In MusicManager.java
    private static MediaPlayer mediaPlayer; // Assuming you have one

    @OptIn(markerClass = UnstableApi.class)
    public static void updateLiveVolume(float volumeLevel) {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.setVolume(volumeLevel, volumeLevel);
            } catch (IllegalStateException e) {
                // Handle case where mediaPlayer might not be in a valid state
                androidx.media3.common.util.Log.e("MusicManager", "Error setting live volume", e);
            }
        }
    }

    // Your existing setVolume method might look like this:
    @OptIn(markerClass = UnstableApi.class)
    public static void setVolume(Context context) {
        if (mediaPlayer != null && context != null) {
            SharedPreferences prefs = context.getSharedPreferences("GameSettings", android.content.Context.MODE_PRIVATE);
            // OR, if you've imported android.content.Context, simply:
            // SharedPreferences prefs = context.getSharedPreferences("GameSettings", Context.MODE_PRIVATE);
            int savedVolumePercent = prefs.getInt("volume", 50); // Default to 50%
            float volumeLevel = savedVolumePercent / 100f;
            try {
                mediaPlayer.setVolume(volumeLevel, volumeLevel);
                androidx.media3.common.util.Log.d("MusicManager", "Volume set from prefs: " + volumeLevel);
            } catch (IllegalStateException e) {
                androidx.media3.common.util.Log.e("MusicManager", "Error setting volume from prefs", e);
            }
        }
    }

    public static void start(Context context) {
        // ... your existing start logic ...
        // Ensure volume is set when music starts/restarts
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            // ...
        }
        setVolume(context); // Apply saved volume when starting
        // ...
    }

    // Helper method to setup a Spinner

    // You'll need to adapt loadPreferences if you switched to Spinners
    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        volumeSlider.setProgress(prefs.getInt("volume", 50));

        // Load preferences for Spinners
        String savedSpeed = prefs.getString("snailSpeed", "Normal Chase");
        setSpinnerSelection(speedDropdown, speedOptions, savedSpeed);

        String savedDistance = prefs.getString("snailDistance", "Distant");
        setSpinnerSelection(distanceDropdown, distanceOptions, savedDistance);

        String savedUnit = prefs.getString(KEY_MEASUREMENT_UNIT, "Metric");
        setSpinnerSelection(measurementUnitDropdown, unitOptions, savedUnit);

        Log.d("MainActivity_Settings", "Speed: " + savedSpeed + ", Distance: " + savedDistance + ", Units: " + savedUnit);
        Log.d("SettingsActivity", "Preferences loaded. Volume: " + volumeSlider.getProgress() +
                ", Speed: " + savedSpeed + ", Distance: " + savedDistance +
                ", Units: " + savedUnit +
                ", Sprite: " + currentSelectedSpriteIdentifier);
    }
    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        // Save volume
        editor.putInt("volume", volumeSlider.getProgress());
        Log.d("SettingsActivity_Save", "Attempting to save sprite. Identifier: " + currentSelectedSpriteIdentifier + ", Key: " + KEY_SELECTED_SNAIL_SPRITE);
        editor.putString(KEY_SELECTED_SNAIL_SPRITE, currentSelectedSpriteIdentifier);
        boolean success = editor.commit(); // Use commit() for immediate result and logging
        if (success) {
            Log.i("SettingsActivity_Save", "Sprite saved successfully to SharedPreferences.");
        } else {
            Log.e("SettingsActivity_Save", "FAILED to save sprite to SharedPreferences!");
        }
        // Save selected items from Spinners
        if (speedDropdown.getSelectedItem() != null) {
            editor.putString("snailSpeed", speedDropdown.getSelectedItem().toString());
        }
        if (distanceDropdown.getSelectedItem() != null) {
            editor.putString("snailDistance", distanceDropdown.getSelectedItem().toString());
        }
        if (measurementUnitDropdown.getSelectedItem() != null) {
            editor.putString(KEY_MEASUREMENT_UNIT, measurementUnitDropdown.getSelectedItem().toString());
        }

        // Save the selected snail sprite identifier
        editor.putString("selectedSnailSprite", currentSelectedSpriteIdentifier);

        editor.putString(KEY_SELECTED_SNAIL_SPRITE, currentSelectedSpriteIdentifier);
        Log.d("SettingsActivity", "Settings saved. Volume: " + volumeSlider.getProgress() +
                ", Speed: " + (speedDropdown.getSelectedItem() != null ? speedDropdown.getSelectedItem().toString() : "N/A") +
                ", Distance: " + (distanceDropdown.getSelectedItem() != null ? distanceDropdown.getSelectedItem().toString() : "N/A") +
                ", Units: " + (measurementUnitDropdown.getSelectedItem() != null ? measurementUnitDropdown.getSelectedItem().toString() : "N/A") +
                ", Sprite: " + currentSelectedSpriteIdentifier);
        editor.apply();
    }
    // Helper method to set Spinner selection
    private void setSpinnerSelection(Spinner spinner, String[] options, String value) {
        if (spinner == null || options == null) return;
        for (int i = 0; i < options.length; i++) {
            if (options[i].equals(value)) {
                spinner.setSelection(i);
                return;
            }
        }
        // Optionally, set to a default if the saved value isn't in options
        if (options.length > 0) {
            spinner.setSelection(0);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MusicManager.setVolume(this);
        MusicManager.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MusicManager.pause();
    }
}