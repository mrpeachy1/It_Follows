package com.itfollows.game;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.PowerManager;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.location.LocationResult;

public class LocationUpdatesReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context ctx, Intent intent) {
        if (!"LOC_TICK".equals(intent.getAction())) return;

        LocationResult result = LocationResult.extractResult(intent);
        if (result == null) return;

        GameStateRepo repo = GameStateRepo.getInstance(ctx);
        SnailPhysics physics = SnailPhysics.getInstance(ctx);

        PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "ItFollows:loc");
        wl.acquire(3000);

        try {
            for (Location loc : result.getLocations()) {
                repo.setPlayerLatLng(loc.getLatitude(), loc.getLongitude(), loc.getTime());
                physics.advanceSnailTowardPlayer(loc.getTime());
            }
            repo.flush();
            LocalBroadcastManager.getInstance(ctx).sendBroadcast(new Intent("GAME_TICK"));
        } finally {
            if (wl.isHeld()) wl.release();
        }
    }
}
