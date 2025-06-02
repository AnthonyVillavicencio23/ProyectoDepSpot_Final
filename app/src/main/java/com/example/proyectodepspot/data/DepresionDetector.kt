package com.example.proyectodepspot.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response

class DepresionDetector {
    private val TAG = "DepresionDetector"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    // Palabras clave que podrían indicar depresión
    private val palabrasDepresivas = setOf(
        "triste", "tristeza", "deprimido", "depresión", "suicidio", "morir",
        "muerte", "sin esperanza", "sin sentido", "vacío", "solo", "solitario",
        "desesperado", "desesperación", "no puedo más", "quiero desaparecer",
        "no quiero vivir", "me quiero morir", "no tengo ganas", "no tengo fuerzas"
    )

    // Interfaz para la API de Brevo
    interface BrevoService {
        @POST("v3/smtp/email")
        suspend fun sendEmail(
            @Header("api-key") apiKey: String,
            @Body emailRequest: EmailRequest
        ): Response<Unit>
    }

    // Clase para la solicitud de correo
    data class EmailRequest(
        val sender: Sender,
        val to: List<To>,
        val subject: String,
        val htmlContent: String
    )

    data class Sender(
        val name: String,
        val email: String
    )

    data class To(
        val email: String,
        val name: String
    )

    private val brevoService: BrevoService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.brevo.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BrevoService::class.java)
    }

    suspend fun analizarMensaje(userId: String, mensaje: String) {
        val mensajeLower = mensaje.lowercase()
        
        Log.d(TAG, "Analizando mensaje: $mensajeLower")
        
        // Verificar si el mensaje contiene palabras depresivas
        val palabrasEncontradas = palabrasDepresivas.filter { palabra ->
            mensajeLower.contains(palabra)
        }

        if (palabrasEncontradas.isNotEmpty()) {
            Log.d(TAG, "Se detectaron signos depresivos en el mensaje. Palabras encontradas: $palabrasEncontradas")
            enviarAlertaContactos(userId)
        } else {
            Log.d(TAG, "No se detectaron signos depresivos en el mensaje")
        }
    }

    private suspend fun enviarAlertaContactos(userId: String) {
        try {
            Log.d(TAG, "Buscando contactos de apoyo para el usuario: $userId")
            
            // Obtener el email del usuario actual
            val userEmail = auth.currentUser?.email
            if (userEmail == null) {
                Log.e(TAG, "No se pudo obtener el email del usuario")
                return
            }
            
            Log.d(TAG, "Email del usuario: $userEmail")

            // Obtener los contactos de apoyo del usuario
            val contactos = db.collection("contactos_apoyo")
                .whereEqualTo("userEmail", userEmail)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    doc.toObject(ContactoApoyo::class.java)
                }

            Log.d(TAG, "Contactos encontrados: ${contactos.size}")
            Log.d(TAG, "Detalles de contactos: ${contactos.map { "${it.nombre} (${it.correo})" }}")

            if (contactos.isNotEmpty()) {
                // Preparar el correo
                val emailRequest = EmailRequest(
                    sender = Sender(
                        name = "Deppy - Sistema de Alerta",
                        email = "megamrd102@gmail.com"
                    ),
                    to = contactos.map { contacto ->
                        To(
                            email = contacto.correo,
                            name = contacto.nombre
                        )
                    },
                    subject = "Alerta: Usuario con síntomas depresivos",
                    htmlContent = """
                        <h2>Alerta de Deppy</h2>
                        <p>Hemos detectado que el usuario ha mostrado signos de depresión en su conversación reciente.</p>
                        <p>Por favor, contacta con el usuario lo antes posible para brindarle apoyo.</p>
                        <p>Este es un mensaje automático del sistema de alerta de Deppy.</p>
                    """.trimIndent()
                )

                Log.d(TAG, "Preparando envío de correo a: ${contactos.map { it.correo }}")

                // Enviar el correo usando Brevo
                val response = brevoService.sendEmail(
                    apiKey = "x",
                    emailRequest = emailRequest
                )

                if (response.isSuccessful) {
                    Log.d(TAG, "Alerta enviada exitosamente a los contactos de apoyo")
                } else {
                    Log.e(TAG, "Error al enviar correo. Código: ${response.code()}, Mensaje: ${response.message()}")
                }
            } else {
                Log.d(TAG, "No se encontraron contactos de apoyo para enviar la alerta")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al enviar alerta a contactos", e)
            e.printStackTrace()
        }
    }
} 