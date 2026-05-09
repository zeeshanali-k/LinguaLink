import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

val buildEnvDir = layout.buildDirectory.dir("generated/buildenv/kotlin")

val generateBuildEnv = tasks.register("generateBuildEnv") {
    val llmBaseUrl = providers.environmentVariable("LINGUALINK_LLM_BASE_URL").orElse("")
    val llmApiKey = providers.environmentVariable("LINGUALINK_LLM_API_KEY").orElse("")
    val llmModel = providers.environmentVariable("LINGUALINK_LLM_MODEL").orElse("")
    val deepgramApiKey = providers.environmentVariable("LINGUALINK_DEEPGRAM_API_KEY").orElse("")
    val outDir = buildEnvDir

    inputs.property("llmBaseUrl", llmBaseUrl)
    inputs.property("llmApiKey", llmApiKey)
    inputs.property("llmModel", llmModel)
    inputs.property("deepgramApiKey", deepgramApiKey)
    outputs.dir(outDir)

    doLast {
        val dir = outDir.get().asFile
        val file = dir.resolve("com/devscion/lingualink/BuildEnv.kt")
        file.parentFile.mkdirs()
        file.writeText(buildString {
            appendLine("package com.devscion.lingualink")
            appendLine()
            appendLine("actual object BuildEnv {")
            appendLine("    actual val llmBaseUrl: String = \"${llmBaseUrl.get()}\"")
            appendLine("    actual val llmApiKey: String = \"${llmApiKey.get()}\"")
            appendLine("    actual val llmModel: String = \"${llmModel.get()}\"")
            appendLine("    actual val deepgramApiKey: String = \"${deepgramApiKey.get()}\"")
            append("}")
        })
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateBuildEnv)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain {
            kotlin.srcDir(buildEnvDir)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(compose.materialIconsExtended)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)

            implementation(libs.kotlinx.coroutines.core)

            // Koin
            implementation(libs.koin.core)
            implementation(libs.koin.cmp.compose)
            implementation(libs.koin.compose.viewmodel)
            implementation(libs.koin.compose.viewmodel.navigation)

            // Navigation
            implementation(libs.navigation.compose)

            // Serialization
            implementation(libs.kotlinx.serialization.json)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutinesSwing)

            // Ktor Client
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.client.websockets)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinx.json)

            // SQLDelight
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.sqldelight.coroutines)

            implementation("org.slf4j:slf4j-simple:2.0.9")
        }
    }
}

sqldelight {
    databases {
        create("LinguaLinkDB") {
            packageName.set("com.devscion.lingualink.db")
            srcDirs.setFrom("src/jvmMain/sqldelight")
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.devscion.lingualink.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.devscion.lingualink"
            packageVersion = "1.0.0"
            includeAllModules = true
        }
    }
}
