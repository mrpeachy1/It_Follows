package com.itfollows.shared

import kotlin.jvm.JvmStatic
import kotlin.math.*

object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6371000.0

    private fun toRadians(deg: Double): Double = deg * (PI / 180.0)

    @JvmStatic
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = toRadians(lat2 - lat1)
        val dLon = toRadians(lon2 - lon1)

        val rLat1 = toRadians(lat1)
        val rLat2 = toRadians(lat2)

        val sinDLat = sin(dLat / 2.0)
        val sinDLon = sin(dLon / 2.0)

        val a =
            (sinDLat * sinDLat) +
                    cos(rLat1) * cos(rLat2) * (sinDLon * sinDLon)

        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
        return EARTH_RADIUS_METERS * c
    }
}
