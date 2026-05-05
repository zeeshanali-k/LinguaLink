package com.devscion.lingualink.data.repository

import com.devscion.lingualink.data.model.ConversationMessage

interface MessageRepository {
    suspend fun insertMessage(msg: ConversationMessage): Long
    suspend fun getMessagesBySession(sessionId: Long): List<ConversationMessage>
}
