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
import com.google.firebase.firestore.FirebaseFirestore

class SplashActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Ocultar la barra de acción para el splash screen
        supportActionBar?.hide()

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Iniciar animación del logo
        val logo = findViewById<ShapeableImageView>(R.id.ivLogo)
        val logoAnimation = AnimationUtils.loadAnimation(this, R.anim.logo_animation)
        logo.startAnimation(logoAnimation)

        // Seleccionar mensaje aleatorio
        val messages = resources.getStringArray(R.array.splash_messages)
        val randomMessage = messages.random()
        findViewById<TextView>(R.id.tvMessage).text = randomMessage

        // Mostrar el splash por 3 segundos y luego verificar autenticación
        Handler(Looper.getMainLooper()).postDelayed({
            if (auth.currentUser != null) {
                // Usuario autenticado, verificar contactos de emergencia
                checkEmergencyContacts()
            } else {
                // Usuario no autenticado, ir a la pantalla de bienvenida
                startActivity(Intent(this, WelcomeActivity::class.java))
                finish()
            }
        }, 3000)
    }

    private fun checkEmergencyContacts() {
        val userEmail = auth.currentUser?.email
        if (userEmail != null) {
            db.collection("contactos_apoyo")
                .whereEqualTo("userEmail", userEmail)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        // No hay contactos de emergencia, mostrar prompt
                        startActivity(Intent(this, EmergencyContactPromptActivity::class.java))
                    } else {
                        // Hay contactos de emergencia, ir al chat
                        startActivity(Intent(this, ChatActivity::class.java))
                    }
                    finish()
                }
                .addOnFailureListener { e ->
                    // En caso de error, ir al chat
                    startActivity(Intent(this, ChatActivity::class.java))
                    finish()
                }
        }
    }
} 