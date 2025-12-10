package com.abyxcz.cbindingkmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform