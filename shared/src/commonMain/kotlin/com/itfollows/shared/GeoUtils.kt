package com.itfollows.shared

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_378_137.0

fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val deltaLat = Math.toRadians(lat2 - lat1)
    val deltaLon = Math.toRadians(lon2 - lon1)

    val sinHalfDeltaLat = sin(deltaLat / 2)
    val sinHalfDeltaLon = sin(deltaLon / 2)

    val a = sinHalfDeltaLat * sinHalfDeltaLat +
        cos(lat1Rad) * cos(lat2Rad) * sinHalfDeltaLon * sinHalfDeltaLon
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return EARTH_RADIUS_METERS * c
}

fun bearingDegrees(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val deltaLon = Math.toRadians(lon2 - lon1)

    val y = sin(deltaLon) * cos(lat2Rad)
    val x = cos(lat1Rad) * sin(lat2Rad) -
        sin(lat1Rad) * cos(lat2Rad) * cos(deltaLon)
    val bearing = Math.toDegrees(atan2(y, x))
    return clampBearing0To360(bearing)
}

fun movePoint(
    lat: Double,
    lon: Double,
    bearingDeg: Double,
    distanceMeters: Double
): Pair<Double, Double> {
    val bearingRad = Math.toRadians(bearingDeg)
    val latRad = Math.toRadians(lat)
    val lonRad = Math.toRadians(lon)

    val angularDistance = distanceMeters / EARTH_RADIUS_METERS
    val sinLat = sin(latRad)
    val cosLat = cos(latRad)

    val sinAngular = sin(angularDistance)
    val cosAngular = cos(angularDistance)

    val newLat = asin(sinLat * cosAngular + cosLat * sinAngular * cos(bearingRad))
    val newLon = lonRad + atan2(
        sin(bearingRad) * sinAngular * cosLat,
        cosAngular - sinLat * sin(newLat)
    )

    return Math.toDegrees(newLat) to Math.toDegrees(newLon)
}

fun clampBearing0To360(deg: Double): Double {
    val normalized = deg % 360
    return if (normalized < 0) normalized + 360 else normalized
}
