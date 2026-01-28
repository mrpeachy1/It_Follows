package com.itfollows.shared

import kotlin.math.*

object GeoUtils {
    private const val EARTH_RADIUS_METERS = 6378137.0

    private fun degToRad(deg: Double): Double = deg * PI / 180.0
    private fun radToDeg(rad: Double): Double = rad * 180.0 / PI
    fun bearingDegrees(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Double {
        val lat1 = degToRad(fromLat)
        val lat2 = degToRad(toLat)
        val dLon = degToRad(toLon - fromLon)

        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)

        val brng = radToDeg(atan2(y, x))
        var d = brng % 360.0
        if (d < 0) d += 360.0
        return d
    }
}
