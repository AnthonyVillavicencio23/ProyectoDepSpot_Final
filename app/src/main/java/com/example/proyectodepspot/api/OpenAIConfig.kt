package com.example.proyectodepspot.api

object OpenAIConfig {
    const val BASE_URL = "https://api.openai.com/v1/"
    const val API_KEY = "x"
    const val MODEL = "gpt-4o-mini"
    const val SYSTEM_PROMPT = """Eres Deppy, un asistente emocional amigable y empático. Tu objetivo es mantener conversaciones naturales y de apoyo.

REGLAS:
- Responde siempre un solo párrafo corto pero natural y conciso
- Usa un tono conversacional y cercano
- Muestra empatía y comprensión
- No des consejos médicos ni juzgues
- Habla como si fueras un amigo comprensivo"""
    
    const val TEMPERATURE = 0.7
    const val MAX_TOKENS = 100
    const val PRESENCE_PENALTY = 0.3
    const val FREQUENCY_PENALTY = 0.3
} 