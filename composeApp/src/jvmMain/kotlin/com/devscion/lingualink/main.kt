package com.devscion.lingualink

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.devscion.lingualink.di.commonModule
import com.devscion.lingualink.di.jvmPlatformModule
import org.koin.core.context.startKoin

fun main() = application {
    startKoin {
        modules(commonModule, jvmPlatformModule)
    }

    val windowState = rememberWindowState(width = 1280.dp, height = 800.dp)

    Window(
        onCloseRequest = ::exitApplication,
        title = "LinguaLink — Real-Time Translation",
        state = windowState
    ) {
        App()
    }
}
