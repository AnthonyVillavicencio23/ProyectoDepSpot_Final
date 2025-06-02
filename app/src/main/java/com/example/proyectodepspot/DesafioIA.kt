package com.example.proyectodepspot

data class DesafioIA(
    val fraseMotivadora: String,
    val desafio: String,
    val fecha: String,
    val completado: Boolean = false
) {
    companion object {
        fun fromMap(map: Map<String, Any>): DesafioIA {
            return DesafioIA(
                fraseMotivadora = map["frase_motivadora"] as String,
                desafio = map["desafio"] as String,
                fecha = map["fecha"] as String,
                completado = map["completado"] as? Boolean ?: false
            )
        }

        fun toMap(desafio: DesafioIA): Map<String, Any> {
            return mapOf(
                "frase_motivadora" to desafio.fraseMotivadora,
                "desafio" to desafio.desafio,
                "fecha" to desafio.fecha,
                "completado" to desafio.completado
            )
        }
    }
} 