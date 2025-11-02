package com.itfollows.game.ui.achievements;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class AchievementsManager {

    public static final String PREFS = "Achievements";

    public static class Achievement {
        public final String key;
        public final String title;
        public final String desc;
        public final int target; // 0 = binary unlock, >0 = progress-based
        public Achievement(String key, String title, String desc, int target) {
            this.key = key; this.title = title; this.desc = desc; this.target = target;
        }
    }

    public static List<Achievement> catalog() {
        List<Achievement> list = new ArrayList<>();
        list.add(new Achievement("first_escape",    "First Escape",     "Survive your first chase.", 0));
        list.add(new Achievement("night_crawler",   "Night Crawler",    "Survive after 8PM.", 0));
        list.add(new Achievement("salt_specialist", "Salt Specialist",  "Use 10 Salt Bombs.", 10));
        list.add(new Achievement("endurance_master","Endurance Master", "Survive 10km total.", 10000)); // meters
        return list;
    }

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // Binary unlock
    public static void unlock(Context c, String key) {
        prefs(c).edit().putBoolean(key, true).apply();
    }

    public static boolean isUnlocked(Context c, String key) {
        return prefs(c).getBoolean(key, false);
    }

    // Progress-based
    public static void addProgress(Context c, String key, int delta) {
        int p = getProgress(c, key) + delta;
        prefs(c).edit().putInt(key + "_progress", p).apply();
    }

    public static int getProgress(Context c, String key) {
        return prefs(c).getInt(key + "_progress", 0);
    }

    public static int getUnlockedCount(Context c) {
        int count = 0;
        for (Achievement a : catalog()) {
            if (a.target == 0 && isUnlocked(c, a.key)) count++;
            else if (a.target > 0 && getProgress(c, a.key) >= a.target) count++;
        }
        return count;
    }

    public static int getTotalCount() {
        return catalog().size();
    }
}
