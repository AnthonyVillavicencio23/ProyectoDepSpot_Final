package com.example.proyectodepspot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectodepspot.data.Message
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {
    private val messages = mutableListOf<Message>()
    private val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("America/Lima")
    }

    class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val messageCard: MaterialCardView = view.findViewById(R.id.messageCard)
        val messageText: TextView = view.findViewById(R.id.messageText)
        val timestampText: TextView = view.findViewById(R.id.timestampText)
        val avatarImage: ImageView = view.findViewById(R.id.avatarImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.message_item, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.messageText.text = message.content
        holder.timestampText.text = dateFormat.format(Date(message.timestamp))

        // Configurar el estilo del mensaje según si es propio o del asistente
        val isOwnMessage = message.senderId == FirebaseAuth.getInstance().currentUser?.uid
        if (isOwnMessage) {
            // Mensajes del usuario a la derecha
            holder.messageCard.setCardBackgroundColor(holder.itemView.context.getColor(R.color.message_background_own))
            holder.avatarImage.visibility = View.GONE
            holder.messageCard.layoutParams = (holder.messageCard.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = 64
                marginEnd = 8
            }
            (holder.messageCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).apply {
                endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                horizontalBias = 1.0f
            }
            // Alinear timestamp a la derecha
            (holder.timestampText.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).apply {
                endToEnd = holder.messageCard.id
                startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            }
        } else {
            // Mensajes de la IA a la izquierda
            holder.messageCard.setCardBackgroundColor(holder.itemView.context.getColor(R.color.message_background))
            holder.avatarImage.visibility = View.VISIBLE
            holder.avatarImage.setImageResource(R.drawable.icono_deppy)
            holder.messageCard.layoutParams = (holder.messageCard.layoutParams as ViewGroup.MarginLayoutParams).apply {
                marginStart = 8
                marginEnd = 64
            }
            (holder.messageCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).apply {
                startToEnd = holder.avatarImage.id
                endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                horizontalBias = 0.0f
            }
            // Alinear timestamp a la izquierda
            (holder.timestampText.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams).apply {
                startToStart = holder.messageCard.id
                endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
            }
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: Message) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun removeMessage(message: Message) {
        val position = messages.indexOf(message)
        if (position != -1) {
            messages.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    fun getMessage(position: Int): Message {
        return messages[position]
    }
} 