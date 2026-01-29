package com.itfollows.game;

public class GeoMath {
    private static final double R = 6371000.0;

    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        return com.itfollows.shared.GeoUtils.distanceMeters(lat1, lng1, lat2, lng2);
    }


    public static double[] moveToward(double lat1, double lng1, double lat2, double lng2, double d) {
        return com.itfollows.shared.NavUtils.INSTANCE.moveToward(lat1, lng1, lat2, lng2, d);


    }
}
