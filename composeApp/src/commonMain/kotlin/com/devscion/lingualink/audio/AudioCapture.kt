package com.devscion.lingualink.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface AudioCapture {
    val audioChunks: Flow<ByteArray>
    val audioLevel: StateFlow<Float>
    val isCapturing: Boolean
    fun isMicAvailable(): Boolean
    fun startCapture(scope: CoroutineScope)
    fun stopCapture()
}
