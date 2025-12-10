package com.abyxcz.sampleapplication

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "SampleApplication",
    ) {
        App()
    }
}