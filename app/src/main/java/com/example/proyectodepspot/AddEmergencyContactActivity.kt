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
import java.util.UUID

class AddEmergencyContactActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tilContactName: TextInputLayout
    private lateinit var tilPhoneNumber: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_emergency_contact)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inicializar vistas
        tilContactName = findViewById(R.id.tilContactName)
        tilPhoneNumber = findViewById(R.id.tilPhoneNumber)

        // Configurar botón de agregar contacto
        findViewById<MaterialButton>(R.id.btnAddContact).setOnClickListener {
            if (validateFields()) {
                saveEmergencyContact()
            }
        }
    }

    private fun validateFields(): Boolean {
        val contactName = tilContactName.editText?.text.toString()
        val phoneNumber = tilPhoneNumber.editText?.text.toString()

        var isValid = true

        if (contactName.isEmpty()) {
            tilContactName.error = "El nombre del contacto es requerido"
            isValid = false
        } else {
            tilContactName.error = null
        }

        if (phoneNumber.isEmpty()) {
            tilPhoneNumber.error = "El número de teléfono es requerido"
            isValid = false
        } else if (!phoneNumber.matches(Regex("^[+]?[0-9]{10,13}$"))) {
            tilPhoneNumber.error = "Ingrese un número de teléfono válido"
            isValid = false
        } else {
            tilPhoneNumber.error = null
        }

        return isValid
    }

    private fun saveEmergencyContact() {
        val userEmail = auth.currentUser?.email
        if (userEmail != null) {
            val contactName = tilContactName.editText?.text.toString()
            val phoneNumber = tilPhoneNumber.editText?.text.toString()

            val contact = hashMapOf(
                "id" to UUID.randomUUID().toString(),
                "nombre" to contactName,
                "telefono" to phoneNumber,
                "userEmail" to userEmail
            )

            db.collection("contactos_apoyo")
                .add(contact)
                .addOnSuccessListener {
                    Toast.makeText(this, "Contacto agregado exitosamente", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, ChatActivity::class.java))
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al agregar contacto: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
} 