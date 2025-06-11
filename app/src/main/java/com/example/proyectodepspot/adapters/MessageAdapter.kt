package com.example.proyectodepspot.adapters

import android.content.Context
import android.media.MediaPlayer
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectodepspot.R
import com.example.proyectodepspot.data.Message
import com.example.proyectodepspot.data.NotificationSettingsRepository
import com.example.proyectodepspot.utils.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val context: Context
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    private var messages: List<Message> = emptyList()
    private var lastMessageTimestamp: Long = 0
    private var mediaPlayer: MediaPlayer? = null
    private val settingsRepository = NotificationSettingsRepository()
    private val notificationHelper = NotificationHelper(context)
    private val coroutineScope = CoroutineScope(Dispatchers.Main)

    fun submitList(newMessages: List<Message>) {
        val previousSize = messages.size
        val isNewMessage = newMessages.size > previousSize
        
        // Solo reproducir sonido y vibrar si es un mensaje nuevo del bot
        if (isNewMessage) {
            val lastMessage = newMessages.last()
            if (lastMessage.senderId == "bot_depresion" && 
                lastMessage.timestamp > lastMessageTimestamp) {
                // Verificar si el mensaje es realmente nuevo (no es parte de la carga inicial)
                val timeSinceLastMessage = System.currentTimeMillis() - lastMessage.timestamp
                if (timeSinceLastMessage < 5000) { // Si el mensaje es de los últimos 5 segundos
                    coroutineScope.launch {
                        try {
                            val settings = settingsRepository.getSettings()
                            if (settings.soundEnabled) {
                                playMessageSound(settings.selectedRingtone)
                            }
                            if (settings.vibrationEnabled) {
                                vibrate()
                            }
                            if (settings.pushNotificationsEnabled) {
                                notificationHelper.showMessageNotification(lastMessage.content)
                            }
                        } catch (e: Exception) {
                            // Si hay error al obtener configuraciones, usar valores por defecto
                            playMessageSound(1)
                            vibrate()
                        }
                    }
                }
                lastMessageTimestamp = lastMessage.timestamp
            }
        }
        
        messages = newMessages
        notifyDataSetChanged()
    }

    private fun playMessageSound(selectedRingtone: Int) {
        // Liberar el MediaPlayer anterior si existe
        mediaPlayer?.release()
        
        // Seleccionar el recurso de sonido según el tono elegido
        val soundResource = when (selectedRingtone) {
            1 -> R.raw.message_sound
            2 -> R.raw.message_sound_soft
            3 -> R.raw.message_sound_modern
            else -> R.raw.message_sound
        }
        
        // Crear y reproducir el sonido
        mediaPlayer = MediaPlayer.create(context, soundResource)
        mediaPlayer?.setOnCompletionListener { mp ->
            mp.release()
        }
        mediaPlayer?.start()
    }

    private fun vibrate() {
        val vibrator = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
        
        // Mostrar Toast para confirmar que se intentó vibrar
        Toast.makeText(context, "Vibración activada", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int {
        return messages.size
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
        val messageTime: TextView = itemView.findViewById(R.id.messageTime)
        val messageContainer: View = itemView.findViewById(R.id.messageContainer)
        val botIcon: ImageView = itemView.findViewById(R.id.botIcon)
        val rootLayout: ConstraintLayout = itemView.findViewById(R.id.rootLayout)
        val typingProgress: ProgressBar = itemView.findViewById(R.id.typingProgress)

        fun bind(message: Message) {
            messageText.text = message.content
            
            // Configurar la zona horaria de Perú
            val peruTimeZone = TimeZone.getTimeZone("America/Lima")
            val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            dateFormat.timeZone = peruTimeZone
            
            // Convertir el timestamp a la hora de Perú
            val date = Date(message.timestamp)
            val peruTime = dateFormat.format(date)
            messageTime.text = peruTime
            
            val isOwnMessage = message.senderId == FirebaseAuth.getInstance().currentUser?.uid
            
            // Mostrar/ocultar el icono de Deepy
            botIcon.visibility = if (isOwnMessage) View.GONE else View.VISIBLE
            
            // Configurar el fondo del mensaje
            messageContainer.setBackgroundResource(
                if (isOwnMessage) R.drawable.bg_message_sent
                else R.drawable.bg_message_received
            )

            // Configurar la alineación del mensaje
            val constraintSet = ConstraintSet()
            constraintSet.clone(rootLayout)

            if (isOwnMessage) {
                // Mensaje del usuario (derecha)
                constraintSet.clear(R.id.messageContainer, ConstraintSet.START)
                constraintSet.connect(R.id.messageContainer, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                messageText.gravity = android.view.Gravity.END
                
                // Alinear la hora a la derecha
                constraintSet.clear(R.id.messageTime, ConstraintSet.START)
                constraintSet.connect(R.id.messageTime, ConstraintSet.END, R.id.messageContainer, ConstraintSet.END)
                messageTime.gravity = android.view.Gravity.END
            } else {
                // Mensaje del bot (izquierda)
                constraintSet.clear(R.id.messageContainer, ConstraintSet.END)
                constraintSet.connect(R.id.messageContainer, ConstraintSet.START, R.id.botIcon, ConstraintSet.END)
                messageText.gravity = android.view.Gravity.START
                
                // Alinear la hora a la izquierda
                constraintSet.clear(R.id.messageTime, ConstraintSet.END)
                constraintSet.connect(R.id.messageTime, ConstraintSet.START, R.id.messageContainer, ConstraintSet.START)
                messageTime.gravity = android.view.Gravity.START
            }

            constraintSet.applyTo(rootLayout)
        }
    }
} 