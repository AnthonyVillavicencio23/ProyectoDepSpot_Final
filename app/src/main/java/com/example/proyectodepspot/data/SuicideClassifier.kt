package com.example.proyectodepspot.data

import android.content.Context
import android.util.Log
import com.opencsv.CSVReader
import java.io.InputStreamReader
import kotlin.math.ln
import kotlin.math.min
import com.example.proyectodepspot.api.OpenAIService
import com.example.proyectodepspot.api.ChatRequest
import com.example.proyectodepspot.api.Message as APIMessage
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.proyectodepspot.api.OpenAIConfig

class SuicideClassifier(private val context: Context) {
    private val TAG = "SuicideClassifier"
    private var ngramProbabilities: MutableMap<String, Pair<Double, Double>> = mutableMapOf()
    private var priorSuicide: Double = 0.0
    private var priorNonSuicide: Double = 0.0
    private val ngramSize = 3 // Tamaño de los n-gramas

    // Pesos para diferentes tamaños de n-gramas
    private val ngramWeights = mapOf(
        1 to 1.0,  // Palabras individuales
        2 to 2.0,  // Pares de palabras
        3 to 3.0   // Tríos de palabras
    )

    private val openAIService: OpenAIService by lazy {
        Retrofit.Builder()
            .baseUrl(OpenAIConfig.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenAIService::class.java)
    }

    init {
        Log.d(TAG, "Inicializando SuicideClassifier...")
        trainModel()
    }

    private fun generateNGrams(text: String): List<Pair<String, Int>> {
        val words = text.lowercase().split("\\s+".toRegex())
        val ngrams = mutableListOf<Pair<String, Int>>()
        
        // Generar n-gramas de diferentes tamaños (1 a ngramSize)
        for (n in 1..ngramSize) {
            for (i in 0..words.size - n) {
                val ngram = words.subList(i, i + n).joinToString(" ")
                ngrams.add(Pair(ngram, n)) // Guardamos el tamaño del n-grama
            }
        }
        
        return ngrams
    }

    private fun trainModel() {
        try {
            Log.d(TAG, "Intentando abrir el archivo suicide_dataset.csv...")
            val inputStream = context.assets.open("suicide_dataset.csv")
            Log.d(TAG, "Archivo CSV encontrado y abierto correctamente")
            
            val reader = CSVReader(InputStreamReader(inputStream))
            Log.d(TAG, "CSVReader inicializado correctamente")
            
            var suicideCount = 0
            var nonSuicideCount = 0
            val ngramCounts = mutableMapOf<String, Pair<Int, Int>>()

            // Leer el dataset
            Log.d(TAG, "Comenzando a leer el dataset...")
            val rows = reader.readAll()
            Log.d(TAG, "Número total de filas en el CSV: ${rows.size}")
            
            // Ignorar la primera fila (encabezados) y procesar el resto
            rows.drop(1).forEachIndexed { index, row ->
                if (row.size >= 2) {
                    try {
                        val message = row[0].lowercase()
                        val isSuicide = row[1].toInt() == 1

                        if (isSuicide) suicideCount++ else nonSuicideCount++

                        // Generar n-gramas del mensaje
                        val ngrams = generateNGrams(message)
                        ngrams.forEach { (ngram, _) ->
                            val currentCounts = ngramCounts.getOrDefault(ngram, Pair(0, 0))
                            if (isSuicide) {
                                ngramCounts[ngram] = Pair(currentCounts.first + 1, currentCounts.second)
                            } else {
                                ngramCounts[ngram] = Pair(currentCounts.first, currentCounts.second + 1)
                            }
                        }

                        if (index % 1000 == 0) {
                            Log.d(TAG, "Procesando mensaje $index de ${rows.size - 1}")
                        }
                    } catch (e: NumberFormatException) {
                        Log.w(TAG, "Error al procesar la fila $index: ${row.joinToString()}. Error: ${e.message}")
                    }
                } else {
                    Log.w(TAG, "Fila $index no tiene el formato esperado: ${row.joinToString()}")
                }
            }

            // Calcular probabilidades a priori
            val total = suicideCount + nonSuicideCount
            priorSuicide = ln(suicideCount.toDouble() / total)
            priorNonSuicide = ln(nonSuicideCount.toDouble() / total)

            // Calcular probabilidades condicionales con suavizado de Laplace
            val vocabularySize = ngramCounts.size
            ngramCounts.forEach { (ngram, counts) ->
                val pSuicide = ln((counts.first + 1.0) / (suicideCount + vocabularySize))
                val pNonSuicide = ln((counts.second + 1.0) / (nonSuicideCount + vocabularySize))
                ngramProbabilities[ngram] = Pair(pSuicide, pNonSuicide)
            }

            Log.d(TAG, "Modelo entrenado exitosamente")
            Log.d(TAG, "Total de mensajes: $total")
            Log.d(TAG, "Mensajes suicidas: $suicideCount")
            Log.d(TAG, "Mensajes no suicidas: $nonSuicideCount")
            Log.d(TAG, "Tamaño del vocabulario: $vocabularySize")

        } catch (e: Exception) {
            Log.e(TAG, "Error al entrenar el modelo", e)
            e.printStackTrace()
        }
    }

