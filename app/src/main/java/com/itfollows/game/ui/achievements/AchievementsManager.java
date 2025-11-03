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

        // Core survival
        list.add(new Achievement("first_escape",       "First Escape",        "Survive your first chase.", 0));
        list.add(new Achievement("ten_min_survivor",   "Stayin’ Alive",       "Survive 10 minutes in one run.", 600));      // seconds
        list.add(new Achievement("thirty_min_survivor","Marathon Mindset",    "Survive 30 minutes in one run.", 1800));     // seconds

        // Distance (meters)
        list.add(new Achievement("endurance_master",   "Endurance Master",    "Survive 10 km total.", 10000));
        list.add(new Achievement("road_runner",        "Road Runner",         "Survive 25 km total.", 25000));
        list.add(new Achievement("it_follows_vet",     "It Follows (Vet)",    "Survive 50 km total.", 50000));

        // Night Mode / time-based
        list.add(new Achievement("night_crawler",      "Night Crawler",       "Survive after 8PM.", 0));
        list.add(new Achievement("midnight_oil",       "Midnight Oil",        "Survive a full 20 minutes past midnight.", 1200)); // seconds after 00:00

        // Power-ups usage
        list.add(new Achievement("salt_specialist",    "Salt Specialist",     "Use 10 Salt Bombs.", 10));
        list.add(new Achievement("shielded",           "Shell Shielded",      "Block 5 snail encounters with Shell Shield.", 5));
        list.add(new Achievement("decoy_artist",       "Decoy Artist",        "Deploy 5 Decoy Shells.", 5));
        list.add(new Achievement("flash_freeze",       "Flash Freeze",        "Stun the snail 10 times.", 10));

        // Repel feature
        list.add(new Achievement("first_repel",        "Back You Go",         "Use Snail Repel once.", 1));
        list.add(new Achievement("repel_master",       "Repel Master",        "Repel the snail 10 times.", 10));

        // Beacon system
        list.add(new Achievement("beacon_savior",      "Beacon Savior",       "Reach 3 Snail Beacons before the snail.", 3));
        list.add(new Achievement("beacon_race",        "Beacon Racer",        "Win 5 beacon races.", 5));

        // Economy / shop
        list.add(new Achievement("first_purchase",     "Window Shopper",      "Buy your first power-up.", 1));
        list.add(new Achievement("big_spender",        "Big Spender",         "Spend 100K Snail Coins total.", 100000));
        list.add(new Achievement("thrifty_survivor",   "Thrifty Survivor",    "Survive 10 minutes with no power-ups used.", 600)); // seconds in a no-usage run

        // Streaks / days
        list.add(new Achievement("daily_return",       "Back Again",          "Play 2 days in a row.", 2));
        list.add(new Achievement("weekly_return",      "Can’t Stop Won’t Stop","Play 7 days in a row.", 7));

        // Minigame #2: Don’t Look Away
        list.add(new Achievement("dont_look_clear1",   "Don’t Blink",         "Beat 'Don’t Look Away' once.", 1));
        list.add(new Achievement("dont_look_clear5",   "Eyes of Steel",       "Beat 'Don’t Look Away' five times.", 5));

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
