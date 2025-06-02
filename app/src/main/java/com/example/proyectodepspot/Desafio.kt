package com.example.proyectodepspot

data class Desafio(
    val fraseMotivadora: String,
    val desafio: String
)

object DesafiosPredeterminados {
    val lista = listOf(
        Desafio(
            "Cada día es una nueva oportunidad para crecer",
            "Lee cinco páginas del libro que te gusta y subraya un párrafo que te impacte"
        ),
        Desafio(
            "Pequeños pasos llevan a grandes cambios",
            "Dedica 10 minutos a meditar y enfócate en tu respiración"
        ),
        Desafio(
            "Tu bienestar es una prioridad",
            "Escribe tres cosas por las que estés agradecido hoy"
        ),
        Desafio(
            "La constancia es la clave del éxito",
            "Realiza 15 minutos de ejercicio físico que disfrutes"
        ),
        Desafio(
            "Cada momento es una oportunidad para ser mejor",
            "Practica un acto de bondad con alguien cercano"
        ),
        Desafio(
            "El autocuidado es fundamental",
            "Toma un momento para disfrutar de tu bebida favorita con calma"
        )
    )
} 