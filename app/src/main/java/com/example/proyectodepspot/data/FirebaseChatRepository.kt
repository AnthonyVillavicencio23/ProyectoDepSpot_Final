package com.example.proyectodepspot.data

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import com.example.proyectodepspot.api.OpenAIConfig
import com.example.proyectodepspot.api.ChatRequest
import com.example.proyectodepspot.api.Message as APIMessage
import com.example.proyectodepspot.api.OpenAIService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class FirebaseChatRepository(private val context: Context) {
    private val TAG = "FirebaseChatRepository"
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val usuariosCollection = db.collection("usuarios")
    private val depresionDetector = DepresionDetector(context)
    
    private val openAIService = Retrofit.Builder()
        .baseUrl(OpenAIConfig.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OpenAIService::class.java)

    private val responseCache = mutableMapOf<String, String>()
    private var lastRequestTime = 0L
    private val minRequestInterval = 2000L // Aumentado a 2 segundos entre solicitudes
    private val maxRetries = 3
    private val backoffMultiplier = 2L

    private suspend fun makeOpenAIRequest(apiMessages: List<APIMessage>, maxRetries: Int = 3): String {
        var retryCount = 0
        var lastError: Exception? = null

        while (retryCount < maxRetries) {
            try {
                // Esperar si es necesario para respetar el límite de rate
                val currentTime = System.currentTimeMillis()
                val timeSinceLastRequest = currentTime - lastRequestTime
                if (timeSinceLastRequest < minRequestInterval) {
                    kotlinx.coroutines.delay(minRequestInterval - timeSinceLastRequest)
                }

                // Verificar caché antes de hacer la solicitud
                val messageKey = apiMessages.joinToString("|") { "${it.role}:${it.content}" }
                val cachedResponse = responseCache[messageKey]
                if (cachedResponse != null) {
                    Log.d(TAG, "Usando respuesta en caché")
                    return cachedResponse
                }

                val chatRequest = ChatRequest(
                    model = OpenAIConfig.MODEL,
                    messages = apiMessages
                )

                Log.d(TAG, "Request a OpenAI: $chatRequest")
                val response = openAIService.createChatCompletion(request = chatRequest)
                Log.d(TAG, "Respuesta de OpenAI: $response")
                
                lastRequestTime = System.currentTimeMillis()
                val responseContent = response.choices.firstOrNull()?.message?.content 
                    ?: "Lo siento, no pude procesar tu mensaje."

                // Guardar en caché
                responseCache[messageKey] = responseContent
                
                return responseContent

            } catch (e: Exception) {
                lastError = e
                if (e is retrofit2.HttpException && e.code() == 429) {
                    // Esperar con backoff exponencial
                    val backoffTime = (backoffMultiplier shl retryCount) * 1000L
                    Log.d(TAG, "Error 429, reintentando en ${backoffTime}ms")
                    kotlinx.coroutines.delay(backoffTime)
                    retryCount++
                } else {
                    throw e
                }
            }
        }

        throw lastError ?: Exception("Error desconocido después de $maxRetries intentos")
    }

    private fun getCachedResponse(message: String): String? {
        return responseCache[message]
    }

    private fun cacheResponse(message: String, response: String) {
        responseCache[message] = response
        // Limitar el tamaño del caché
        if (responseCache.size > 50) { // Reducido de 100 a 50
            responseCache.clear()
        }
    }

    fun getMessages(userId: String): Flow<List<Message>> = callbackFlow {
        val listener = usuariosCollection
            .document(userId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                }?.distinctBy { it.timestamp } ?: emptyList()
                
                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(userId: String, content: String, sendAsUser: Boolean = true) {
        val timestamp = System.currentTimeMillis()

        // Verificar si ya existe un mensaje con el mismo contenido y timestamp
        val existingMessage = usuariosCollection
            .document(userId)
            .collection("messages")
            .whereEqualTo("content", content)
            .whereEqualTo("timestamp", timestamp)
            .get()
            .await()
            .documents
            .firstOrNull()

        if (existingMessage != null) {
            Log.d(TAG, "Mensaje duplicado detectado, ignorando...")
            return
        }

        // Analizar el mensaje en busca de signos depresivos
        if (sendAsUser) {
            depresionDetector.analizarMensaje(userId, content)
        }

        // Enviar mensaje del usuario o del bot según el parámetro sendAsUser
        val newMessage = Message(
            senderId = if (sendAsUser) userId else "bot_depresion",
            content = content,
            timestamp = timestamp
        )

        usuariosCollection.document(userId)
            .collection("messages")
            .add(newMessage)
            .await()

        // Si el mensaje es del usuario, procesar la respuesta
        if (sendAsUser) {
            try {
                // Verificar si tenemos una respuesta en caché
                val cachedResponse = getCachedResponse(content)
                if (cachedResponse != null) {
                    val botMessage = Message(
                        senderId = "bot_depresion",
                        content = cachedResponse,
                        timestamp = System.currentTimeMillis()
                    )
                    usuariosCollection.document(userId)
                        .collection("messages")
                        .add(botMessage)
                        .await()
                    return
                }

                // Obtener historial de mensajes (últimos 5 mensajes)
                val messageHistory = usuariosCollection
                    .document(userId)
                    .collection("messages")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { doc ->
                        val messageObj = doc.toObject(Message::class.java)
                        if (messageObj != null) {
                            APIMessage(
                                role = if (messageObj.senderId == userId) "user" else "assistant",
                                content = messageObj.content
                            )
                        } else null
                    }
                    .reversed()

                // Determinar si estamos en el flujo de introducción
                val userMessages = messageHistory.count { it.role == "user" }
                val isFirstUserMessage = userMessages == 1
                val isSecondUserMessage = userMessages == 2

                // Crear lista de mensajes para la API
                val apiMessages = mutableListOf<APIMessage>().apply {
                    // Agregar mensaje del sistema
                    add(APIMessage(
                        role = "system",
                        content = OpenAIConfig.SYSTEM_PROMPT
                    ))
                    // Agregar historial de mensajes
                    addAll(messageHistory)
                }

                // Si es el primer mensaje del usuario (respuesta al nombre)
                if (isFirstUserMessage) {
                    val botResponse = "¡Hola $content! ¿Cuántos años tienes?"
                    val botMessage = Message(
                        senderId = "bot_depresion",
                        content = botResponse,
                        timestamp = System.currentTimeMillis()
                    )
                    usuariosCollection.document(userId)
                        .collection("messages")
                        .add(botMessage)
                        .await()
                    return
                }

                // Si es el segundo mensaje del usuario (respuesta a la edad)
                if (isSecondUserMessage) {
                    val botResponse = "Gracias por compartir eso conmigo. Soy Deppy, tu asistente de apoyo emocional. Estoy aquí para escucharte y ayudarte en lo que necesites. ¿Cómo te sientes hoy?"
                    val botMessage = Message(
                        senderId = "bot_depresion",
                        content = botResponse,
                        timestamp = System.currentTimeMillis()
                    )
                    usuariosCollection.document(userId)
                        .collection("messages")
                        .add(botMessage)
                        .await()
                    return
                }

                // Para el resto de la conversación, usar OpenAI con manejo de reintentos
                val botResponse = makeOpenAIRequest(apiMessages)
                
                // Guardar la respuesta en caché
                cacheResponse(content, botResponse)

                // Enviar respuesta del bot
                val botMessage = Message(
                    senderId = "bot_depresion",
                    content = botResponse,
                    timestamp = System.currentTimeMillis()
                )

                usuariosCollection.document(userId)
                    .collection("messages")
                    .add(botMessage)
                    .await()

            } catch (e: Exception) {
                Log.e(TAG, "Error al comunicarse con OpenAI", e)
                // En caso de error, enviar mensaje de fallback
                val errorMessage = Message(
                    senderId = "bot_depresion",
                    content = "Lo siento, hubo un error al procesar tu mensaje. Por favor, intenta de nuevo en unos momentos. Error: ${e.message}",
                    timestamp = System.currentTimeMillis()
                )

                usuariosCollection.document(userId)
                    .collection("messages")
                    .add(errorMessage)
                    .await()
            }
        }
    }

    suspend fun initializeChat(userId: String) {
        // Verificar si el usuario ya tiene mensajes
        val existingMessages = usuariosCollection
            .document(userId)
            .collection("messages")
            .get()
            .await()
            .documents

        // Si no hay mensajes, enviar mensaje de bienvenida
        if (existingMessages.isEmpty()) {
            val welcomeMessage = Message(
                senderId = "bot_depresion",
                content = "Me gustaría conocerte un poco más, ¿cuál es tu nombre?",
                timestamp = System.currentTimeMillis()
            )

            usuariosCollection.document(userId)
                .collection("messages")
                .add(welcomeMessage)
                .await()
        }
    }
} 