package com.itfollows.shared

import kotlin.test.Test
import kotlin.test.assertTrue

class DistancePresenterTest {
    @Test
    fun snailDistanceLabel_containsPrefix() {
        val s = DistancePresenter.snailDistanceLabel(0.0, 0.0, 0.0, 0.0)
        assertTrue(s.startsWith("Snail:"), "Got: $s")
    }
}
