package com.devscion.lingualink

expect object BuildEnv {
    val llmBaseUrl: String
    val llmApiKey: String
    val llmModel: String
    val deepgramApiKey: String
}
