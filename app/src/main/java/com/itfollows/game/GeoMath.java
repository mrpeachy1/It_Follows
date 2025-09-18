package com.itfollows.game;

public class GeoMath {
    private static final double R = 6371000.0;

    public static double haversineMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public static double[] moveToward(double lat1, double lng1, double lat2, double lng2, double d) {
        double dist = haversineMeters(lat1, lng1, lat2, lng2);
        if (dist <= 0.001 || d >= dist) return new double[]{lat2, lng2};
        double frac = d / dist;

        double phi1 = Math.toRadians(lat1), lambda1 = Math.toRadians(lng1);
        double phi2 = Math.toRadians(lat2), lambda2 = Math.toRadians(lng2);

        double sinDist = Math.sin(dist / R);
        double A = Math.sin((1 - frac) * dist / R) / sinDist;
        double B = Math.sin(frac * dist / R) / sinDist;

        double x = A * Math.cos(phi1) * Math.cos(lambda1) + B * Math.cos(phi2) * Math.cos(lambda2);
        double y = A * Math.cos(phi1) * Math.sin(lambda1) + B * Math.cos(phi2) * Math.sin(lambda2);
        double z = A * Math.sin(phi1) + B * Math.sin(phi2);

        double phi = Math.atan2(z, Math.sqrt(x * x + y * y));
        double lambda = Math.atan2(y, x);

        return new double[]{Math.toDegrees(phi), Math.toDegrees(lambda)};
    }
}
