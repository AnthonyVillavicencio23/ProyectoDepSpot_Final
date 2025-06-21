package com.example.proyectodepspot

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.util.Patterns

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
        setupTextChangeListeners()
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

    private fun setupTextChangeListeners() {
        val fields = listOf(tilContactName, tilContactEmail)

        fields.forEach { field ->
            field.editText?.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    clearError(field)
                }
            })
        }
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
        val contactName = etContactName.text.toString().trim()
        val contactEmail = etContactEmail.text.toString().trim()

        val isNameValid = validateName(contactName)
        val isEmailValid = validateEmail(contactEmail)

        return isNameValid && isEmailValid
    }

    private fun validateName(name: String): Boolean {
        return when {
            name.isEmpty() -> {
                tilContactName.error = "El nombre es requerido"
                tilContactName.isErrorEnabled = true
                false
            }
            name.length < 4 -> {
                tilContactName.error = "El nombre debe tener al menos 4 caracteres"
                tilContactName.isErrorEnabled = true
                false
            }
            name.length > 30 -> {
                tilContactName.error = "El nombre no puede exceder 30 caracteres"
                tilContactName.isErrorEnabled = true
                false
            }
            !name.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) -> {
                tilContactName.error = "El nombre solo debe contener letras"
                tilContactName.isErrorEnabled = true
                false
            }
            else -> {
                clearError(tilContactName)
                true
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                tilContactEmail.error = "El correo electrónico es requerido"
                tilContactEmail.isErrorEnabled = true
                false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                tilContactEmail.error = "Ingrese un correo electrónico válido"
                tilContactEmail.isErrorEnabled = true
                false
            }
            else -> {
                clearError(tilContactEmail)
                true
            }
        }
    }

    private fun clearError(field: TextInputLayout) {
        field.error = null
        field.isErrorEnabled = false
    }

    private fun saveEmergencyContact() {
        val userEmail = auth.currentUser?.email ?: return
        val contactName = etContactName.text.toString().trim()
        val contactEmail = etContactEmail.text.toString().trim()

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