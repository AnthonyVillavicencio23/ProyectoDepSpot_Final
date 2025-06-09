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

    // Interfaz para la API de Resend
    interface ResendService {
        @POST("emails")
        suspend fun sendEmail(
            @Header("Authorization") apiKey: String,
            @Body emailRequest: EmailRequest
        ): Response<Unit>
    }

    // Clase para la solicitud de correo
    data class EmailRequest(
        val from: String,
        val to: String,
        val subject: String,
        val html: String
    )

    private val resendService: ResendService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        Retrofit.Builder()
            .baseUrl("https://api.resend.com/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ResendService::class.java)
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
            enviarAlertaContactos(userId, palabrasEncontradas)
        } else {
            Log.d(TAG, "No se detectaron signos depresivos en el mensaje")
        }
    }

    private suspend fun enviarAlertaContactos(userId: String, palabrasEncontradas: List<String>) {
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
                // Preparar el correo para cada contacto
                for (contacto in contactos) {
                    val emailRequest = EmailRequest(
                        from = "deppy_ia@depspot.online",
                        to = contacto.correo,
                        subject = "🚨 Alerta: Usuario con síntomas depresivos",
                        html = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <style>
                                    body {
                                        font-family: Arial, sans-serif;
                                        line-height: 1.6;
                                        color: #333;
                                        max-width: 600px;
                                        margin: 0 auto;
                                        padding: 20px;
                                    }
                                    .header {
                                        text-align: center;
                                        padding: 20px;
                                        background-color: #f8f9fa;
                                        border-radius: 8px;
                                        margin-bottom: 20px;
                                    }
                                    .logo {
                                        max-width: 150px;
                                        margin-bottom: 15px;
                                    }
                                    .alert-box {
                                        background-color: #fff3cd;
                                        border: 1px solid #ffeeba;
                                        color: #856404;
                                        padding: 15px;
                                        border-radius: 8px;
                                        margin: 20px 0;
                                    }
                                    .info-box {
                                        background-color: #e9ecef;
                                        padding: 15px;
                                        border-radius: 8px;
                                        margin: 20px 0;
                                    }
                                    .footer {
                                        text-align: center;
                                        margin-top: 30px;
                                        padding-top: 20px;
                                        border-top: 1px solid #dee2e6;
                                        font-size: 0.9em;
                                        color: #6c757d;
                                    }
                                    .button {
                                        display: inline-block;
                                        padding: 10px 20px;
                                        background-color: #007bff;
                                        color: white;
                                        text-decoration: none;
                                        border-radius: 5px;
                                        margin: 20px 0;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="header">
                                    <img src="https://st4.depositphotos.com/5161043/25260/v/450/depositphotos_252604978-stock-illustration-water-wave-symbol-and-icon.jpg" alt="Deppy Logo" class="logo">
                                    <h1>Alerta de Deppy</h1>
                                </div>
                                
                                <div class="alert-box">
                                    <h2>🚨 Alerta de Detección</h2>
                                    <p>Hemos detectado que el usuario <strong>${userEmail}</strong> ha mostrado signos de depresión en su conversación reciente.</p>
                                </div>

                                <div class="info-box">
                                    <h3>Detalles de la Situación:</h3>
                                    <ul>
                                        <li>Usuario: ${userEmail}</li>
                                        <li>Palabras detectadas: ${palabrasEncontradas.joinToString(", ")}</li>
                                    </ul>
                                </div>

                                <p>Como contacto de apoyo, tu intervención es crucial en este momento. Por favor:</p>
                                <ul>
                                    <li>Contacta al usuario lo antes posible</li>
                                    <li>Mantén una conversación empática y comprensiva</li>
                                    <li>Escucha activamente sus preocupaciones</li>
                                    <li>Ofrece tu apoyo y compañía</li>
                                </ul>

                                <div style="text-align: center;">
                                    <a href="mailto:${userEmail}" class="button">Contactar al Usuario</a>
                                </div>

                                <div class="footer">
                                    <p>Este es un mensaje automático del sistema de alerta de Deppy.</p>
                                    <p>Si necesitas ayuda adicional, no dudes en contactarnos.</p>
                                    <p>© 2024 Deppy - Sistema de Apoyo Emocional</p>
                                </div>
                            </body>
                            </html>
                        """.trimIndent()
                    )

                    Log.d(TAG, "Preparando envío de correo a: ${contacto.correo}")

                    // Enviar el correo usando Resend
                    val response = resendService.sendEmail(
                        apiKey = "x",
                        emailRequest = emailRequest
                    )

                    if (response.isSuccessful) {
                        Log.d(TAG, "Alerta enviada exitosamente a ${contacto.correo}")
                    } else {
                        Log.e(TAG, "Error al enviar correo a ${contacto.correo}. Código: ${response.code()}, Mensaje: ${response.message()}")
                    }
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