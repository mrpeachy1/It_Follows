package com.itfollows.shared

import kotlin.jvm.JvmStatic

object KmmBridge {
    @JvmStatic
    fun platformName(): String = getPlatform().name
}
