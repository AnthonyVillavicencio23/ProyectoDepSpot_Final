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

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

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

    suspend fun classifyMessage(message: String): Quadruple<Boolean, String, Int, String> {
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
            val (gptResponse, motivo, porcentaje, presuncion) = consultarGPT4(message)
            // Si GPT-4 confirma, usar el porcentaje que nos devuelve
            return if (gptResponse) {
                Quadruple(true, motivo, porcentaje, presuncion)
            } else {
                // Si el análisis local también sugiere no depresión, disminuir la probabilidad
                Quadruple(false, motivo, if (localProbSuicide < 0.5) 5 else 15, "No se detectaron síntomas significativos")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al consultar GPT-4, usando probabilidad local", e)
            return Quadruple(localProbSuicide > 0.5, "No se pudo determinar el motivo", (localProbSuicide * 100).toInt(), "No se pudo realizar un diagnóstico detallado")
        }
    }

    private suspend fun consultarGPT4(message: String): Quadruple<Boolean, String, Int, String> {
        val systemPrompt = """Analiza el mensaje para detectar posibles signos de malestar emocional. Responde (SI/NO)|NIVEL|PORCENTAJE|MOTIVO|PRESUNCION.LEVE (0–40): tristeza, desánimo leve, cansancio o poco interés. MODERADO (41–70): desesperanza o aislamiento, fatiga persistente, cambios de sueño, dificultad en concentrarse. GRAVE (71–100): posible ideación suicida, soledad excesiva o autolesión, desesperanza extrema, pánico severo. Para el MOTIVO, usa términos algo más generales como "problemas familiares", "dificultades académicas" estos son solo ejemplos no los uses. Para la PRESUNCION, describe de forma cautelosa los posibles signos observados sin mencionar los indicadores específicos de gravedad. Usa frases como por ejemplo "podría estar experimentando", "parece mostrar signos de", solo son ejemplos, no uses estos. No menciones términos como "desesperanza extrema", "pensamientos de autolesión" o "ideación suicida" en la presunción, ya que no es un diagnostico definitivo. Ej: SI|GRAVE|85|problemas familiares|El usuario parece mostrar signos de malestar emocional que podrían estar relacionados con conflictos familiares, lo que sugiere que podría estar experimentando dificultades para manejar el estrés diario (No uses este texo literal). No agregues texto extra"""

        val apiMessages = listOf(
            APIMessage(role = "system", content = systemPrompt),
            APIMessage(role = "user", content = message)
        )

        val chatRequest = ChatRequest(
            model = OpenAIConfig.MODEL,
            messages = apiMessages,
            store = true
        )

        val response = openAIService.createChatCompletion(request = chatRequest)
        val gptResponse = response.choices.firstOrNull()?.message?.content?.trim() ?: "NO|LEVE|0|ninguno|No se detectaron síntomas significativos"
        
        val parts = gptResponse.split("|")
        val isDepressed = parts[0].uppercase() == "SI"
        val level = parts.getOrNull(1)?.uppercase() ?: "LEVE"
        val percentage = parts.getOrNull(2)?.toIntOrNull() ?: 0
        val motivo = parts.getOrNull(3)?.trim() ?: "ninguno"
        val presuncion = parts.getOrNull(4)?.trim() ?: "No se pudo determinar una presunción específica"
        
        Log.d(TAG, "Respuesta de GPT-4: $gptResponse")
        
        // Solo mostrar logs de nivel y porcentaje si es SI
        if (isDepressed) {
            Log.d(TAG, "Nivel de malestar emocional: $level")
            Log.d(TAG, "Porcentaje de riesgo: $percentage%")
            Log.d(TAG, "Motivo detectado: $motivo")
            Log.d(TAG, "Presunción: $presuncion")
        }
        
        // Solo retornar true si es SI y es grave con 80% o más
        return Quadruple(isDepressed && level == "GRAVE" && percentage >= 80, motivo, percentage, presuncion)
    }
} 