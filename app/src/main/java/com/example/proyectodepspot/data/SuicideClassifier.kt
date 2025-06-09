package com.example.proyectodepspot.data

import android.content.Context
import android.util.Log
import com.opencsv.CSVReader
import java.io.InputStreamReader
import kotlin.math.ln

class SuicideClassifier(private val context: Context) {
    private val TAG = "SuicideClassifier"
    private var wordProbabilities: MutableMap<String, Pair<Double, Double>> = mutableMapOf()
    private var priorSuicide: Double = 0.0
    private var priorNonSuicide: Double = 0.0

    init {
        Log.d(TAG, "Inicializando SuicideClassifier...")
        trainModel()
    }

    private fun trainModel() {
        try {
            Log.d(TAG, "Intentando abrir el archivo suicide_dataset.csv...")
            // Leer el archivo CSV desde los assets
            val inputStream = context.assets.open("suicide_dataset.csv")
            Log.d(TAG, "Archivo CSV encontrado y abierto correctamente")
            
            val reader = CSVReader(InputStreamReader(inputStream))
            Log.d(TAG, "CSVReader inicializado correctamente")
            
            var suicideCount = 0
            var nonSuicideCount = 0
            val wordCounts = mutableMapOf<String, Pair<Int, Int>>()

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

                        // Tokenizar el mensaje
                        val words = message.split("\\s+".toRegex())
                        words.forEach { word ->
                            val currentCounts = wordCounts.getOrDefault(word, Pair(0, 0))
                            if (isSuicide) {
                                wordCounts[word] = Pair(currentCounts.first + 1, currentCounts.second)
                            } else {
                                wordCounts[word] = Pair(currentCounts.first, currentCounts.second + 1)
                            }
                        }

                        // Log cada 1000 mensajes para no saturar el log
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
            val vocabularySize = wordCounts.size
            wordCounts.forEach { (word, counts) ->
                val pSuicide = ln((counts.first + 1.0) / (suicideCount + vocabularySize))
                val pNonSuicide = ln((counts.second + 1.0) / (nonSuicideCount + vocabularySize))
                wordProbabilities[word] = Pair(pSuicide, pNonSuicide)
            }

            Log.d(TAG, "Modelo entrenado exitosamente")
            Log.d(TAG, "Total de mensajes: $total")
            Log.d(TAG, "Mensajes suicidas: $suicideCount")
            Log.d(TAG, "Mensajes no suicidas: $nonSuicideCount")
            Log.d(TAG, "Tamaño del vocabulario: $vocabularySize")
            Log.d(TAG, "Primeras 5 palabras del vocabulario: ${wordCounts.keys.take(5).joinToString()}")

        } catch (e: Exception) {
            Log.e(TAG, "Error al entrenar el modelo", e)
            e.printStackTrace()
        }
    }

    fun classifyMessage(message: String): Double {
        val words = message.lowercase().split("\\s+".toRegex())
        var logProbSuicide = priorSuicide
        var logProbNonSuicide = priorNonSuicide

        Log.d(TAG, "Clasificando mensaje: $message")
        Log.d(TAG, "Palabras encontradas: ${words.joinToString()}")

        words.forEach { word ->
            val probabilities = wordProbabilities[word]
            if (probabilities != null) {
                logProbSuicide += probabilities.first
                logProbNonSuicide += probabilities.second
                Log.d(TAG, "Palabra '$word' encontrada en el vocabulario")
            } else {
                Log.d(TAG, "Palabra '$word' no encontrada en el vocabulario")
            }
        }

        // Calcular la probabilidad final
        val probSuicide = 1.0 / (1.0 + Math.exp(logProbNonSuicide - logProbSuicide))
        Log.d(TAG, "Probabilidad de suicidio calculada: $probSuicide")
        return probSuicide
    }
} 