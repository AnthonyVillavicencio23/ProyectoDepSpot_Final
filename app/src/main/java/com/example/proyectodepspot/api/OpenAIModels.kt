package com.example.proyectodepspot.api

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val store: Boolean = false
)

data class Message(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

// prueba