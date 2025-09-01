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
        if (json == null) return new AvatarConfig();
        try { return gson.fromJson(json, AvatarConfig.class); }
        catch (Exception e) { return new AvatarConfig(); }
    }

    public static void save(Context ctx, AvatarConfig cfg) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        sp.edit().putString(KEY, gson.toJson(cfg)).apply();
    }

    public static SharedPreferences prefs(Context ctx){
        return ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
