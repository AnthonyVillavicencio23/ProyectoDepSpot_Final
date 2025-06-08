package com.example.proyectodepspot

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectodepspot.adapters.MessageAdapter
import com.example.proyectodepspot.data.FirebaseChatRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*

class ChatActivity : AppCompatActivity() {
    private lateinit var chatRepository: FirebaseChatRepository
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var messageInput: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var audioButton: ImageButton
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var currentPopupWindow: PopupWindow? = null
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isFirstInteraction = true
    private var isWaitingForName = false
    private var isWaitingForAge = false

    companion object {
        private const val PERMISSION_REQUEST_CODE = 123
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.RECORD_AUDIO
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Inicializar SpeechRecognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        setupSpeechRecognizer()

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Verificar si el usuario está autenticado
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_chat

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_emotions -> {
                    startActivity(Intent(this, BitacoraEmocionalActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, AjustesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_chat -> true
                else -> false
            }
        }

        // Inicializar el repositorio
        chatRepository = FirebaseChatRepository()

        // Inicializar vistas
        recyclerView = findViewById(R.id.recyclerView)
        messageInput = findViewById(R.id.messageInput)
        sendButton = findViewById(R.id.sendButton)
        audioButton = findViewById(R.id.audioButton)

        // Configurar RecyclerView
        messageAdapter = MessageAdapter(this)
        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = messageAdapter
        }

        // Configurar botón de envío
        sendButton.setOnClickListener {
            val message = messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                handleUserMessage(message)
                messageInput.text.clear()
            }
        }

        // Configurar botón de audio
        audioButton.setOnClickListener {
            if (checkAudioPermission()) {
                startVoiceRecognition()
            } else {
                requestAudioPermission()
            }
        }

        // Inicializar chat
        lifecycleScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            chatRepository.initializeChat(userId)
            observeMessages(userId)
        }
    }

    private fun handleUserMessage(message: String) {
        if (isFirstInteraction) {
            isFirstInteraction = false
            isWaitingForName = true
            // Solo enviamos el mensaje del usuario
            sendMessage(message)
            // El bot responderá automáticamente a través del repositorio
            return
        }

        if (isWaitingForAge) {
            isWaitingForAge = false
            // Solo enviamos el mensaje del usuario
            sendMessage(message)
            // El bot responderá automáticamente a través del repositorio
            return
        }

        // Para el resto de la conversación, solo enviamos el mensaje del usuario
        sendMessage(message)
    }

    private fun setupSpeechRecognizer() {
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                showVoiceRecognitionPopup()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                matches?.get(0)?.let { recognizedText ->
                    messageInput.setText(recognizedText)
                }
                currentPopupWindow?.dismiss()
                currentPopupWindow = null
            }

            override fun onError(error: Int) {
                currentPopupWindow?.dismiss()
                currentPopupWindow = null
                when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> Toast.makeText(this@ChatActivity, "Error de audio", Toast.LENGTH_SHORT).show()
                    SpeechRecognizer.ERROR_CLIENT -> Toast.makeText(this@ChatActivity, "Error del cliente", Toast.LENGTH_SHORT).show()
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Toast.makeText(this@ChatActivity, "Permisos insuficientes", Toast.LENGTH_SHORT).show()
                    SpeechRecognizer.ERROR_NETWORK -> Toast.makeText(this@ChatActivity, "Error de red", Toast.LENGTH_SHORT).show()
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> Toast.makeText(this@ChatActivity, "Tiempo de espera de red agotado", Toast.LENGTH_SHORT).show()
                    SpeechRecognizer.ERROR_NO_MATCH -> Toast.makeText(this@ChatActivity, "No se encontró coincidencia", Toast.LENGTH_SHORT).show()
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Toast.makeText(this@ChatActivity, "Reconocedor ocupado", Toast.LENGTH_SHORT).show()
                    SpeechRecognizer.ERROR_SERVER -> Toast.makeText(this@ChatActivity, "Error del servidor", Toast.LENGTH_SHORT).show()
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Toast.makeText(this@ChatActivity, "Tiempo de espera de voz agotado", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun checkAudioPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestAudioPermission() {
        ActivityCompat.requestPermissions(
            this,
            REQUIRED_PERMISSIONS,
            PERMISSION_REQUEST_CODE
        )
    }

    private fun startVoiceRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-PE")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        
        try {
            speechRecognizer.startListening(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error al iniciar el reconocimiento de voz", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showVoiceRecognitionPopup() {
        val popupView = layoutInflater.inflate(R.layout.popup_voice_recognition, null)
        val popupWindow = PopupWindow(
            popupView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
            true
        )
        
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0)
        currentPopupWindow = popupWindow
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    private fun observeMessages(userId: String) {
        lifecycleScope.launch {
            chatRepository.getMessages(userId).collectLatest { messages ->
                messageAdapter.submitList(messages)
                if (messages.isNotEmpty()) {
                    recyclerView.post {
                        recyclerView.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }
        }
    }

    private fun sendMessage(content: String) {
        val userId = auth.currentUser?.uid ?: return
        lifecycleScope.launch {
            chatRepository.sendMessage(userId, content)
        }
    }
} 