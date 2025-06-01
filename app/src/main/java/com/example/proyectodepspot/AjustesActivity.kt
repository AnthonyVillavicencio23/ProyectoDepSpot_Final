package com.example.proyectodepspot

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.proyectodepspot.data.FirebaseChatRepository
import android.widget.Toast

class AjustesActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tvNombreCompleto: TextView
    private lateinit var tvApodo: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajustes)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inicializar vistas
        tvNombreCompleto = findViewById(R.id.tvNombreCompleto)
        tvApodo = findViewById(R.id.tvApodo)

        // Cargar datos del usuario
        cargarDatosUsuario()

        // Configurar la barra superior
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Configurar los botones
        setupButtons()

        // Configurar la navegación inferior
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        // Recargar los datos del usuario cada vez que la actividad vuelve a estar en primer plano
        cargarDatosUsuario()
    }

    private fun cargarDatosUsuario() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("usuarios")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val nombre = document.getString("nombre") ?: ""
                        val apellido = document.getString("apellido") ?: ""
                        val username = document.getString("username") ?: ""

                        // Actualizar la UI con los datos del usuario
                        tvNombreCompleto.text = "$nombre $apellido"
                        tvApodo.text = "@$username"
                    }
                }
                .addOnFailureListener { _ ->
                    // Manejar el error
                    tvNombreCompleto.text = "Error al cargar datos"
                    tvApodo.text = ""
                }
        }
    }

    private fun setupButtons() {
        // Botón de red de apoyo
        findViewById<MaterialButton>(R.id.buttonSupportNetwork).setOnClickListener {
            startActivity(Intent(this, RedApoyoActivity::class.java))
        }

        // Botón de gestionar cuenta
        findViewById<MaterialButton>(R.id.buttonManageAccount).setOnClickListener {
            startActivity(Intent(this, GestionarCuentaActivity::class.java))
        }

        // Botón de evaluación de depresión
        findViewById<MaterialButton>(R.id.buttonDepressionTest).setOnClickListener {
            startActivity(Intent(this, EvaluacionDepresionActivity::class.java))
        }

        // Botón de notificaciones
        findViewById<MaterialButton>(R.id.buttonNotifications).setOnClickListener {
            startActivity(Intent(this, NotificacionesActivity::class.java))
        }

        // Botón de cerrar sesión
        findViewById<MaterialButton>(R.id.buttonLogout).setOnClickListener {
            showLogoutConfirmationDialog()
        }
    }

    private fun showLogoutConfirmationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_logout_confirmation, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnSi).setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnNo).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_settings

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_emotions -> {
                    startActivity(Intent(this, BitacoraEmocionalActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> true
                R.id.nav_chat -> {
                    startActivity(Intent(this, ChatActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }
} 