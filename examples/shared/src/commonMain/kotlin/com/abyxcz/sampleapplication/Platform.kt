package com.abyxcz.sampleapplication

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform