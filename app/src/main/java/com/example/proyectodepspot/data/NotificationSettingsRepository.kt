package com.example.proyectodepspot.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NotificationSettingsRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val collection = db.collection("notification_settings")

    suspend fun getSettings(): NotificationSettings {
        val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
        
        return try {
            val document = collection.document(userId).get().await()
            if (document.exists()) {
                document.toObject(NotificationSettings::class.java) ?: NotificationSettings(userId = userId)
            } else {
                // Si no existe, crear configuración por defecto
                val defaultSettings = NotificationSettings(userId = userId)
                collection.document(userId).set(defaultSettings).await()
                defaultSettings
            }
        } catch (e: Exception) {
            throw Exception("Error al obtener configuraciones: ${e.message}")
        }
    }

    suspend fun updateSettings(settings: NotificationSettings) {
        val userId = auth.currentUser?.uid ?: throw Exception("Usuario no autenticado")
        
        try {
            collection.document(userId).set(settings).await()
        } catch (e: Exception) {
            throw Exception("Error al actualizar configuraciones: ${e.message}")
        }
    }

    suspend fun updateSoundEnabled(enabled: Boolean) {
        val settings = getSettings()
        updateSettings(settings.copy(soundEnabled = enabled))
    }

    suspend fun updateVibrationEnabled(enabled: Boolean) {
        val settings = getSettings()
        updateSettings(settings.copy(vibrationEnabled = enabled))
    }

    suspend fun updatePushNotificationsEnabled(enabled: Boolean) {
        val settings = getSettings()
        updateSettings(settings.copy(pushNotificationsEnabled = enabled))
    }

    suspend fun updateSelectedRingtone(ringtone: Int) {
        val settings = getSettings()
        updateSettings(settings.copy(selectedRingtone = ringtone))
    }
} 