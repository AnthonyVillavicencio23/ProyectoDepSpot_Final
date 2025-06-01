package com.example.proyectodepspot.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface OpenAIService {
    @POST("chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String = "Bearer ${OpenAIConfig.API_KEY}",
        @Body request: ChatRequest
    ): ChatResponse
} 