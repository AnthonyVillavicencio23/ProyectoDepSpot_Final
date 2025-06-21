package com.example.proyectodepspot.data

data class NotificationSettings(
    val userId: String = "",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = false,
    val pushNotificationsEnabled: Boolean = false,
    val selectedRingtone: Int = 1 // 1: Clásico, 2: Suave, 3: Moderno
) 