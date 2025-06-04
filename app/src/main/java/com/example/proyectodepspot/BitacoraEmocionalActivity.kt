package com.example.proyectodepspot

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.TimeZone
import java.text.SimpleDateFormat
import java.util.Locale
import android.view.View
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import android.util.Log
import com.google.firebase.Timestamp





class BitacoraEmocionalActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var fraseMotivadora: TextView
    private lateinit var desafioDiario: TextView
    private lateinit var btnCompletado: Button
    private lateinit var fraseMotivadoraIA: TextView
    private lateinit var desafioDiarioIA: TextView
    private lateinit var btnCompletadoIA: Button
    private lateinit var btnGenerarDesafio: Button
    private lateinit var textViewFechaLimitePredeterminado: TextView
    private lateinit var btnResumenDesafios: Button
    private lateinit var btnResumenDesafiosIA: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bitacora_emocional)

        try {
            // Inicializar Firebase
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()

            // Inicializar vistas
            fraseMotivadora = findViewById(R.id.fraseMotivadora)
            desafioDiario = findViewById(R.id.desafioDiario)
            btnCompletado = findViewById(R.id.btnCompletado)
            fraseMotivadoraIA = findViewById(R.id.fraseMotivadoraIA)
            desafioDiarioIA = findViewById(R.id.desafioDiarioIA)
            btnCompletadoIA = findViewById(R.id.btnCompletadoIA)
            btnGenerarDesafio = findViewById(R.id.btnGenerarDesafio)
            textViewFechaLimitePredeterminado = findViewById(R.id.textViewFechaLimitePredeterminado)
            btnResumenDesafios = findViewById(R.id.btnResumenDesafios)
            btnResumenDesafiosIA = findViewById(R.id.btnResumenDesafiosIA)

            // Configurar navegación y botones
            setupBottomNavigation()
            setupBotones()
            setupCalendarioButton()
            setupResumenButtons()

            // Cargar desafíos
            cargarDesafioDiario()
            cargarDesafioIA()
        } catch (e: Exception) {
            Toast.makeText(this, "Error al inicializar: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupBotones() {
        btnCompletado.setOnClickListener {
            marcarDesafioCompletado()
        }

        btnCompletadoIA.setOnClickListener {
            marcarDesafioIACompletado()
        }

        btnGenerarDesafio.setOnClickListener {
            generarDesafioIA()
        }
    }

    private fun getPeruDate(): String {
        val peruTimeZone = TimeZone.getTimeZone("America/Lima")
        val calendar = Calendar.getInstance()
        calendar.timeZone = peruTimeZone
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale("es", "PE"))
        dateFormat.timeZone = peruTimeZone
        return dateFormat.format(calendar.time)
    }

    private fun cargarDesafioDiario() {
        val userId = auth.currentUser?.uid ?: return
        val fechaActual = getPeruDate()

        // Logs de depuración
        val localTime = Calendar.getInstance().time
        val now = Timestamp.now()
        val peruCalendar = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"))
        Log.d("DEBUG_TIME", "Local time: $localTime")
        Log.d("DEBUG_TIME", "Firestore timestamp: $now")
        Log.d("DEBUG_TIME", "Peru time: ${peruCalendar.time}")
        Log.d("DEBUG_TIME", "Fecha actual (Peru): $fechaActual")
        Log.d("DEBUG_TIME", "TimeZone Peru: ${peruCalendar.timeZone.id}")
        Log.d("DEBUG_TIME", "TimeZone Local: ${TimeZone.getDefault().id}")

        db.collection("usuarios")
            .document(userId)
            .collection("desafios")
            .document("ultimo_desafio")
            .collection(fechaActual)
            .document("desafio")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val completado = document.getBoolean("completado") ?: false
                    val desafioIndex = document.getLong("desafio_index")?.toInt() ?: 0
                    mostrarDesafio(desafioIndex)
                    btnCompletado.isEnabled = !completado
                } else {
                    // Generar nuevo desafío
                    generarNuevoDesafio(userId, fechaActual)
                }
            }
    }

    private fun cargarDesafioIA() {
        val userId = auth.currentUser?.uid ?: return
        val fechaActual = getPeruDate()

        // Logs de depuración
        val localTime = Calendar.getInstance().time
        val now = Timestamp.now()
        val peruCalendar = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"))
        Log.d("DEBUG_TIME_IA", "Local time: $localTime")
        Log.d("DEBUG_TIME_IA", "Firestore timestamp: $now")
        Log.d("DEBUG_TIME_IA", "Peru time: ${peruCalendar.time}")
        Log.d("DEBUG_TIME_IA", "Fecha actual (Peru): $fechaActual")
        Log.d("DEBUG_TIME_IA", "TimeZone Peru: ${peruCalendar.timeZone.id}")
        Log.d("DEBUG_TIME_IA", "TimeZone Local: ${TimeZone.getDefault().id}")

        db.collection("usuarios")
            .document(userId)
            .collection("desafios")
            .document("ultimo_desafio_ia")
            .collection(fechaActual)
            .document("desafio")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val desafioIA = DesafioIA.fromMap(document.data ?: return@addOnSuccessListener)
                    mostrarDesafioIA(desafioIA.fraseMotivadora, desafioIA.desafio)
                    btnCompletadoIA.isEnabled = !desafioIA.completado && desafioIA.desafio != "En espera"
                } else {
                    // Generar nuevo desafío automáticamente
                    generarDesafioIA()
                }
            }
    }

    private fun reiniciarDesafioIA(userId: String, fecha: String) {
        val desafioIA = DesafioIA(
            fraseMotivadora = "",
            desafio = "En espera",
            fecha = fecha,
            completado = false
        )

        db.collection("usuarios")
            .document(userId)
            .collection("desafios")
            .document("ultimo_desafio_ia")
            .collection(fecha)
            .document("desafio")
            .set(DesafioIA.toMap(desafioIA))
            .addOnSuccessListener {
                mostrarDesafioIA("", "En espera")
                btnCompletadoIA.isEnabled = false
            }
    }

    private fun mostrarDesafioIA(frase: String, desafio: String) {
        fraseMotivadoraIA.text = frase
        desafioDiarioIA.text = desafio

        // Configurar la zona horaria de Perú
        val peruTimeZone = TimeZone.getTimeZone("America/Lima")
        val calendar = Calendar.getInstance(peruTimeZone)
        
        // Establecer la hora límite a las 12:00 PM del día actual
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Formatear la fecha límite
        val dateFormat = SimpleDateFormat("'Fecha límite:' dd 'de' MMMM 'a las' hh:mm a", Locale("es", "PE"))
        dateFormat.timeZone = peruTimeZone
        val fechaLimite = dateFormat.format(calendar.time)
        
        // Mostrar la fecha límite debajo del desafío
        val textViewFechaLimite = findViewById<TextView>(R.id.textViewFechaLimite)
        textViewFechaLimite.text = fechaLimite
        textViewFechaLimite.visibility = View.VISIBLE
    }

    private fun generarDesafioIA() {
        val userId = auth.currentUser?.uid ?: return
        val peruTimeZone = TimeZone.getTimeZone("America/Lima")
        val calendar = Calendar.getInstance(peruTimeZone)
        val fechaActual = SimpleDateFormat("yyyy-MM-dd", Locale("es", "PE")).format(calendar.time)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Obtener desafíos anteriores
                val desafiosAnteriores = withContext(Dispatchers.IO) {
                    val snapshot = db.collection("usuarios")
                        .document(userId)
                        .collection("desafios")
                        .document("historial_desafios_ia")
                        .get()
                        .await()
                    
                    if (snapshot.exists()) {
                        snapshot.get("desafios") as? List<String> ?: emptyList()
                    } else {
                        emptyList()
                    }
                }

                val prompt = """Genera un desafío motivador y variado para el día de hoy. 
                    El desafío debe ser simple pero significativo para el bienestar emocional.
                    
                    REGLAS IMPORTANTES:
                    1. El desafío debe ser COMPLETAMENTE DIFERENTE a estos desafíos anteriores:
                    ${desafiosAnteriores.joinToString("\n")}
                    
                    2. NO hagas variaciones del mismo tipo de actividad.
                    3. Cada desafío debe ser único y no relacionado con otros.
                    
                    Ejemplos de desafíos diversos (NO uses estos, solo son ejemplos):
                    - "Prepara una comida saludable que nunca hayas cocinado antes"
                    - "Aprende a tocar una canción simple en un instrumento musical"
                    - "Dedica 20 minutos a dibujar o pintar algo que te inspire"
                    - "Investiga sobre un tema que te interese y comparte lo aprendido"
                    - "Organiza un espacio de tu casa y dona lo que no uses"
                    - "Practica un nuevo idioma durante 15 minutos"
                    - "Planta una semilla y cuídala durante la semana"
                    
                    IMPORTANTE: 
                    1. Responde SOLO con un objeto JSON que tenga exactamente esta estructura:
                    {
                        "frase_motivadora": "una frase corta motivadora aquí",
                        "desafio": "el desafío específico aquí"
                    }
                    2. No incluyas ningún otro texto antes o después del JSON.
                    3. Asegúrate de que el desafío sea realizable en un día.
                    4. El desafío debe ser completamente diferente a los desafíos anteriores listados."""

                val response = GPT4Service.generateResponse(prompt)
                val jsonResponse = JSONObject(response)
                
                val desafioIA = DesafioIA(
                    fraseMotivadora = jsonResponse.getString("frase_motivadora"),
                    desafio = jsonResponse.getString("desafio"),
                    fecha = fechaActual,
                    completado = false
                )

                withContext(Dispatchers.Main) {
                    mostrarDesafioIA(desafioIA.fraseMotivadora, desafioIA.desafio)
                    btnCompletadoIA.isEnabled = true

                    // Guardar en Firestore
                    db.collection("usuarios")
                        .document(userId)
                        .collection("desafios")
                        .document("ultimo_desafio_ia")
                        .collection(fechaActual)
                        .document("desafio")
                        .set(DesafioIA.toMap(desafioIA))

                    // Actualizar historial de desafíos
                    val nuevosDesafios = desafiosAnteriores.toMutableList().apply {
                        add(desafioIA.desafio)
                        // Mantener solo los últimos 10 desafíos
                        if (size > 10) removeAt(0)
                    }
                    
                    db.collection("usuarios")
                        .document(userId)
                        .collection("desafios")
                        .document("historial_desafios_ia")
                        .set(mapOf("desafios" to nuevosDesafios))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@BitacoraEmocionalActivity, 
                        "Error al generar el desafío: ${e.message}", 
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun marcarDesafioIACompletado() {
        val userId = auth.currentUser?.uid ?: return
        val fechaActual = getPeruDate()

        db.collection("usuarios")
            .document(userId)
            .collection("desafios")
            .document("ultimo_desafio_ia")
            .collection(fechaActual)
            .document("desafio")
            .update("completado", true)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Felicidades por completar tu desafío personalizado!", Toast.LENGTH_SHORT).show()
                btnCompletadoIA.isEnabled = false
            }
    }

    private fun generarNuevoDesafio(userId: String, fechaActual: String) {
        val peruTimeZone = TimeZone.getTimeZone("America/Lima")
        val calendar = Calendar.getInstance(peruTimeZone)
        val desafioIndex = (0 until DesafiosPredeterminados.lista.size).random()
        val desafio = DesafiosPredeterminados.lista[desafioIndex]

        db.collection("usuarios")
            .document(userId)
            .collection("desafios")
            .document("ultimo_desafio")
            .collection(fechaActual)
            .document("desafio")
            .set(mapOf(
                "desafio_index" to desafioIndex,
                "completado" to false,
                "fecha" to fechaActual
            ))

        mostrarDesafio(desafioIndex)
        btnCompletado.isEnabled = true
    }

    private fun mostrarDesafio(index: Int) {
        val desafio = DesafiosPredeterminados.lista[index]
        fraseMotivadora.text = desafio.fraseMotivadora
        desafioDiario.text = desafio.desafio

        // Configurar la zona horaria de Perú
        val peruTimeZone = TimeZone.getTimeZone("America/Lima")
        val calendar = Calendar.getInstance(peruTimeZone)
        
        // Establecer la hora límite a las 12:00 PM del día actual
        calendar.set(Calendar.HOUR_OF_DAY, 12)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Formatear la fecha límite
        val dateFormat = SimpleDateFormat("'Fecha límite:' dd 'de' MMMM 'a las' hh:mm a", Locale("es", "PE"))
        dateFormat.timeZone = peruTimeZone
        val fechaLimite = dateFormat.format(calendar.time)
        
        // Mostrar la fecha límite
        textViewFechaLimitePredeterminado.text = fechaLimite
        textViewFechaLimitePredeterminado.visibility = View.VISIBLE
    }

    private fun marcarDesafioCompletado() {
        val userId = auth.currentUser?.uid ?: return
        val fechaActual = getPeruDate()

        db.collection("usuarios")
            .document(userId)
            .collection("desafios")
            .document("ultimo_desafio")
            .collection(fechaActual)
            .document("desafio")
            .update("completado", true)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Felicidades por completar tu desafío!", Toast.LENGTH_SHORT).show()
                btnCompletado.isEnabled = false
            }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_emotions

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chat -> {
                    startActivity(Intent(this, ChatActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, AjustesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_emotions -> true
                else -> false
            }
        }
    }

    private fun setupCalendarioButton() {
        findViewById<Button>(R.id.btnCalendarioEmocional).setOnClickListener {
            startActivity(Intent(this, CalendarioEmocionalActivity::class.java))
        }
    }

    private fun setupResumenButtons() {
        btnResumenDesafios.setOnClickListener {
            val intent = Intent(this, ResumenDesafiosActivity::class.java).apply {
                putExtra("es_desafio_ia", false)
            }
            startActivity(intent)
        }

        btnResumenDesafiosIA.setOnClickListener {
            val intent = Intent(this, ResumenDesafiosActivity::class.java).apply {
                putExtra("es_desafio_ia", true)
            }
            startActivity(intent)
        }
    }
} 