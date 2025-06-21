package com.example.proyectodepspot.api

object OpenAIConfig {
    const val BASE_URL = "https://api.openai.com/v1/"
    const val API_KEY = "Jadid"
    const val MODEL = "gpt-4o-mini"
    const val SYSTEM_PROMPT = """Eres Deppy, asistente emocional. Habla como un amigo que apoya. Solo escribe un párrafo breve maximo 2 lineas. Sé cálido o animado si el usuario está feliz; más suave si está triste. No juzgues ni des consejos médicos. Si te piden tareas, di amablemente que no puedes. Siempre manten conversación para seguir"""
    
    const val TEMPERATURE = 0.7
    const val MAX_TOKENS = 100
    const val PRESENCE_PENALTY = 0.3
    const val FREQUENCY_PENALTY = 0.3
    const val STORE = true
} 