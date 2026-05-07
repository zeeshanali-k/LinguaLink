package com.devscion.lingualink

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.devscion.lingualink.data.config.ConfigManager
import com.devscion.lingualink.data.model.SessionType
import com.devscion.lingualink.data.repository.MessageRepository
import com.devscion.lingualink.data.repository.SessionRepository
import com.devscion.lingualink.navigation.CallRoute
import com.devscion.lingualink.navigation.ChatRoute
import com.devscion.lingualink.navigation.Screen
import com.devscion.lingualink.network.LlmClient
import com.devscion.lingualink.network.TtsClient
import com.devscion.lingualink.ui.screens.*
import com.devscion.lingualink.ui.theme.LinguaLinkTheme
import com.devscion.lingualink.ui.theme.LocalAnimatedVisibilityScope
import com.devscion.lingualink.ui.theme.LocalSharedTransitionScope
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun App() {
    LinguaLinkTheme {
        val navController = rememberNavController()
        val configManager: ConfigManager = koinInject()
        val llmClient: LlmClient = koinInject()
        val ttsClient: TtsClient = koinInject()

        LaunchedEffect(Unit) {
            configManager.load()?.let { cfg ->
                if (cfg.llmApiKey.isNotBlank()) {
                    llmClient.configure(cfg.llmBaseUrl, cfg.llmApiKey, cfg.llmModel)
                }
                ttsClient.configure(cfg.deepgramApiKey)
            }
        }

        val startDest = if (configManager.isConfigured()) Screen.Home.route else Screen.Setup.route

        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavHost(
                    navController = navController,
                    startDestination = startDest,
                    enterTransition = { EnterTransition.None },
                    exitTransition = { ExitTransition.None },
                    popEnterTransition = { EnterTransition.None },
                    popExitTransition = { ExitTransition.None },
                ) {

                    composable(Screen.Setup.route) {
                        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                            val vm: SetupViewModel = koinViewModel()
                            val canGoBack = navController.previousBackStackEntry != null
                            SetupScreen(
                                vm = vm,
                                onSaved = {
                                    if (canGoBack) {
                                        navController.popBackStack()
                                    } else {
                                        navController.navigate(Screen.Home.route) {
                                            popUpTo(Screen.Setup.route) { inclusive = true }
                                        }
                                    }
                                },
                                onBack = if (canGoBack) ({ navController.popBackStack() }) else null,
                            )
                        }
                    }

                    composable(Screen.Home.route) {
                        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                            val vm: HomeViewModel = koinViewModel()
                            HomeScreen(
                                vm = vm,
                                onStartCall  = { id, src, tgt -> navController.navigate(CallRoute(id, src, tgt)) },
                                onOpenChat   = { id, src, tgt -> navController.navigate(ChatRoute(id, src, tgt)) },
                                onHistory    = { navController.navigate(Screen.History.route) },
                                onSettings   = { navController.navigate(Screen.Settings.route) }
                            )
                        }
                    }

                    composable(Screen.Settings.route) {
                        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                            val configMgr: ConfigManager = koinInject()
                            SettingsScreen(
                                configManager = configMgr,
                                onBack = { navController.popBackStack() },
                                onEditApiConfig = { navController.navigate(Screen.Setup.route) },
                            )
                        }
                    }

                    composable<CallRoute> { back ->
                        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                            val route = back.toRoute<CallRoute>()
                            val vm: CallViewModel = koinViewModel()
                            val configMgr: ConfigManager = koinInject()
                            CallScreen(
                                sessionId  = route.sessionId,
                                sourceLang = route.sourceLang,
                                targetLang = route.targetLang,
                                vm = vm,
                                onBack = { navController.popBackStack() },
                                configManager = configMgr
                            )
                        }
                    }

                    composable<ChatRoute> { back ->
                        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                            val route = back.toRoute<ChatRoute>()
                            val vm: ChatViewModel = koinViewModel()
                            val configMgr: ConfigManager = koinInject()
                            ChatScreen(
                                sessionId  = route.sessionId,
                                sourceLang = route.sourceLang,
                                targetLang = route.targetLang,
                                vm = vm,
                                onBack = { navController.popBackStack() },
                                configManager = configMgr
                            )
                        }
                    }

                    composable(Screen.History.route) {
                        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this@composable) {
                            val sessionRepo: SessionRepository = koinInject()
                            val messageRepo: MessageRepository = koinInject()
                            HistoryScreen(
                                sessionRepo = sessionRepo,
                                messageRepo = messageRepo,
                                onReopen = { id, type, src, tgt ->
                                    if (type == SessionType.CALL) navController.navigate(CallRoute(id, src, tgt))
                                    else navController.navigate(ChatRoute(id, src, tgt))
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}
