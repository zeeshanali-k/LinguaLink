package com.devscion.lingualink.navigation

import kotlinx.serialization.Serializable

sealed class Screen(val route: String) {
    object Setup : Screen("setup")
    object Home : Screen("home")
    object History : Screen("history")
    object Settings : Screen("settings")
}

@Serializable
data class CallRoute(val sessionId: Long, val sourceLang: String, val targetLang: String)

@Serializable
data class ChatRoute(val sessionId: Long, val sourceLang: String, val targetLang: String)