    suspend fun classifyMessage(message: String): Double {
        val ngrams = generateNGrams(message.lowercase())
        var logProbSuicide = priorSuicide
        var logProbNonSuicide = priorNonSuicide

        Log.d(TAG, "Clasificando mensaje: $message")
        Log.d(TAG, "N-gramas encontrados: ${ngrams.joinToString()}")

        // Contar cuántos n-gramas coinciden con el vocabulario
        var matchedNGrams = 0
        var totalWeight = 0.0
        var matchedWeight = 0.0

        ngrams.forEach { (ngram, size) ->
            val probabilities = ngramProbabilities[ngram]
            val weight = ngramWeights[size] ?: 1.0
            totalWeight += weight

            if (probabilities != null) {
                // Aplicar el peso al cálculo de probabilidades
                logProbSuicide += probabilities.first * weight
                logProbNonSuicide += probabilities.second * weight
                matchedNGrams++
                matchedWeight += weight
                Log.d(TAG, "N-grama '$ngram' (tamaño $size, peso $weight) encontrado en el vocabulario")
            } else {
                Log.d(TAG, "N-grama '$ngram' (tamaño $size, peso $weight) no encontrado en el vocabulario")
            }
        }

        // Si no hay suficientes n-gramas coincidentes, reducir la confianza
        val matchRatio = if (totalWeight > 0) matchedWeight / totalWeight else 0.0
        val confidenceMultiplier = if (matchRatio < 0.3) matchRatio else 1.0

        // Calcular la probabilidad final del análisis local
        val localProbSuicide = (1.0 / (1.0 + Math.exp(logProbNonSuicide - logProbSuicide))) * confidenceMultiplier
        Log.d(TAG, "Probabilidad local de suicidio calculada: $localProbSuicide (ratio de coincidencia: $matchRatio)")

        // Siempre consultar a GPT-4 para validación
        try {
            val gptResponse = consultarGPT4(message)
            // Si GPT-4 confirma, dar más peso a su decisión
            return if (gptResponse) {
                // Si el análisis local también sugiere depresión, aumentar la probabilidad
                if (localProbSuicide > 0.5) 0.95 else 0.85
            } else {
                // Si el análisis local también sugiere no depresión, disminuir la probabilidad
                if (localProbSuicide < 0.5) 0.05 else 0.15
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al consultar GPT-4, usando probabilidad local", e)
            return localProbSuicide
        }
    }

    private suspend fun consultarGPT4(message: String): Boolean {
        val systemPrompt = """
            Eres un asistente especializado en detectar signos de depresión y pensamientos suicidas. Analiza bien las negaciones y frase del usuario.
            Analiza el siguiente mensaje y responde SOLO con "SI" si detectas signos claros de depresión,
            o "NO" si no detectas estos signos. No incluyas ninguna otra explicación o texto.
        """.trimIndent()

        val apiMessages = listOf(
            APIMessage(role = "system", content = systemPrompt),
            APIMessage(role = "user", content = message)
        )

        val chatRequest = ChatRequest(
            model = OpenAIConfig.MODEL,
            messages = apiMessages
        )

        val response = openAIService.createChatCompletion(request = chatRequest)
        val gptResponse = response.choices.firstOrNull()?.message?.content?.trim()?.uppercase() ?: "NO"
        
        Log.d(TAG, "Respuesta de GPT-4: $gptResponse")
        return gptResponse == "SI"
    }
} 