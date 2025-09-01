package com.example.itfollows.avatar;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;

public class AvatarStorage {
    private static final String PREFS = "AvatarPrefs";
    private static final String KEY = "avatarConfig";
    private static final Gson gson = new Gson();

    public static AvatarConfig load(Context ctx) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String json = sp.getString(KEY, null);
        if (json == null) return AvatarPresets.defaultPreset();
        try {
            AvatarConfig cfg = gson.fromJson(json, AvatarConfig.class);
            if (cfg == null || cfg.pixels == null || cfg.pixels.length != AvatarConfig.WIDTH*AvatarConfig.HEIGHT) {
                return AvatarPresets.defaultPreset();
            }
            // Safety: clamp palette indices out of range to transparent
            for (int i=0;i<cfg.pixels.length;i++){
                int v = cfg.pixels[i];
                if (v < -1 || v >= cfg.palette.length) cfg.pixels[i] = -1;
            }
            return cfg;
        } catch (Exception e) {
            return AvatarPresets.defaultPreset();
        }
    }

    public static void save(Context ctx, AvatarConfig cfg) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY, gson.toJson(cfg)).apply();
    }

    public static SharedPreferences prefs(Context ctx){
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
