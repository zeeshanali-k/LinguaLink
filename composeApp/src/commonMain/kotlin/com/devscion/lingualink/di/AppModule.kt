package com.devscion.lingualink.di

import com.devscion.lingualink.pipeline.TranslationPipeline
import com.devscion.lingualink.ui.screens.CallViewModel
import com.devscion.lingualink.ui.screens.ChatViewModel
import com.devscion.lingualink.ui.screens.HomeViewModel
import com.devscion.lingualink.ui.screens.SessionDetailsViewModel
import com.devscion.lingualink.ui.screens.SetupViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val commonModule = module {
    single { TranslationPipeline(get(), get(), get(), get()) }
    viewModel { SetupViewModel(get(), get(), get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { CallViewModel(get(), get(), get(), get()) }
    viewModel { ChatViewModel(get(), get(), get()) }
    viewModel { SessionDetailsViewModel(get(), get(), get(), get()) }
}
