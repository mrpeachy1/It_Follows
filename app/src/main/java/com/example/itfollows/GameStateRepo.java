package com.example.itfollows;

import android.content.Context;
import android.content.SharedPreferences;

public class GameStateRepo {
    private static final String PREF = "GameState";
    private static GameStateRepo I;
    private final SharedPreferences sp;
    private final Context app;

    public static synchronized GameStateRepo getInstance(Context c) {
        if (I == null) I = new GameStateRepo(c.getApplicationContext());
        return I;
    }

    private GameStateRepo(Context c) {
        this.app = c;
        this.sp = c.getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public Context ctx() {
        return app;
    }

    public void setPlayerLatLng(double lat, double lng, long ts) {
        sp.edit()
                .putLong("playerTs", ts)
                .putLong("playerLatE6", (long) (lat * 1e6))
                .putLong("playerLngE6", (long) (lng * 1e6))
                .apply();
    }

    public double[] getPlayerLatLng() {
        if (!sp.contains("playerLatE6")) return null;
        return new double[]{
                sp.getLong("playerLatE6", 0) / 1e6,
                sp.getLong("playerLngE6", 0) / 1e6
        };
    }

    public long getPlayerTs() {
        return sp.getLong("playerTs", System.currentTimeMillis());
    }

    public void setSnailLatLng(double lat, double lng, long ts) {
        sp.edit()
                .putLong("snailTs", ts)
                .putLong("snailLatE6", (long) (lat * 1e6))
                .putLong("snailLngE6", (long) (lng * 1e6))
                .apply();
    }

    public double[] getSnailLatLng() {
        if (!sp.contains("snailLatE6")) return null;
        return new double[]{
                sp.getLong("snailLatE6", 0) / 1e6,
                sp.getLong("snailLngE6", 0) / 1e6
        };
    }

    public long getSnailTs() {
        return sp.getLong("snailTs", -1L);
    }

    public void setLastSnailUpdateMs(long ts) {
        sp.edit().putLong("lastSnailUpdateMs", ts).apply();
    }

    public long getLastSnailUpdateMsOr(long def) {
        return sp.getLong("lastSnailUpdateMs", def);
    }

    public float getGameOverRadiusMeters() {
        return sp.getFloat("gameOverRadiusM", 5f);
    }

    public double getCurrentSnailSpeedMps() {
        String label = sp.getString("snailSpeed", "Normal Chase");
        if ("Snail IRL".equals(label)) return 0.001;
        if ("Aggressive Slime Pursuit".equals(label)) return 2.0;
        return 0.6;
    }

    public void flush() {
        // no-op; using apply()
    }
}
