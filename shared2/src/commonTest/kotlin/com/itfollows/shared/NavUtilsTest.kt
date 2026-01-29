package com.itfollows.shared

import kotlin.test.Test
import kotlin.test.assertTrue

class NavUtilsTest {

    @Test
    fun moveToward_returnsTargetWhenStepExceedsDistance() {
        val out = NavUtils.moveToward(0.0, 0.0, 0.0, 0.001, 100000.0)
        assertTrue(kotlin.math.abs(out[0] - 0.0) < 1e-9)
        assertTrue(kotlin.math.abs(out[1] - 0.001) < 1e-9)
    }

    @Test
    fun moveToward_movesCloser() {
        val startLat = 38.0
        val startLng = -77.0
        val targetLat = 38.001
        val targetLng = -77.0

        val before = GeoUtils.distanceMeters(startLat, startLng, targetLat, targetLng)
        val out = NavUtils.moveToward(startLat, startLng, targetLat, targetLng, 10.0)
        val after = GeoUtils.distanceMeters(out[0], out[1], targetLat, targetLng)

        assertTrue(after < before, "Expected to be closer. before=$before after=$after")
    }
}
