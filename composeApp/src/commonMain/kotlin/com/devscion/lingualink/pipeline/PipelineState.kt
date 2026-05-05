package com.devscion.lingualink.pipeline

sealed class PipelineState {
    object Idle : PipelineState()
    object Listening : PipelineState()
    data class Transcribing(val partial: String) : PipelineState()
    data class Translating(val original: String) : PipelineState()
    data class Speaking(val translated: String) : PipelineState()
    data class Error(val message: String) : PipelineState()
}
