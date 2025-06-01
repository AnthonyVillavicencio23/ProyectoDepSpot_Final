package com.example.proyectodepspot.data

data class NotificationSettings(
    val userId: String = "",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val pushNotificationsEnabled: Boolean = true,
    val selectedRingtone: Int = 1 // 1: Clásico, 2: Suave, 3: Moderno
) 