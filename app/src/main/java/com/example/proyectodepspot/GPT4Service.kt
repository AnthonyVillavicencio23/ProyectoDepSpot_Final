package com.example.proyectodepspot

import com.example.proyectodepspot.api.ChatRequest
import com.example.proyectodepspot.api.Message
import com.example.proyectodepspot.api.OpenAIConfig
import com.example.proyectodepspot.api.OpenAIService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.delay

object GPT4Service {
    private val openAIService = Retrofit.Builder()
        .baseUrl(OpenAIConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenAIService::class.java)

    private var lastRequestTime = 0L
    private val minRequestInterval = 2000L // 2 segundos entre solicitudes

    suspend fun generateResponse(prompt: String): String {
        // Esperar si es necesario para respetar el límite de rate
        val currentTime = System.currentTimeMillis()
        val timeSinceLastRequest = currentTime - lastRequestTime
        if (timeSinceLastRequest < minRequestInterval) {
            delay(minRequestInterval - timeSinceLastRequest)
        }

        val messages = listOf(
            Message(
                role = "system",
                content = """Eres un asistente especializado en generar desafíos personalizados para mejorar el bienestar emocional.
                    IMPORTANTE: Cuando te pidan generar un desafío, responde SOLO con un objeto JSON válido.
                    No incluyas ningún otro texto, explicaciones o comentarios."""
            ),
            Message(
                role = "user",
                content = prompt
            )
        )

        val chatRequest = ChatRequest(
            model = OpenAIConfig.MODEL,
            messages = messages
        )

        return try {
            val response = openAIService.createChatCompletion(request = chatRequest)
            lastRequestTime = System.currentTimeMillis()
            val content = response.choices.firstOrNull()?.message?.content 
                ?: throw Exception("No se recibió respuesta de la API")
            
            // Limpiar la respuesta de cualquier texto que no sea JSON
            content.trim().let { text ->
                val startIndex = text.indexOf('{')
                val endIndex = text.lastIndexOf('}')
                if (startIndex >= 0 && endIndex > startIndex) {
                    text.substring(startIndex, endIndex + 1)
                } else {
                    throw Exception("La respuesta no contiene un JSON válido")
                }
            }
        } catch (e: Exception) {
            // No registramos el error aquí, dejamos que el llamador lo maneje
            throw e
        }
    }
} 