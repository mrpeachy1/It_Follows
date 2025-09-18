package com.itfollows.game;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.concurrent.TimeUnit;

public class ReconcileWorker extends Worker {
    public ReconcileWorker(@NonNull Context ctx, @NonNull WorkerParameters p) {
        super(ctx, p);
    }

    @NonNull
    @Override
    public Result doWork() {
        long now = System.currentTimeMillis();
        SnailPhysics.getInstance(getApplicationContext()).advanceSnailTowardPlayer(now);
        return Result.success();
    }
}

class ReconcileScheduler {
    static void schedule(Context ctx) {
        Constraints c = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build();
        PeriodicWorkRequest wr = new PeriodicWorkRequest.Builder(ReconcileWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(c)
                .build();
        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "reconcile", ExistingPeriodicWorkPolicy.UPDATE, wr);
    }
}
