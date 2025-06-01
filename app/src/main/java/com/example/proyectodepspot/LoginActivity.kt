package com.example.proyectodepspot

import android.content.Intent
import android.os.Bundle
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
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)

        // Configurar botón de inicio de sesión
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
                                task.exception?.message?.contains("password") == true -> 
                                    "La contraseña debe tener al menos 6 caracteres"
                                task.exception?.message?.contains("no user record") == true -> 
                                    "No existe una cuenta con este correo electrónico"
                                task.exception?.message?.contains("badly formatted") == true -> 
                                    "El formato del correo electrónico no es válido"
                                else -> "Error al iniciar sesión: ${task.exception?.message}"
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        // Configurar botón de registro
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

        var isValid = true

        if (email.isEmpty()) {
            tilEmail.error = "El correo electrónico es requerido"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Ingrese un correo electrónico válido"
            isValid = false
        } else {
            tilEmail.error = null
        }

        if (password.isEmpty()) {
            tilPassword.error = "La contraseña es requerida"
            isValid = false
        } else {
            tilPassword.error = null
        }

        return isValid
    }
} 