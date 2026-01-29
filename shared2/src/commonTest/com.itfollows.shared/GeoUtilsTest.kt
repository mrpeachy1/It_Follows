package com.itfollows.shared

import kotlin.test.Test
import kotlin.test.assertTrue

class GeoUtilsTest {
    @Test
    fun distanceMeters_isZeroForSamePoint() {
        val d = GeoUtils.distanceMeters(38.0, -77.0, 38.0, -77.0)
        assertTrue(d < 0.001, "Expected near-zero, got $d")
    }

    @Test
    fun distanceMeters_isReasonableForKnownDelta() {
        // ~111m per 0.001 degrees latitude
        val d = GeoUtils.distanceMeters(0.0, 0.0, 0.001, 0.0)
        assertTrue(d > 80 && d < 140, "Expected around 111m, got $d")
    }
}
