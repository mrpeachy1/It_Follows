package com.itfollows.game;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;

public class MusicManager {

    private static MediaPlayer player;
    private static boolean isInitialized = false;

    public static void start(Context context) {
        if (isInitialized) return;

        player = MediaPlayer.create(context.getApplicationContext(), R.raw.ambience01);
        player.setLooping(true);
        setVolume(context);
        player.start();
        isInitialized = true;
    }

    public static void setVolume(Context context) {
        if (player != null) {
            SharedPreferences prefs = context.getSharedPreferences("GameSettings", Context.MODE_PRIVATE);
            int volume = prefs.getInt("volume", 50); // 0–100 from slider
            float scaledVolume = (volume / 100f) * 0.10f; // 👈 cap at 60% system volume
            player.setVolume(scaledVolume, scaledVolume);
        }
    }

    public static void pause() {
        if (player != null && player.isPlaying()) {
            player.pause();
        }
    }

    public static void resume() {
        if (player != null && !player.isPlaying()) {
            player.start();
        }
    }

    public static void stop() {
        if (player != null) {
            player.stop();
            player.release();
            player = null;
            isInitialized = false;
        }
    }
}
