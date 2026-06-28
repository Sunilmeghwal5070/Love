package com.example.data

import kotlinx.coroutines.flow.Flow

class ChatRepository(private val chatDao: ChatDao) {
    val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()

    suspend fun insertMessage(sender: String, message: String) {
        chatDao.insertMessage(ChatMessage(sender = sender, message = message))
    }
}
