package com.example.proyectodepspot

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Patterns
import android.widget.Toast

class EmergencyContactPromptActivity : AppCompatActivity() {
    private lateinit var tilContactName: TextInputLayout
    private lateinit var tilContactEmail: TextInputLayout
    private lateinit var etContactName: TextInputEditText
    private lateinit var etContactEmail: TextInputEditText
    private lateinit var btnAddContact: Button
    private lateinit var btnSkip: Button
    private lateinit var tvTitle: TextView
    private lateinit var tvDescription: TextView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_emergency_contact_prompt)

        initializeViews()
        setupClickListeners()
    }

    private fun initializeViews() {
        tilContactName = findViewById(R.id.tilContactName)
        tilContactEmail = findViewById(R.id.tilContactEmail)
        etContactName = findViewById(R.id.etContactName)
        etContactEmail = findViewById(R.id.etContactEmail)
        btnAddContact = findViewById(R.id.btnAddContact)
        btnSkip = findViewById(R.id.btnSkip)
        tvTitle = findViewById(R.id.tvTitle)
        tvDescription = findViewById(R.id.tvDescription)

        tvTitle.text = "¿Necesitas ayuda?"
        tvDescription.text = "Agrega un contacto de emergencia que pueda ayudarte en momentos difíciles. Este contacto recibirá notificaciones cuando detectemos que podrías necesitar ayuda."
    }

    private fun setupClickListeners() {
        btnAddContact.setOnClickListener {
            if (validateInputs()) {
                saveEmergencyContact()
            }
        }

        btnSkip.setOnClickListener {
            navigateToMainActivity()
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        // Validar nombre
        if (etContactName.text.toString().trim().isEmpty()) {
            tilContactName.error = "Por favor ingresa un nombre"
            isValid = false
        } else {
            tilContactName.error = null
        }

        // Validar correo electrónico
        val email = etContactEmail.text.toString().trim()
        if (email.isEmpty()) {
            tilContactEmail.error = "Por favor ingresa un correo electrónico"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilContactEmail.error = "Por favor ingresa un correo electrónico válido"
            isValid = false
        } else {
            tilContactEmail.error = null
        }

        return isValid
    }

    private fun saveEmergencyContact() {
        val userEmail = auth.currentUser?.email ?: return
        val contactName = etContactName.text.toString().trim()
        val contactEmail = etContactEmail.text.toString().trim()

        // Generar un nuevo ID para el contacto
        val docRef = db.collection("contactos_apoyo").document()
        val contactData = hashMapOf(
            "id" to docRef.id,
            "nombre" to contactName,
            "correo" to contactEmail,
            "userEmail" to userEmail
        )

        docRef.set(contactData)
            .addOnSuccessListener {
                Toast.makeText(this, "Contacto guardado exitosamente", Toast.LENGTH_SHORT).show()
                navigateToMainActivity()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar el contacto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
} 