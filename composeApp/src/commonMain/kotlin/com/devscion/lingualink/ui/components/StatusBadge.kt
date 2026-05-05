package com.devscion.lingualink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devscion.lingualink.pipeline.PipelineState
import com.devscion.lingualink.ui.theme.LinguaLinkColors

@Composable
fun StatusBadge(state: PipelineState) {
    val (text, color) = when (state) {
        is PipelineState.Idle        -> "Ready" to LinguaLinkColors.TextSecondary
        is PipelineState.Listening   -> "Listening..." to LinguaLinkColors.Success
        is PipelineState.Transcribing -> "Transcribing..." to LinguaLinkColors.Primary
        is PipelineState.Translating -> "Translating..." to LinguaLinkColors.Accent
        is PipelineState.Speaking    -> "Speaking..." to LinguaLinkColors.Accent
        is PipelineState.Error       -> "Error" to LinguaLinkColors.Error
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(text, color = color, style = MaterialTheme.typography.labelSmall)
    }
}
