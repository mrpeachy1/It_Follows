package com.itfollows.shared

import kotlin.jvm.JvmStatic
import kotlin.math.round

object DistancePresenter {
    @JvmStatic
    fun snailDistanceLabel(playerLat: Double, playerLon: Double, snailLat: Double, snailLon: Double): String {
        val meters = GeoUtils.distanceMeters(playerLat, playerLon, snailLat, snailLon)
        val rounded1 = round(meters * 10.0) / 10.0
        return "Snail: $rounded1 m"
    }
}
