package com.example.proyectodepspot

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class EmergencyContactPromptActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_contact_prompt)

        // Configurar botón de agregar contacto de emergencia
        findViewById<MaterialButton>(R.id.btnAddEmergency).setOnClickListener {
            startActivity(Intent(this, AddEmergencyContactActivity::class.java))
            finish()
        }

        // Configurar botón de omitir
        findViewById<MaterialButton>(R.id.btnSkip).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
            finish()
        }
    }
} 