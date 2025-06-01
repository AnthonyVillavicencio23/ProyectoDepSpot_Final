package com.example.proyectodepspot.utils

class DepressionDetector {
    companion object {
        private val PALABRAS_DEPRESIVAS = setOf(
            "triste", "tristeza", "deprimido", "depresión", "vacío", "sin esperanza",
            "sin valor", "culpable", "inútil", "muerte", "morir", "suicidio", "suicidarme",
            "acabar con todo", "no quiero vivir", "no tiene sentido", "no puedo más",
            "estoy solo", "nadie me entiende", "nadie me quiere", "no sirvo para nada",
            "quiero desaparecer", "quiero dormir para siempre", "no quiero despertar",
            "mejor estaría muerto", "no tengo ganas de nada", "todo me da igual",
            "no tengo esperanza", "no veo futuro", "no hay salida", "estoy atrapado",
            "no puedo salir de esto", "me siento vacío", "me siento solo",
            "nadie me ayuda", "nadie me comprende", "me siento inútil",
            "me siento culpable", "me siento cansado", "me siento agotado",
            "no tengo energía", "no tengo motivación", "no tengo ilusión",
            "no tengo ganas de vivir", "prefiero estar muerto", "quiero morir",
            "mejor estaría muerto", "no quiero seguir", "quiero terminar con todo"
        )
    }

    fun detectarSignosDepresivos(mensaje: String): Boolean {
        val mensajeLimpio = mensaje.lowercase()
        return PALABRAS_DEPRESIVAS.any { palabra ->
            mensajeLimpio.contains(palabra)
        }
    }
} 