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
    private val minRequestInterval = 3000L // Aumentado a 3 segundos entre solicitudes
    private val maxRetries = 3 // Aumentado el número máximo de reintentos
    private val backoffMultiplier = 2L
    private val maxBackoffTime = 15000L // 25 segundos máximo de espera

    private suspend fun makeOpenAIRequest(apiMessages: List<APIMessage>, maxRetries: Int = 5): String {
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
                    messages = apiMessages,
                    store = true
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
                if (e is retrofit2.HttpException) {
                    when (e.code()) {
                        429 -> {
                            // Calcular tiempo de espera con backoff exponencial
                            val backoffTime = minOf(
                                (backoffMultiplier shl retryCount) * 1000L,
                                maxBackoffTime
                            )
                            Log.d(TAG, "Error 429 (Too Many Requests), reintentando en ${backoffTime}ms")
                            kotlinx.coroutines.delay(backoffTime)
                            retryCount++
                        }
                        401 -> {
                            Log.e(TAG, "Error de autenticación (401)")
                            throw e
                        }
                        403 -> {
                            Log.e(TAG, "Error de autorización (403)")
                            throw e
                        }
                        else -> {
                            Log.e(TAG, "Error HTTP ${e.code()}")
                            throw e
                        }
                    }
                } else {
                    Log.e(TAG, "Error en la solicitud a OpenAI", e)
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

    suspend fun sendMessage(userId: String, content: String, systemMessage: String = OpenAIConfig.SYSTEM_PROMPT) {
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
        depresionDetector.analizarMensaje(userId, content)

        // Enviar mensaje del usuario
        val newMessage = Message(
            senderId = userId,
            content = content,
            timestamp = timestamp
        )

        usuariosCollection.document(userId)
            .collection("messages")
            .add(newMessage)
            .await()

        try {
            // Obtener historial de mensajes (últimos 8 mensajes)
            val messageHistory = usuariosCollection
                .document(userId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(8)
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

            // Crear lista de mensajes para la API
            val apiMessages = mutableListOf<APIMessage>().apply {
                // Combinar el mensaje del sistema con la información del usuario y el detector de depresión
                add(APIMessage(
                    role = "system",
                    content = """
                        $systemMessage
                        ${OpenAIConfig.SYSTEM_PROMPT}
                    """.trimIndent()
                ))
                // Agregar historial de mensajes
                addAll(messageHistory)
            }

            // Para el resto de la conversación, usar OpenAI
            val chatRequest = ChatRequest(
                model = OpenAIConfig.MODEL,
                messages = apiMessages
            )

            val response = openAIService.createChatCompletion(request = chatRequest)
            val botResponse = response.choices.firstOrNull()?.message?.content ?: "Lo siento, no pude procesar tu mensaje."

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
                content = "Lo siento, hubo un error al procesar tu mensaje. Por favor, intenta de nuevo en unos momentos.",
                timestamp = System.currentTimeMillis()
            )

            usuariosCollection.document(userId)
                .collection("messages")
                .add(errorMessage)
                .await()
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

        // Si no hay mensajes, obtener datos del usuario y enviar mensaje de bienvenida personalizado
        if (existingMessages.isEmpty()) {
            val userDoc = usuariosCollection.document(userId).get().await()
            val userName = userDoc.getString("nombre") ?: ""
            val userAge = userDoc.getLong("edad")?.toInt() ?: 0
            val userUsername = userDoc.getString("username") ?: ""

            val welcomeMessage = Message(
                senderId = "bot_depresion",
                content = when {
                    userName.isNotEmpty() && userAge > 0 && userUsername.isNotEmpty() -> 
                        "¡Hola $userUsername! Mi nombre es Deppy, tu compañero emocional, y me alegra que estés aquí. Veo que tienes $userAge años. ¿Cómo te sientes hoy? Estoy aquí para escucharte y apoyarte."
                    userName.isNotEmpty() && userUsername.isNotEmpty() -> 
                        "¡Hola $userUsername! Me alegra que estés aquí. ¿Cómo te sientes hoy? Estoy aquí para escucharte y apoyarte."
                    userName.isNotEmpty() -> 
                        "¡Hola $userName! Me alegra que estés aquí. ¿Cómo te sientes hoy? Estoy aquí para escucharte y apoyarte."
                    else -> 
                        "¡Hola! Me gustaría conocerte un poco más, ¿cuál es tu nombre?"
                },
                timestamp = System.currentTimeMillis()
            )

            usuariosCollection.document(userId)
                .collection("messages")
                .add(welcomeMessage)
                .await()
        }
    }

    suspend fun processMessageWithoutSaving(userId: String, content: String) {
        try {
            // Obtener historial de mensajes (últimos 10 mensajes)
            val messageHistory = usuariosCollection
                .document(userId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val message = doc.toObject(Message::class.java)
                    if (message != null) {
                        APIMessage(
                            role = if (message.senderId == userId) "user" else "assistant",
                            content = message.content
                        )
                    } else null
                }
                .reversed()

            // Crear lista de mensajes para la API incluyendo el mensaje actual
            val apiMessages = mutableListOf<APIMessage>().apply {
                // Agregar mensaje del sistema
                add(APIMessage(
                    role = "system",
                    content = OpenAIConfig.SYSTEM_PROMPT
                ))
                // Agregar historial de mensajes
                addAll(messageHistory)
                // Agregar el mensaje actual como contexto
                add(APIMessage(
                    role = "user",
                    content = content
                ))
            }

            // Usar OpenAI para generar respuesta
            val chatRequest = ChatRequest(
                model = OpenAIConfig.MODEL,
                messages = apiMessages
            )

            Log.d(TAG, "Request a OpenAI: $chatRequest")
            val response = openAIService.createChatCompletion(request = chatRequest)
            Log.d(TAG, "Respuesta de OpenAI: $response")
            
            val botResponse = response.choices.firstOrNull()?.message?.content 
                ?: "Lo siento, no pude procesar tu mensaje."

            // Enviar solo la respuesta del bot
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
                content = "Lo siento, hubo un error al procesar tu mensaje. Por favor, intenta de nuevo. Error: ${e.message}",
                timestamp = System.currentTimeMillis()
            )

            usuariosCollection.document(userId)
                .collection("messages")
                .add(errorMessage)
                .await()
        }
    }
} 