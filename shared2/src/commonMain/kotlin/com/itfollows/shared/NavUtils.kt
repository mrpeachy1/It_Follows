package com.itfollows.shared

import kotlin.jvm.JvmStatic
import kotlin.math.*

object NavUtils {
    private const val R = 6371000.0

    private fun toRadians(deg: Double) = deg * (PI / 180.0)
    private fun toDegrees(rad: Double) = rad * (180.0 / PI)

    @JvmStatic
    fun moveToward(lat1: Double, lng1: Double, lat2: Double, lng2: Double, d: Double): DoubleArray {
        val dist = GeoUtils.distanceMeters(lat1, lng1, lat2, lng2)
        if (dist <= 0.001 || d >= dist) return doubleArrayOf(lat2, lng2)

        val frac = d / dist

        val phi1 = toRadians(lat1)
        val lambda1 = toRadians(lng1)
        val phi2 = toRadians(lat2)
        val lambda2 = toRadians(lng2)

        val sinDist = sin(dist / R)
        val a = sin((1 - frac) * dist / R) / sinDist
        val b = sin(frac * dist / R) / sinDist

        val x = a * cos(phi1) * cos(lambda1) + b * cos(phi2) * cos(lambda2)
        val y = a * cos(phi1) * sin(lambda1) + b * cos(phi2) * sin(lambda2)
        val z = a * sin(phi1) + b * sin(phi2)

        val phi = atan2(z, sqrt(x * x + y * y))
        val lambda = atan2(y, x)

        return doubleArrayOf(toDegrees(phi), toDegrees(lambda))
    }
}
