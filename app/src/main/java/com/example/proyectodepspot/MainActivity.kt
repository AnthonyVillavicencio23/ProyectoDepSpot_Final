package com.example.proyectodepspot

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Iniciar ChatActivity después de un breve delay
        android.os.Handler(mainLooper).postDelayed({
            startActivity(Intent(this, ChatActivity::class.java))
            finish() // Cerrar MainActivity para que no se pueda volver atrás
        }, 500) // Medio segundo de delay
    }
} 