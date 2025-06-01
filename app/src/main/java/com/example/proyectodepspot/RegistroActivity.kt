package com.example.proyectodepspot

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class RegistroActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var tilNombre: TextInputLayout
    private lateinit var tilApellido: TextInputLayout
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilTelefono: TextInputLayout
    private lateinit var tilFechaNacimiento: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var cbConsentimiento: MaterialCheckBox
    private lateinit var tvEdad: TextView

    private val calendar = Calendar.getInstance()
    private var fechaNacimiento: Date? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        // Inicializar Firebase
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Inicializar vistas
        initializeViews()
        setupDatePicker()
        setupValidations()
        setupRegisterButton()
    }

    private fun initializeViews() {
        tilNombre = findViewById(R.id.tilNombre)
        tilApellido = findViewById(R.id.tilApellido)
        tilUsername = findViewById(R.id.tilUsername)
        tilTelefono = findViewById(R.id.tilTelefono)
        tilFechaNacimiento = findViewById(R.id.tilFechaNacimiento)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        cbConsentimiento = findViewById(R.id.cbConsentimiento)
        tvEdad = findViewById(R.id.tvEdad)
    }

    private fun setupDatePicker() {
        val etFechaNacimiento = findViewById<TextInputEditText>(R.id.etFechaNacimiento)
        etFechaNacimiento.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                calendar.set(selectedYear, selectedMonth, selectedDay)
                fechaNacimiento = calendar.time
                updateFechaNacimientoDisplay()
                calculateAge()
            },
            year,
            month,
            day
        ).show()
    }

    private fun updateFechaNacimientoDisplay() {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        findViewById<TextInputEditText>(R.id.etFechaNacimiento).setText(
            fechaNacimiento?.let { dateFormat.format(it) }
        )
    }

    private fun calculateAge() {
        if (fechaNacimiento != null) {
            val today = Calendar.getInstance()
            val birthDate = Calendar.getInstance()
            birthDate.time = fechaNacimiento!!

            var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            tvEdad.text = "Edad: $age años"
        }
    }

    private fun setupValidations() {
        // Validación de nombre
        tilNombre.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateNombre(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Validación de apellido
        tilApellido.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateApellido(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Validación de username
        tilUsername.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateUsername(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Validación de teléfono
        tilTelefono.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateTelefono(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Validación de email
        tilEmail.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateEmail(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Validación de contraseña
        tilPassword.editText?.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validatePassword(s.toString())
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun validateNombre(nombre: String): Boolean {
        return when {
            nombre.isEmpty() -> {
                tilNombre.error = "El nombre es requerido"
                false
            }
            nombre.length < 2 -> {
                tilNombre.error = "El nombre debe tener al menos 2 caracteres"
                false
            }
            !nombre.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) -> {
                tilNombre.error = "El nombre solo debe contener letras"
                false
            }
            else -> {
                tilNombre.error = null
                true
            }
        }
    }

    private fun validateApellido(apellido: String): Boolean {
        return when {
            apellido.isEmpty() -> {
                tilApellido.error = "El apellido es requerido"
                false
            }
            apellido.length < 2 -> {
                tilApellido.error = "El apellido debe tener al menos 2 caracteres"
                false
            }
            !apellido.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) -> {
                tilApellido.error = "El apellido solo debe contener letras"
                false
            }
            else -> {
                tilApellido.error = null
                true
            }
        }
    }

    private fun validateUsername(username: String): Boolean {
        return when {
            username.isEmpty() -> {
                tilUsername.error = "El nombre de usuario es requerido"
                false
            }
            username.length < 4 -> {
                tilUsername.error = "El nombre de usuario debe tener al menos 4 caracteres"
                false
            }
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> {
                tilUsername.error = "El nombre de usuario solo puede contener letras, números y guiones bajos"
                false
            }
            else -> {
                tilUsername.error = null
                true
            }
        }
    }

    private fun validateTelefono(telefono: String): Boolean {
        return when {
            telefono.isEmpty() -> {
                tilTelefono.error = "El teléfono es requerido"
                false
            }
            !telefono.matches(Regex("^[0-9]{9}$")) -> {
                tilTelefono.error = "El teléfono debe tener 9 dígitos"
                false
            }
            else -> {
                tilTelefono.error = null
                true
            }
        }
    }

    private fun validateEmail(email: String): Boolean {
        return when {
            email.isEmpty() -> {
                tilEmail.error = "El correo electrónico es requerido"
                false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                tilEmail.error = "Ingrese un correo electrónico válido"
                false
            }
            else -> {
                tilEmail.error = null
                true
            }
        }
    }

    private fun validatePassword(password: String): Boolean {
        return when {
            password.isEmpty() -> {
                tilPassword.error = "La contraseña es requerida"
                false
            }
            password.length < 8 -> {
                tilPassword.error = "La contraseña debe tener al menos 8 caracteres"
                false
            }
            !password.matches(Regex(".*[A-Z].*")) -> {
                tilPassword.error = "La contraseña debe contener al menos una mayúscula"
                false
            }
            !password.matches(Regex(".*[a-z].*")) -> {
                tilPassword.error = "La contraseña debe contener al menos una minúscula"
                false
            }
            !password.matches(Regex(".*[0-9].*")) -> {
                tilPassword.error = "La contraseña debe contener al menos un número"
                false
            }
            else -> {
                tilPassword.error = null
                true
            }
        }
    }

    private fun validateFechaNacimiento(): Boolean {
        return when {
            fechaNacimiento == null -> {
                tilFechaNacimiento.error = "La fecha de nacimiento es requerida"
                false
            }
            else -> {
                tilFechaNacimiento.error = null
                true
            }
        }
    }

    private fun setupRegisterButton() {
        findViewById<MaterialButton>(R.id.btnRegistrar).setOnClickListener {
            if (validateAllFields()) {
                val email = tilEmail.editText?.text.toString()
                val password = tilPassword.editText?.text.toString()
                val nombre = tilNombre.editText?.text.toString()
                val apellido = tilApellido.editText?.text.toString()
                val username = tilUsername.editText?.text.toString()
                val telefono = tilTelefono.editText?.text.toString()
                val fechaNac = fechaNacimiento?.let { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) }

                // Registrar usuario en Firebase Authentication
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Guardar datos adicionales en Firestore
                            val user = auth.currentUser
                            val userData = hashMapOf(
                                "nombre" to nombre,
                                "apellido" to apellido,
                                "username" to username,
                                "telefono" to telefono,
                                "fechaNacimiento" to fechaNac,
                                "email" to email
                            )

                            db.collection("usuarios")
                                .document(user?.uid ?: "")
                                .set(userData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Registro exitoso", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            val errorMessage = when {
                                task.exception?.message?.contains("password") == true -> 
                                    "La contraseña debe tener al menos 6 caracteres"
                                task.exception?.message?.contains("already in use") == true -> 
                                    "Ya existe una cuenta con este correo electrónico"
                                task.exception?.message?.contains("badly formatted") == true -> 
                                    "El formato del correo electrónico no es válido"
                                else -> "Error al registrarse: ${task.exception?.message}"
                            }
                            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private fun validateAllFields(): Boolean {
        val nombre = tilNombre.editText?.text.toString()
        val apellido = tilApellido.editText?.text.toString()
        val username = tilUsername.editText?.text.toString()
        val telefono = tilTelefono.editText?.text.toString()
        val email = tilEmail.editText?.text.toString()
        val password = tilPassword.editText?.text.toString()

        val isNombreValid = validateNombre(nombre)
        val isApellidoValid = validateApellido(apellido)
        val isUsernameValid = validateUsername(username)
        val isTelefonoValid = validateTelefono(telefono)
        val isFechaNacimientoValid = validateFechaNacimiento()
        val isEmailValid = validateEmail(email)
        val isPasswordValid = validatePassword(password)
        val isConsentimientoValid = cbConsentimiento.isChecked

        if (!isConsentimientoValid) {
            Toast.makeText(this, "Debe aceptar los términos y condiciones", Toast.LENGTH_SHORT).show()
        }

        return isNombreValid && isApellidoValid && isUsernameValid && isTelefonoValid &&
                isFechaNacimientoValid && isEmailValid && isPasswordValid && isConsentimientoValid
    }
} 