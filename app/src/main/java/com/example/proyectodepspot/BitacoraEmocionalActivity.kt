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

    private val desafios = listOf(
        Desafio(
            "Cada día es una nueva oportunidad para crecer",
            "Lee cinco páginas del libro que te gusta y subraya un párrafo que te impacte"
        ),
        Desafio(
            "Pequeños pasos llevan a grandes cambios",
            "Dedica 10 minutos a meditar y enfócate en tu respiración"
        ),
        Desafio(
            "Tu bienestar es una prioridad",
            "Escribe tres cosas por las que estés agradecido hoy"
        ),
        Desafio(
            "La constancia es la clave del éxito",
            "Realiza 15 minutos de ejercicio físico que disfrutes"
        ),
        Desafio(
            "Cada momento es una oportunidad para ser mejor",
            "Practica un acto de bondad con alguien cercano"
        ),
        Desafio(
            "El autocuidado es fundamental",
            "Toma un momento para disfrutar de tu bebida favorita con calma"
        )
    )

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

            setupBottomNavigation()
            cargarDesafioDiario()
            cargarDesafioIA()
            setupBotones()
            setupCalendarioButton()
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

    private fun cargarDesafioDiario() {
        val userId = auth.currentUser?.uid ?: return
        val hoy = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        db.collection("usuarios")
            .document(userId)
            .collection("desafios")
            .document("ultimo_desafio")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val ultimoDia = document.getLong("dia") ?: 0
                    val completado = document.getBoolean("completado") ?: false
                    
                    if (ultimoDia == hoy.toLong()) {
                        // Mostrar el desafío del día
                        val desafioIndex = document.getLong("desafio_index")?.toInt() ?: 0
                        mostrarDesafio(desafioIndex)
                        // Actualizar estado del botón
                        btnCompletado.isEnabled = !completado
                    } else {
                        // Generar nuevo desafío
                        generarNuevoDesafio(userId, hoy)
                    }
                } else {
                    // Primera vez, generar desafío
                    generarNuevoDesafio(userId, hoy)
                }
            }
    }

    private fun cargarDesafioIA() {
        val userId = auth.currentUser?.uid ?: return
        val hoy = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)

        db.collection("usuarios")
            .document(userId)
            .collection("desafios")
            .document("ultimo_desafio_ia")
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val ultimoDia = document.getLong("dia") ?: 0
                    val completado = document.getBoolean("completado") ?: false
                    
                    if (ultimoDia == hoy.toLong()) {
                        // Mostrar el desafío del día
                        val frase = document.getString("frase_motivadora") ?: ""
                        val desafio = document.getString("desafio") ?: "En espera"
                        mostrarDesafioIA(frase, desafio)
                        btnCompletadoIA.isEnabled = !completado && desafio != "En espera"
                    } else {
                        // Reiniciar para nuevo día
                        reiniciarDesafioIA(userId, hoy)
                    }
                } else {
                    // Primera vez
                    reiniciarDesafioIA(userId, hoy)
                }
            }
    }

    private fun reiniciarDesafioIA(userId: String, dia: Int) {
        db.collection("usuarios")
            .document(userId)
            .collection("desafios")
            .document("ultimo_desafio_ia")
            .set(mapOf(
                "dia" to dia,
                "frase_motivadora" to "",
                "desafio" to "En espera",
                "completado" to false
            ))
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
        btnGenerarDesafio.isEnabled = false
        btnGenerarDesafio.text = "Generando..."

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
                
                val frase = jsonResponse.getString("frase_motivadora")
                val desafio = jsonResponse.getString("desafio")

                withContext(Dispatchers.Main) {
                    mostrarDesafioIA(frase, desafio)
                    btnCompletadoIA.isEnabled = true
                    btnGenerarDesafio.isEnabled = true
                    btnGenerarDesafio.text = "Generar Desafío"

                    // Guardar en Firestore
                    val updates = hashMapOf<String, Any>(
                        "frase_motivadora" to frase,
                        "desafio" to desafio
                    )
                    db.collection("usuarios")
                        .document(userId)
                        .collection("desafios")
                        .document("ultimo_desafio_ia")
                        .update(updates)

                    // Actualizar historial de desafíos
                    val nuevosDesafios = desafiosAnteriores.toMutableList().apply {
                        add(desafio)
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
                    btnGenerarDesafio.isEnabled = true
                    btnGenerarDesafio.text = "Generar Desafío"
                }
            }
        }
    }

    private fun marcarDesafioIACompletado() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("usuarios")
            .document(userId)
            .collection("desafios")
            .document("ultimo_desafio_ia")
            .update("completado", true)
            .addOnSuccessListener {
                Toast.makeText(this, "¡Felicidades por completar tu desafío personalizado!", Toast.LENGTH_SHORT).show()
                btnCompletadoIA.isEnabled = false
            }
    }

    private fun generarNuevoDesafio(userId: String, dia: Int) {
        val desafioIndex = (0 until desafios.size).random()
        val desafio = desafios[desafioIndex]

        db.collection("usuarios")
            .document(userId)
            .collection("desafios")
            .document("ultimo_desafio")
            .set(mapOf(
                "dia" to dia,
                "desafio_index" to desafioIndex,
                "completado" to false
            ))
            .addOnSuccessListener {
                mostrarDesafio(desafioIndex)
                btnCompletado.isEnabled = true
            }
    }

    private fun mostrarDesafio(index: Int) {
        val desafio = desafios[index]
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
        db.collection("usuarios")
            .document(userId)
            .collection("desafios")
            .document("ultimo_desafio")
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
}

data class Desafio(
    val fraseMotivadora: String,
    val desafio: String
) 