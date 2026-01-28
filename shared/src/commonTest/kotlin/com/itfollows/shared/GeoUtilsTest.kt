package com.itfollows.shared

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

class GeoUtilsTest {
    @Test
    fun distanceMeters_samePointIsZero() {
        val distance = distanceMeters(37.7749, -122.4194, 37.7749, -122.4194)
        assertTrue(distance < 0.01, "Expected near-zero distance but was $distance")
    }

    @Test
    fun bearingDegrees_basicDirections() {
        val northBearing = bearingDegrees(0.0, 0.0, 1.0, 0.0)
        val eastBearing = bearingDegrees(0.0, 0.0, 0.0, 1.0)

        assertTrue(abs(northBearing - 0.0) < 5.0, "Expected ~0° but was $northBearing")
        assertTrue(abs(eastBearing - 90.0) < 5.0, "Expected ~90° but was $eastBearing")
    }

    @Test
    fun movePoint_movesRoughlyRequestedDistance() {
        val startLat = 37.7749
        val startLon = -122.4194
        val distance = 100.0

        val (newLat, newLon) = movePoint(startLat, startLon, 0.0, distance)
        val actualDistance = distanceMeters(startLat, startLon, newLat, newLon)

        assertTrue(abs(actualDistance - distance) < 2.0, "Expected ~$distance meters but was $actualDistance")
    }
}
