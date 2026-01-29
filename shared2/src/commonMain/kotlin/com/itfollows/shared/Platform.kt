package com.itfollows.shared

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
