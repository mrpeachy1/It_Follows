package com.itfollows.shared

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.toDegrees
import kotlin.math.toRadians

object GeoUtils {
    @JvmStatic
    fun bearingDegrees(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Double {
        val lat1 = toRadians(fromLat)
        val lon1 = toRadians(fromLon)
        val lat2 = toRadians(toLat)
        val lon2 = toRadians(toLon)
        val dLon = lon2 - lon1

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) -
            sin(lat1) * cos(lat2) * cos(dLon)

        return (toDegrees(atan2(y, x)) + 360) % 360
    }
}
