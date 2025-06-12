package com.example.proyectodepspot

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.imageview.ShapeableImageView
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

        // Iniciar animación del logo
        val logo = findViewById<ShapeableImageView>(R.id.ivLogo)
        val logoAnimation = AnimationUtils.loadAnimation(this, R.anim.logo_animation)
        logo.startAnimation(logoAnimation)

        // Seleccionar mensaje aleatorio
        val messages = resources.getStringArray(R.array.splash_messages)
        val randomMessage = messages.random()
        findViewById<TextView>(R.id.tvMessage).text = randomMessage

        // Mostrar el splash por 4 segundos y luego verificar autenticación
        Handler(Looper.getMainLooper()).postDelayed({
            if (auth.currentUser != null) {
                // Usuario autenticado, ir al chat
                startActivity(Intent(this, ChatActivity::class.java))
            } else {
                // Usuario no autenticado, ir a la pantalla de bienvenida
                startActivity(Intent(this, WelcomeActivity::class.java))
            }
            finish()
        }, 3000)
    }
} 