package com.example.proyectodepspot

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Ocultar la barra de acción para el splash screen
        supportActionBar?.hide()

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Mostrar el splash por 2 segundos y luego verificar autenticación
        Handler(Looper.getMainLooper()).postDelayed({
            if (auth.currentUser != null) {
                // Usuario autenticado, ir al chat
                startActivity(Intent(this, ChatActivity::class.java))
            } else {
                // Usuario no autenticado, ir a la pantalla de bienvenida
                startActivity(Intent(this, WelcomeActivity::class.java))
            }
            finish()
        }, 2000)
    }
} 