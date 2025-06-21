package com.example.proyectodepspot

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
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
        setupTextChangeListeners()
        setupRegisterButton()
        setupBackButton()
        setupTerminosClick()
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

    private fun setupTextChangeListeners() {
        val fields = listOf(
            tilNombre, tilApellido, tilUsername, tilTelefono,
            tilFechaNacimiento, tilEmail, tilPassword
        )

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
                val age = calculateAge()
                if (age in 13..18) {
                    clearError(tilFechaNacimiento)
                    updateEdadDisplay(age)
                } else {
                    validateFechaNacimiento()
                }
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

    private fun calculateAge(): Int {
        if (fechaNacimiento != null) {
            val today = Calendar.getInstance()
            val birthDate = Calendar.getInstance()
            birthDate.time = fechaNacimiento!!

            var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
                age--
            }
            return age
        }
        return 0
    }

    private fun updateEdadDisplay(age: Int) {
        tvEdad.text = "Edad: $age años"
    }

    private fun validateFechaNacimiento(): Boolean {
        return when {
            fechaNacimiento == null -> {
                tilFechaNacimiento.error = "La fecha de nacimiento es requerida"
                tilFechaNacimiento.isErrorEnabled = true
                tvEdad.text = "Edad: --"
                false
            }
            else -> {
                val age = calculateAge()
                when {
                    age < 13 -> {
                        tilFechaNacimiento.error = "Debes tener al menos 13 años"
                        tilFechaNacimiento.isErrorEnabled = true
                        tvEdad.text = "Edad: --"
                        false
                    }
                    age > 18 -> {
                        tilFechaNacimiento.error = "Debes tener máximo 18 años"
                        tilFechaNacimiento.isErrorEnabled = true
                        tvEdad.text = "Edad: --"
                        false
                    }
                    else -> {
                        clearError(tilFechaNacimiento)
                        updateEdadDisplay(age)
                        true
                    }
                }
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
                                "edad" to calculateAge(),
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

    private fun setupBackButton() {
        findViewById<MaterialButton>(R.id.buttonBack).setOnClickListener {
            onBackPressed()
        }
    }

    private fun clearError(field: TextInputLayout) {
        field.error = null
        field.isErrorEnabled = false
    }

    private fun validateNombre(nombre: String): Boolean {
        return when {
            nombre.isEmpty() -> {
                tilNombre.error = "El nombre es requerido"
                tilNombre.isErrorEnabled = true
                false
            }
            nombre.length < 2 -> {
                tilNombre.error = "El nombre debe tener al menos 2 caracteres"
                tilNombre.isErrorEnabled = true
                false
            }
            nombre.length > 30 -> {
                tilNombre.error = "El nombre no debe exceder los 30 caracteres"
                tilNombre.isErrorEnabled = true
                false
            }
            !nombre.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) -> {
                tilNombre.error = "El nombre solo debe contener letras"
                tilNombre.isErrorEnabled = true
                false
            }
            else -> {
                clearError(tilNombre)
                true
            }
        }
    }

    private fun validateApellido(apellido: String): Boolean {
        return when {
            apellido.isEmpty() -> {
                tilApellido.error = "El apellido es requerido"
                tilApellido.isErrorEnabled = true
                false
            }
            apellido.length < 2 -> {
                tilApellido.error = "El apellido debe tener al menos 2 caracteres"
                tilApellido.isErrorEnabled = true
                false
            }
            apellido.length > 30 -> {
                tilApellido.error = "El apellido no debe exceder los 30 caracteres"
                tilApellido.isErrorEnabled = true
                false
            }
            !apellido.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$")) -> {
                tilApellido.error = "El apellido solo debe contener letras"
                tilApellido.isErrorEnabled = true
                false
            }
            else -> {
                clearError(tilApellido)
                true
            }
        }
    }

    private fun validateUsername(username: String): Boolean {
        return when {
            username.isEmpty() -> {
                tilUsername.error = "El nombre de usuario es requerido"
                tilUsername.isErrorEnabled = true
                false
            }
            username.length < 4 -> {
                tilUsername.error = "El nombre de usuario debe tener al menos 4 caracteres"
                tilUsername.isErrorEnabled = true
                false
            }
            username.length > 15 -> {
                tilUsername.error = "El nombre de usuario no debe exceder los 15 caracteres"
                tilUsername.isErrorEnabled = true
                false
            }
            !username.matches(Regex("^[a-zA-Z0-9_]+$")) -> {
                tilUsername.error = "El nombre de usuario solo puede contener letras, números y guiones bajos"
                tilUsername.isErrorEnabled = true
                false
            }
            else -> {
                clearError(tilUsername)
                true
            }
        }
    }

    private fun validateTelefono(telefono: String): Boolean {
        return when {
            telefono.isEmpty() -> {
                tilTelefono.error = "El teléfono es requerido"
                tilTelefono.isErrorEnabled = true
                false
            }
            !telefono.matches(Regex("^[0-9]{9}$")) -> {
                tilTelefono.error = "El teléfono debe tener 9 dígitos"
                tilTelefono.isErrorEnabled = true
                false
            }
            else -> {
                clearError(tilTelefono)
                true
            }
        }
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
            !email.endsWith("@gmail.com") -> {
                tilEmail.error = "Solo se permiten correos de Gmail"
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
            password.length < 8 -> {
                tilPassword.error = "La contraseña debe tener al menos 8 caracteres"
                tilPassword.isErrorEnabled = true
                false
            }
            password.length > 30 -> {
                tilPassword.error = "La contraseña no debe exceder los 30 caracteres"
                tilPassword.isErrorEnabled = true
                false
            }
            !password.matches(Regex(".*[A-Z].*")) -> {
                tilPassword.error = "La contraseña debe contener al menos una mayúscula"
                tilPassword.isErrorEnabled = true
                false
            }
            !password.matches(Regex(".*[a-z].*")) -> {
                tilPassword.error = "La contraseña debe contener al menos una minúscula"
                tilPassword.isErrorEnabled = true
                false
            }
            !password.matches(Regex(".*[0-9].*")) -> {
                tilPassword.error = "La contraseña debe contener al menos un número"
                tilPassword.isErrorEnabled = true
                false
            }
            else -> {
                clearError(tilPassword)
                true
            }
        }
    }

    private fun setupTerminosClick() {
        val checkBox = findViewById<MaterialCheckBox>(R.id.cbConsentimiento)
        val text = "Acepto los términos y condiciones"
        val spannableString = SpannableString(text)
        
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                startActivity(Intent(this@RegistroActivity, TerminosActivity::class.java))
            }

            override fun updateDrawState(ds: TextPaint) {
                super.updateDrawState(ds)
                ds.isUnderlineText = true
                ds.color = getColor(R.color.purple_500)
            }
        }

        val startIndex = text.indexOf("términos y condiciones")
        if (startIndex != -1) {
            spannableString.setSpan(
                clickableSpan,
                startIndex,
                startIndex + "términos y condiciones".length,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        checkBox.text = spannableString
        checkBox.movementMethod = LinkMovementMethod.getInstance()
    }
} 