package com.devscion.lingualink.di

import org.koin.core.module.Module

// DECISION: Web platform modules are stubs — desktop JVM is the primary target.
// Add web-compatible implementations here when targeting browser.
actual fun platformModules(): List<Module> = emptyList()
