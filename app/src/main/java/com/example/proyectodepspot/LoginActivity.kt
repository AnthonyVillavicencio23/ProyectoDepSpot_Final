package com.example.proyectodepspot

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Si el usuario ya está autenticado, verificar contactos de emergencia
        if (auth.currentUser != null) {
            checkEmergencyContacts()
            return
        }

        // Inicializar vistas
        initializeViews()
        setupTextChangeListeners()
        setupLoginButton()
        setupRegisterButton()
    }

    private fun initializeViews() {
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
    }

    private fun setupTextChangeListeners() {
        val fields = listOf(tilEmail, tilPassword)

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

    private fun setupLoginButton() {
        findViewById<MaterialButton>(R.id.btnLogin).setOnClickListener {
            if (validateFields()) {
                val email = tilEmail.editText?.text.toString()
                val password = tilPassword.editText?.text.toString()
                
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                            checkEmergencyContacts()
                        } else {
                            val errorMessage = when {
                                task.exception?.message?.contains("password") == true || 
                                task.exception?.message?.contains("malformed") == true -> {
                                    tilPassword.error = "La contraseña no pertenece a este correo"
                                    tilPassword.isErrorEnabled = true
                                    "La contraseña no pertenece a este correo"
                                }
                                task.exception?.message?.contains("no user record") == true -> {
                                    tilEmail.error = "No existe una cuenta con este correo electrónico"
                                    tilEmail.isErrorEnabled = true
                                    "No existe una cuenta con este correo electrónico"
                                }
                                task.exception?.message?.contains("badly formatted") == true -> {
                                    tilEmail.error = "El formato del correo electrónico no es válido"
                                    tilEmail.isErrorEnabled = true
                                    "El formato del correo electrónico no es válido"
                                }
                                else -> "Error al iniciar sesión: ${task.exception?.message}"
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private fun setupRegisterButton() {
        findViewById<MaterialButton>(R.id.btnRegister).setOnClickListener {
            startActivity(Intent(this, RegistroActivity::class.java))
        }
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
                    Toast.makeText(this, "Error al verificar contactos: ${e.message}", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, ChatActivity::class.java))
                    finish()
                }
        }
    }

    private fun validateFields(): Boolean {
        val email = tilEmail.editText?.text.toString()
        val password = tilPassword.editText?.text.toString()

        val isEmailValid = validateEmail(email)
        val isPasswordValid = validatePassword(password)

        return isEmailValid && isPasswordValid
    }

    private fun clearError(field: TextInputLayout) {
        field.error = null
        field.isErrorEnabled = false
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                tilEmail.error = "El correo electrónico es requerido"
                tilEmail.isErrorEnabled = true
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                tilEmail.error = "Ingrese un correo electrónico válido"
                tilEmail.isErrorEnabled = true
                false
            }
            else -> {
                clearError(tilEmail)
                true
            }
        }
    }

    private fun validatePassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                tilPassword.error = "La contraseña es requerida"
                tilPassword.isErrorEnabled = true
                false
            }
            password.length < 6 -> {
                tilPassword.error = "La contraseña debe tener al menos 6 caracteres"
                tilPassword.isErrorEnabled = true
                false
            }
            else -> {
                clearError(tilPassword)
                true
            }
        }
    }
} 