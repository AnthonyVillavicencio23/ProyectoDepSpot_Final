package com.example.proyectodepspot

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Window
import android.widget.Toast
import java.util.*

class SpeechToTextManager(private val activity: Activity) {
    private var speechDialog: Dialog? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var onResultCallback: ((String) -> Unit)? = null

    fun startSpeechToText(onResult: (String) -> Unit) {
        onResultCallback = onResult
        showListeningDialog()
        
        if (SpeechRecognizer.isRecognitionAvailable(activity)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(activity).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        // Listo para escuchar
                    }

                    override fun onBeginningOfSpeech() {
                        // Comenzó a hablar
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Nivel de audio cambiando
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        // Buffer recibido
                    }

                    override fun onEndOfSpeech() {
                        // Terminó de hablar
                        dismissListeningDialog()
                    }

                    override fun onError(error: Int) {
                        dismissListeningDialog()
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Error de audio"
                            SpeechRecognizer.ERROR_CLIENT -> "Error del cliente"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permisos insuficientes"
                            SpeechRecognizer.ERROR_NETWORK -> "Error de red"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Tiempo de espera de red agotado"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No se encontró coincidencia"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconocedor ocupado"
                            SpeechRecognizer.ERROR_SERVER -> "Error del servidor"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tiempo de espera de voz agotado"
                            else -> "Error desconocido"
                        }
                        Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT).show()
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.get(0)?.let { text ->
                            onResultCallback?.invoke(text)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        // Resultados parciales
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {
                        // Eventos
                    }
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-ES")
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            speechRecognizer?.startListening(intent)
        } else {
            dismissListeningDialog()
            Toast.makeText(activity, "El reconocimiento de voz no está disponible en este dispositivo", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showListeningDialog() {
        speechDialog = Dialog(activity).apply {
            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setContentView(R.layout.dialog_speech_recognition)
            window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setCancelable(false)
            show()
        }
    }

    fun dismissListeningDialog() {
        speechDialog?.dismiss()
        speechDialog = null
    }

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        dismissListeningDialog()
    }
} 