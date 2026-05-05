package com.devscion.lingualink

import androidx.compose.runtime.Composable
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
import com.devscion.lingualink.ui.screens.*
import com.devscion.lingualink.ui.theme.LinguaLinkTheme
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App() {
    LinguaLinkTheme {
        val navController = rememberNavController()
        val configManager: ConfigManager = koinInject()
        val startDest = if (configManager.isConfigured()) Screen.Home.route else Screen.Setup.route

        NavHost(navController = navController, startDestination = startDest) {

            composable(Screen.Setup.route) {
                val vm: SetupViewModel = koinViewModel()
                SetupScreen(vm = vm, onSaved = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Setup.route) { inclusive = true }
                    }
                })
            }

            composable(Screen.Home.route) {
                val vm: HomeViewModel = koinViewModel()
                HomeScreen(
                    vm = vm,
                    onStartCall  = { id, src, tgt -> navController.navigate(CallRoute(id, src, tgt)) },
                    onOpenChat   = { id, src, tgt -> navController.navigate(ChatRoute(id, src, tgt)) },
                    onHistory    = { navController.navigate(Screen.History.route) },
                    onSettings   = { navController.navigate(Screen.Setup.route) }
                )
            }

            composable<CallRoute> { back ->
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

            composable<ChatRoute> { back ->
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

            composable(Screen.History.route) {
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
