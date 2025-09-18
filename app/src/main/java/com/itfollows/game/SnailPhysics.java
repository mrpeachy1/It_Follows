package com.itfollows.game;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import androidx.core.app.NotificationCompat;
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
            repo.setSnailProximityAlertShown(false);
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

        if (d <= 25.0) {
            if (!repo.isSnailProximityAlertShown() && shouldShowBackgroundAlert()) {
                repo.setSnailProximityAlertShown(true);
                showSnailCloseNotification();
            }
        } else if (d >= 30.0 && repo.isSnailProximityAlertShown()) {
            repo.setSnailProximityAlertShown(false);
        }

        if (d < repo.getGameOverRadiusMeters()) {
            LocalBroadcastManager.getInstance(app).sendBroadcast(
                    new Intent(GameService.ACTION_GAME_OVER));

            if (shouldShowBackgroundAlert()) {
                showCaughtNotification();
            }
        }
    }

    private boolean isAppInForeground() {
        ActivityManager am = (ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        for (ActivityManager.RunningAppProcessInfo info : am.getRunningAppProcesses()) {
            if (info.processName.equals(app.getPackageName())) {
                int imp = info.importance;
                return imp == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND ||
                        imp == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE;
            }
        }
        return false;
    }

    private boolean shouldShowBackgroundAlert() {
        PowerManager pm = (PowerManager) app.getSystemService(Context.POWER_SERVICE);
        boolean screenOn = pm != null && pm.isInteractive();
        boolean foreground = isAppInForeground();
        return !screenOn || !foreground;
    }

    private void showCaughtNotification() {
        String channelId = "snail_caught_channel";
        NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        ensureSnailAlertChannel(nm, channelId);

        Intent intent = new Intent(app, GameActivity.class);
        PendingIntent pi = PendingIntent.getActivity(app, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(app, channelId)
                .setSmallIcon(R.drawable.snail)
                .setContentTitle("The snail caught you")
                .setContentText("Tap to see what happened.")
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();

        nm.notify(2001, notification);
    }

    private void showSnailCloseNotification() {
        String channelId = "snail_caught_channel";
        NotificationManager nm = (NotificationManager) app.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        ensureSnailAlertChannel(nm, channelId);

        Intent intent = new Intent(app, GameActivity.class);
        PendingIntent pi = PendingIntent.getActivity(app, 1, intent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(app, channelId)
                .setSmallIcon(R.drawable.snail)
                .setContentTitle("The snail is nearby")
                .setContentText("It's within 25 meters. Get moving!")
                .setAutoCancel(true)
                .setContentIntent(pi)
                .build();

        nm.notify(2002, notification);
    }

    private void ensureSnailAlertChannel(NotificationManager nm, String channelId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel existing = nm.getNotificationChannel(channelId);
            if (existing == null) {
                NotificationChannel ch = new NotificationChannel(channelId, "Snail Alerts", NotificationManager.IMPORTANCE_HIGH);
                nm.createNotificationChannel(ch);
            }
        }
    }
}
