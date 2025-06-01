package com.example.proyectodepspot

import android.content.Context
import com.example.proyectodepspot.api.ChatMessage
import com.example.proyectodepspot.api.OpenAIConfig
import com.example.proyectodepspot.data.Message
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object ChatStorage {
    private const val PREF_NAME = "chat_storage"
    private val db = FirebaseFirestore.getInstance()
    private val usuariosCollection = db.collection("usuarios")

    fun saveMessages(context: Context, messages: List<Message>) {
        // No guardamos nada
    }

    fun loadMessages(context: Context): List<Message> {
        // Siempre retornamos una lista vacía
        return emptyList()
    }

    fun saveConversationHistory(context: Context, conversation: List<ChatMessage>) {
        // No guardamos nada
    }

    fun loadConversationHistory(context: Context): List<ChatMessage> {
        // Solo retornamos el prompt inicial del sistema
        return listOf(ChatMessage("system", OpenAIConfig.SYSTEM_PROMPT))
    }

    fun clearAll(context: Context) {
        // No es necesario limpiar nada ya que no guardamos datos
    }

    suspend fun saveMessage(userId: String, message: Message) {
        usuariosCollection.document(userId)
            .collection("messages")
            .add(message)
            .await()
    }

    suspend fun getMessages(userId: String): List<Message> {
        return usuariosCollection.document(userId)
            .collection("messages")
            .get()
            .await()
            .toObjects(Message::class.java)
    }
} 