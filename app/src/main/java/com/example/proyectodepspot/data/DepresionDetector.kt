package com.example.proyectodepspot.data

import android.content.Context
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
import java.text.SimpleDateFormat
import java.util.*

class DepresionDetector(private val context: Context) {
    private val TAG = "DepresionDetector"
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val suicideClassifier = SuicideClassifier(context)
    
    // Frases clave que podrían indicar depresión
    private val frasesDepresivas = setOf(
        "me siento triste",
        "estoy deprimido",
        "tengo depresión",
        "pienso en el suicidio",
        "quiero morir",
        "no quiero vivir",
        "me siento sin esperanza",
        "todo es sin sentido",
        "me siento vacío",
        "me siento solo",
        "me siento solitario",
        "estoy desesperado",
        "me siento desesperado",
        "no puedo más",
        "quiero desaparecer",
        "no tengo ganas de nada",
        "no tengo fuerzas",
        "me siento abrumado",
        "no veo salida",
        "todo es oscuro"
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
        
        try {
            // Obtener la probabilidad de que el mensaje sea suicida usando el clasificador
            val (esSuicida, motivo, porcentaje, diagnostico) = suicideClassifier.classifyMessage(mensaje)
            Log.d(TAG, "Probabilidad de suicidio: $porcentaje%")

            // Si la probabilidad es alta (GPT-4 dijo SI o el análisis local es muy alto)
            if (esSuicida && porcentaje >= 85) {
                Log.d(TAG, "Se detectaron signos depresivos en el mensaje. Probabilidad de suicidio: $porcentaje%")
                
                // Actualizar el contador de detecciones
                actualizarContadorDetecciones(userId)
                
                // Enviar alerta a contactos
                enviarAlertaContactos(userId, emptyList(), porcentaje / 100.0, motivo, diagnostico)
            } else {
                Log.d(TAG, "No se detectaron signos depresivos en el mensaje o el porcentaje es menor al 85%")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al analizar mensaje con GPT-4, usando análisis local como respaldo", e)
            
            // Verificar si el mensaje contiene frases depresivas (solo como respaldo)
            val frasesEncontradas = frasesDepresivas.filter { frase ->
                mensajeLower.contains(frase)
            }

            // Si hay frases depresivas, enviar alerta
            if (frasesEncontradas.isNotEmpty()) {
                Log.d(TAG, "Se detectaron signos depresivos en el mensaje (análisis local). Frases encontradas: $frasesEncontradas")
                
                // Actualizar el contador de detecciones
                actualizarContadorDetecciones(userId)
                
                // Enviar alerta a contactos
                enviarAlertaContactos(userId, frasesEncontradas, 0.7, "No se pudo determinar el motivo específico", "Se detectaron signos de depresión en el mensaje del usuario")
            } else {
                Log.d(TAG, "No se detectaron signos depresivos en el mensaje (análisis local)")
            }
        }
    }

    private fun getPeruDate(): String {
        val peruTimeZone = TimeZone.getTimeZone("America/Lima")
        val calendar = Calendar.getInstance()
        calendar.timeZone = peruTimeZone
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale("es", "PE"))
        dateFormat.timeZone = peruTimeZone
        return dateFormat.format(calendar.time)
    }

    private suspend fun actualizarContadorDetecciones(userId: String) {
        try {
            val userEmail = auth.currentUser?.email
            if (userEmail == null) {
                Log.e(TAG, "No se pudo obtener el email del usuario")
                return
            }

            // Obtener la fecha actual en formato dd,mes,año usando la zona horaria de Perú
            val peruTimeZone = TimeZone.getTimeZone("America/Lima")
            val calendar = Calendar.getInstance(peruTimeZone)
            val fechaActual = SimpleDateFormat("dd,MMMM,yyyy", Locale("es", "PE")).format(calendar.time)
            Log.d(TAG, "Fecha actual (Perú): $fechaActual")

            // Referencia al documento de la fecha actual en la subcolección de detecciones
            val deteccionesRef = db.collection("usuarios")
                .document(userId)
                .collection("detecciones")
                .document(fechaActual)

            // Intentar actualizar el contador usando una transacción
            db.runTransaction { transaction ->
                val snapshot = transaction.get(deteccionesRef)
                if (snapshot.exists()) {
                    // Si el documento existe, incrementar el contador
                    val nuevoContador = snapshot.getLong("total")?.plus(1) ?: 1
                    transaction.update(deteccionesRef, "total", nuevoContador)
                    Log.d(TAG, "Contador actualizado a: $nuevoContador para la fecha $fechaActual")
                } else {
                    // Si el documento no existe, crearlo con contador inicial 1
                    transaction.set(deteccionesRef, mapOf(
                        "total" to 1,
                        "fecha" to fechaActual
                    ))
                    Log.d(TAG, "Nuevo documento creado para la fecha $fechaActual con contador inicial: 1")
                }
            }.await()

            Log.d(TAG, "Contador de detecciones actualizado para el usuario $userId en la fecha $fechaActual")
        } catch (e: Exception) {
            Log.e(TAG, "Error al actualizar el contador de detecciones", e)
            e.printStackTrace()
        }
    }

    private suspend fun enviarAlertaContactos(userId: String, frasesEncontradas: List<String>, probabilidadSuicida: Double, motivo: String, diagnostico: String) {
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
                                        border-radius: 8px;
                                        padding: 15px;
                                        margin-bottom: 20px;
                                    }
                                    .info-box {
                                        background-color: #e9ecef;
                                        border-radius: 8px;
                                        padding: 15px;
                                        margin-bottom: 20px;
                                    }
                                    .diagnosis-box {
                                        background-color: #e3f2fd;
                                        border: 1px solid #bbdefb;
                                        border-radius: 8px;
                                        padding: 15px;
                                        margin-bottom: 20px;
                                    }
                                    .button {
                                        display: inline-block;
                                        padding: 10px 20px;
                                        background-color: #007bff;
                                        color: white;
                                        text-decoration: none;
                                        border-radius: 5px;
                                        margin: 10px 0;
                                    }
                                    .footer {
                                        text-align: center;
                                        margin-top: 30px;
                                        font-size: 0.9em;
                                        color: #6c757d;
                                    }
                                </style>
                            </head>
                            <body>
                                <div class="header">
                                    <img src="https://i.ibb.co/3myhSBQB/deppy-despierto2.png" alt="Deppy Logo" class="logo">
                                    <h1>Alerta de Deppy</h1>
                                </div>
                                
                                <div class="alert-box">
                                    <h2>🚨 Alerta de Detección</h2>
                                    <p>Hemos detectado que el usuario <strong>${userEmail}</strong> ha mostrado signos de depresión en su conversación reciente.</p>
                                    <p><strong>Posible punto de dolor/motivo:</strong> ${motivo}</p>
                                </div>

                                <div class="diagnosis-box">
                                    <h3>Diagnóstico:</h3>
                                    <p>${diagnostico}</p>
                                </div>

                                <div class="info-box">
                                    <h3>Detalles de la Situación:</h3>
                                    <ul>
                                        <li>Usuario: ${userEmail}</li>
                                        <li>Probabilidad de riesgo: ${String.format("%.2f", probabilidadSuicida * 100)}%</li>
                                        <li>Motivo detectado: ${motivo}</li>
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
                        apiKey = "zzz",
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