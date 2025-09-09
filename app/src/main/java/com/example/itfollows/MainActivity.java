package com.example.itfollows;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class MainActivity extends AppCompatActivity {
    private TextView distanceText;
    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            updateDistanceUI();
        }
    };
    private final BroadcastReceiver gameOverReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            // TODO: show game over dialog / navigate
        }
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_maps);
        distanceText = findViewById(R.id.snailDistanceText);
        ContextCompat.startForegroundService(this, new Intent(this, GameService.class));
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(tickReceiver, new IntentFilter("GAME_TICK"));
        lbm.registerReceiver(gameOverReceiver, new IntentFilter(GameService.ACTION_GAME_OVER));
        updateDistanceUI();
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(tickReceiver);
        lbm.unregisterReceiver(gameOverReceiver);
    }

    private void updateDistanceUI() {
        GameStateRepo repo = GameStateRepo.getInstance(this);
        double[] p = repo.getPlayerLatLng();
        double[] s = repo.getSnailLatLng();
        if (p == null || s == null) return;
        double d = GeoMath.haversineMeters(p[0], p[1], s[0], s[1]);
        if (distanceText != null) {
            distanceText.setText(String.format("Snail: %.1f m", d));
        }
    }
}
