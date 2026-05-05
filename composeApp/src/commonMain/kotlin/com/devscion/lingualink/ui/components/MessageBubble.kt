package com.devscion.lingualink.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devscion.lingualink.data.model.ConversationMessage
import com.devscion.lingualink.data.model.Speaker
import com.devscion.lingualink.ui.theme.LinguaLinkColors
import com.devscion.lingualink.ui.theme.TranscriptFontFamily

@Composable
fun MessageBubble(message: ConversationMessage) {
    val isUserA = message.speaker == Speaker.USER_A
    val alignment = if (isUserA) Alignment.Start else Alignment.End
    val bubbleColor = if (isUserA) LinguaLinkColors.UserA.copy(alpha = 0.15f)
                      else LinguaLinkColors.UserB.copy(alpha = 0.15f)
    val accentColor = if (isUserA) LinguaLinkColors.UserA else LinguaLinkColors.UserB
    val label = if (isUserA) "USER A" else "USER B"

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
           horizontalAlignment = alignment) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 480.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.originalText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = LinguaLinkColors.TextPrimary,
                    fontFamily = TranscriptFontFamily
                )
                Spacer(Modifier.height(4.dp))
                HorizontalDivider(color = accentColor.copy(alpha = 0.3f), thickness = 0.5.dp)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message.translatedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor,
                    fontFamily = TranscriptFontFamily
                )
                message.confidence?.let { conf ->
                    Text(
                        text = "${(conf * 100).toInt()}% confidence",
                        style = MaterialTheme.typography.labelSmall,
                        color = LinguaLinkColors.TextSecondary
                    )
                }
            }
        }
    }
}
