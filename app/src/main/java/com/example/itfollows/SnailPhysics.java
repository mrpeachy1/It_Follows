package com.example.itfollows;

import android.content.Context;
import android.content.Intent;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class SnailPhysics {
    private static SnailPhysics I;

    public static synchronized SnailPhysics getInstance(Context c) {
        if (I == null) I = new SnailPhysics(c.getApplicationContext());
        return I;
    }

    private final Context app;
    private final GameStateRepo repo;

    private SnailPhysics(Context c) {
        this.app = c;
        this.repo = GameStateRepo.getInstance(c);
    }

    public void advanceSnailTowardPlayer(long nowMs) {
        long last = repo.getLastSnailUpdateMsOr(nowMs);
        long dt = Math.max(0, nowMs - last);
        repo.setLastSnailUpdateMs(nowMs);

        double[] snail = repo.getSnailLatLng();
        double[] player = repo.getPlayerLatLng();
        if (player == null) return;

        if (snail == null) {
            double[] moved = GeoMath.moveToward(
                    player[0], player[1],
                    player[0], player[1] - 0.0005,
                    50.0);
            repo.setSnailLatLng(moved[0], moved[1], nowMs);
            return;
        }

        double speed = repo.getCurrentSnailSpeedMps();
        double distToMove = speed * (dt / 1000.0);

        double[] moved = GeoMath.moveToward(
                snail[0], snail[1],
                player[0], player[1],
                distToMove);
        repo.setSnailLatLng(moved[0], moved[1], nowMs);

        double d = GeoMath.haversineMeters(moved[0], moved[1], player[0], player[1]);
        if (d < repo.getGameOverRadiusMeters()) {
            LocalBroadcastManager.getInstance(app).sendBroadcast(new Intent("ACTION_GAME_OVER"));
        }
    }
}
