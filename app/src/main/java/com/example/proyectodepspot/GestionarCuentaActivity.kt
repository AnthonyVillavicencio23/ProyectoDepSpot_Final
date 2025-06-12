package com.example.proyectodepspot

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
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
import com.google.android.material.textfield.TextInputLayout
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
                val age = calculateAge(calendar.time)
                
                when {
                    age < 13 -> {
                        Toast.makeText(this, "Debes tener al menos 13 años", Toast.LENGTH_SHORT).show()
                    }
                    age > 18 -> {
                        Toast.makeText(this, "Debes tener máximo 18 años", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val formattedDate = dateFormatter.format(calendar.time)
                        updateField("fechaNacimiento", formattedDate) {
                            fechaNacimientoTextView.text = formattedDate
                        }
                    }
                }
            },
            year,
            month,
            day
        ).show()
    }

    private fun calculateAge(birthDate: Date): Int {
        val today = Calendar.getInstance()
        val birthCalendar = Calendar.getInstance()
        birthCalendar.time = birthDate

        var age = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) {
            age--
        }
        return age
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
        val textInputLayout = popupView.findViewById<TextInputLayout>(R.id.textInputLayout)
        editText.setText(currentValue)

        // Agregar TextWatcher para limpiar errores
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                clearError(textInputLayout)
            }
        })

        // Configurar botones
        popupView.findViewById<MaterialButton>(R.id.cancelButton).setOnClickListener {
            popupWindow.dismiss()
        }

        popupView.findViewById<MaterialButton>(R.id.saveButton).setOnClickListener {
            val newValue = editText.text.toString().trim()
            if (validateField(title, newValue, textInputLayout)) {
                onSave(newValue)
                popupWindow.dismiss()
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

    private fun validateField(fieldName: String, value: String, textInputLayout: TextInputLayout): Boolean {
        return when (fieldName) {
            "Nombre" -> validateNombre(value, textInputLayout)
            "Apellido" -> validateApellido(value, textInputLayout)
            "Nombre de usuario" -> validateUsername(value, textInputLayout)
            "Teléfono" -> validateTelefono(value, textInputLayout)
            "Correo electrónico" -> validateEmail(value, textInputLayout)
            else -> true
        }
    }

    private fun validateNombre(nombre: String, textInputLayout: TextInputLayout): Boolean {
        return when {
            nombre.isEmpty() -> {
                textInputLayout.error = "El nombre es requerido"
                textInputLayout.isErrorEnabled = true
                false
            }
            nombre.length < 4 -> {
                textInputLayout.error = "El nombre debe tener al menos 4 caracteres"
                textInputLayout.isErrorEnabled = true
                false
            }
            !nombre.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) -> {
                textInputLayout.error = "El nombre solo debe contener letras"
                textInputLayout.isErrorEnabled = true
                false
            }
            else -> {
                clearError(textInputLayout)
                true
            }
        }
    }

    private fun validateApellido(apellido: String, textInputLayout: TextInputLayout): Boolean {
        return when {
            apellido.isEmpty() -> {
                textInputLayout.error = "El apellido es requerido"
                textInputLayout.isErrorEnabled = true
                false
            }
            apellido.length < 2 -> {
                textInputLayout.error = "El apellido debe tener al menos 2 caracteres"
                textInputLayout.isErrorEnabled = true
                false
            }
            !apellido.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) -> {
                textInputLayout.error = "El apellido solo debe contener letras"
                textInputLayout.isErrorEnabled = true
                false
            }
            else -> {
                clearError(textInputLayout)
                true
            }
        }
    }

    private fun validateUsername(username: String, textInputLayout: TextInputLayout): Boolean {
        return when {
            username.isEmpty() -> {
                textInputLayout.error = "El nombre de usuario es requerido"
                textInputLayout.isErrorEnabled = true
                false
            }
            username.length < 4 -> {
                textInputLayout.error = "El nombre de usuario debe tener al menos 4 caracteres"
                textInputLayout.isErrorEnabled = true
                false
            }
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> {
                textInputLayout.error = "El nombre de usuario solo puede contener letras, números y guiones bajos"
                textInputLayout.isErrorEnabled = true
                false
            }
            else -> {
                clearError(textInputLayout)
                true
            }
        }
    }

    private fun validateTelefono(telefono: String, textInputLayout: TextInputLayout): Boolean {
        return when {
            telefono.isEmpty() -> {
                textInputLayout.error = "El teléfono es requerido"
                textInputLayout.isErrorEnabled = true
                false
            }
            !telefono.matches(Regex("^[0-9]{9}$")) -> {
                textInputLayout.error = "El teléfono debe tener 9 dígitos"
                textInputLayout.isErrorEnabled = true
                false
            }
            else -> {
                clearError(textInputLayout)
                true
            }
        }
    }

    private fun validateEmail(email: String, textInputLayout: TextInputLayout): Boolean {
        return when {
            email.isEmpty() -> {
                textInputLayout.error = "El correo electrónico es requerido"
                textInputLayout.isErrorEnabled = true
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                textInputLayout.error = "Ingrese un correo electrónico válido"
                textInputLayout.isErrorEnabled = true
                false
            }
            else -> {
                clearError(textInputLayout)
                true
            }
        }
    }

    private fun clearError(field: TextInputLayout) {
        field.error = null
        field.isErrorEnabled = false
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