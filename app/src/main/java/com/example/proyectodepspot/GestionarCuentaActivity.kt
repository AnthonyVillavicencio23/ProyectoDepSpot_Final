package com.example.proyectodepspot

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class GestionarCuentaActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var nombreTextView: TextView
    private lateinit var apellidoTextView: TextView
    private lateinit var usernameTextView: TextView
    private lateinit var fechaNacimientoTextView: TextView
    private lateinit var telefonoTextView: TextView
    private lateinit var correoTextView: TextView
    private var currentPopupWindow: PopupWindow? = null
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestionar_cuenta)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inicializar vistas
        nombreTextView = findViewById(R.id.nombreTextView)
        apellidoTextView = findViewById(R.id.apellidoTextView)
        usernameTextView = findViewById(R.id.usernameTextView)
        fechaNacimientoTextView = findViewById(R.id.fechaNacimientoTextView)
        telefonoTextView = findViewById(R.id.celularTextView)
        correoTextView = findViewById(R.id.correoTextView)

        // Configurar la barra superior
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Configurar botones de edición
        findViewById<ImageButton>(R.id.editNombreButton).setOnClickListener {
            showEditPopup("Nombre", nombreTextView.text.toString()) { newValue ->
                updateField("nombre", newValue) { nombreTextView.text = newValue }
            }
        }

        findViewById<ImageButton>(R.id.editApellidoButton).setOnClickListener {
            showEditPopup("Apellido", apellidoTextView.text.toString()) { newValue ->
                updateField("apellido", newValue) { apellidoTextView.text = newValue }
            }
        }

        findViewById<ImageButton>(R.id.editUsernameButton).setOnClickListener {
            showEditPopup("Nombre de usuario", usernameTextView.text.toString()) { newValue ->
                updateField("username", newValue) { usernameTextView.text = newValue }
            }
        }

        findViewById<ImageButton>(R.id.editFechaNacimientoButton).setOnClickListener {
            showDatePicker()
        }

        findViewById<ImageButton>(R.id.editCelularButton).setOnClickListener {
            showEditPopup("Teléfono", telefonoTextView.text.toString()) { newValue ->
                updateField("telefono", newValue) { telefonoTextView.text = newValue }
            }
        }

        findViewById<ImageButton>(R.id.editCorreoButton).setOnClickListener {
            showEditPopup("Correo electrónico", correoTextView.text.toString()) { newValue ->
                updateEmail(newValue)
            }
        }

        // Cargar datos actuales
        cargarDatosUsuario()
    }

    private fun updateEmail(newEmail: String) {
        // Actualizar solo en Firestore
        updateField("email", newEmail) {
            correoTextView.text = newEmail
            Toast.makeText(this, "Correo actualizado correctamente", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val currentDate = fechaNacimientoTextView.text.toString()
        
        if (currentDate.isNotEmpty()) {
            try {
                val date = dateFormatter.parse(currentDate)
                calendar.time = date
            } catch (e: Exception) {
                // Si hay error al parsear la fecha, usar la fecha actual
            }
        }

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                val formattedDate = dateFormatter.format(calendar.time)
                updateField("fechaNacimiento", formattedDate) {
                    fechaNacimientoTextView.text = formattedDate
                }
            },
            year,
            month,
            day
        ).show()
    }

    private fun showEditPopup(title: String, currentValue: String, onSave: (String) -> Unit) {
        val popupView = layoutInflater.inflate(R.layout.popup_edit_field, null)
        
        // Calcular el ancho del diálogo (90% del ancho de la pantalla)
        val displayMetrics = resources.displayMetrics
        val width = (displayMetrics.widthPixels * 0.9).toInt()
        
        val popupWindow = PopupWindow(
            popupView,
            width,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )

        // Configurar título
        popupView.findViewById<TextView>(R.id.titleTextView).text = "Editar $title"

        // Configurar campo de texto
        val editText = popupView.findViewById<EditText>(R.id.editText)
        editText.setText(currentValue)

        // Configurar botones
        popupView.findViewById<MaterialButton>(R.id.cancelButton).setOnClickListener {
            popupWindow.dismiss()
        }

        popupView.findViewById<MaterialButton>(R.id.saveButton).setOnClickListener {
            val newValue = editText.text.toString().trim()
            if (newValue.isNotEmpty()) {
                onSave(newValue)
                popupWindow.dismiss()
            } else {
                Toast.makeText(this, "El campo no puede estar vacío", Toast.LENGTH_SHORT).show()
            }
        }

        // Crear un fondo oscuro
        val rootView = window.decorView.findViewById<ViewGroup>(android.R.id.content)
        val darkOverlay = View(this).apply {
            setBackgroundColor(Color.BLACK)
            alpha = 0f // Comenzar transparente
        }
        rootView.addView(darkOverlay, ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        ))

        // Animar la aparición del fondo oscuro
        darkOverlay.animate()
            .alpha(0.5f)
            .setDuration(200)
            .start()

        // Agregar fondo oscuro
        popupWindow.setOnDismissListener {
            // Animar el desvanecimiento del fondo oscuro
            darkOverlay.animate()
                .alpha(0f)
                .setDuration(200)
                .withEndAction {
                    rootView.removeView(darkOverlay)
                }
                .start()
        }

        // Mostrar popup
        popupWindow.showAtLocation(rootView, Gravity.CENTER, 0, 0)
        currentPopupWindow = popupWindow

        // Asegurarnos de que el popup tenga el estilo correcto
        popupWindow.setBackgroundDrawable(resources.getDrawable(android.R.color.transparent, theme))
        popupWindow.elevation = 8f
    }

    private fun updateField(field: String, value: String, onSuccess: () -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        val update = mapOf(field to value)

        db.collection("usuarios")
            .document(userId)
            .update(update)
            .addOnSuccessListener {
                Toast.makeText(this, "Campo actualizado correctamente", Toast.LENGTH_SHORT).show()
                onSuccess()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al actualizar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cargarDatosUsuario() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("usuarios")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        nombreTextView.text = document.getString("nombre")
                        apellidoTextView.text = document.getString("apellido")
                        usernameTextView.text = document.getString("username")
                        fechaNacimientoTextView.text = document.getString("fechaNacimiento")
                        telefonoTextView.text = document.getString("telefono")
                        correoTextView.text = document.getString("email")
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error al cargar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun guardarDatosUsuario() {
        val userId = auth.currentUser?.uid ?: return
        val nombre = nombreTextView.text.toString()
        val apellido = apellidoTextView.text.toString()
        val fechaNacimiento = fechaNacimientoTextView.text.toString()
        
        val calendar = Calendar.getInstance()
        if (fechaNacimiento.isNotEmpty()) {
            try {
                val date = dateFormatter.parse(fechaNacimiento)
                calendar.time = date
            } catch (e: Exception) {
                // Si hay error al parsear la fecha, usar la fecha actual
            }
        }

        val userData = hashMapOf(
            "nombre" to nombre,
            "apellido" to apellido,
            "fechaNacimiento" to calendar.time
        )

        db.collection("usuarios")
            .document(userId)
            .set(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
} 