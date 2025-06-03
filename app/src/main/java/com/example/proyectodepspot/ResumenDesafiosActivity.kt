package com.example.proyectodepspot

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ResumenDesafiosActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var textViewResumen: TextView
    private lateinit var textViewTitulo: TextView
    private var esDesafioIA: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resumen_desafios)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        textViewResumen = findViewById(R.id.textViewResumen)
        textViewTitulo = findViewById(R.id.textViewTitulo)

        esDesafioIA = intent.getBooleanExtra("es_desafio_ia", false)
        textViewTitulo.text = if (esDesafioIA) "Resumen de Desafíos Personalizados" else "Resumen de Desafíos Diarios"

        setupBottomNavigation()
        cargarResumenSemana()
    }

    private fun cargarResumenSemana() {
        val userId = auth.currentUser?.uid ?: return
        val calendar = Calendar.getInstance(Locale("es", "PE"))
        
        // Ir al lunes de la semana actual
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale("es", "PE"))
        val desafiosSemana = mutableListOf<Map<String, Any>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Recolectar desafíos de la semana
                for (i in 0..6) {
                    val fecha = dateFormat.format(calendar.time)
                    val coleccion = if (esDesafioIA) "ultimo_desafio_ia" else "ultimo_desafio"
                    
                    val document = db.collection("usuarios")
                        .document(userId)
                        .collection("desafios")
                        .document(coleccion)
                        .collection(fecha)
                        .document("desafio")
                        .get()
                        .await()

                    if (document.exists()) {
                        val datos = document.data ?: continue
                        desafiosSemana.add(datos)
                    }
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                // Generar resumen con GPT
                val resumen = generarResumenGPT(desafiosSemana)
                
                withContext(Dispatchers.Main) {
                    textViewResumen.text = resumen
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textViewResumen.text = "Error al cargar el resumen: ${e.message}"
                }
            }
        }
    }

    private suspend fun generarResumenGPT(desafios: List<Map<String, Any>>): String {
        val prompt = """Analiza los siguientes desafíos de la semana y genera un resumen ameno y motivador.
            Incluye:
            1. Un resumen general de la semana
            2. Puntos fuertes y áreas de mejora
            3. Sugerencias para la próxima semana
            
            Desafíos de la semana:
            ${formatearDesafiosParaGPT(desafios)}
            
            IMPORTANTE: 
            - Responde en un tono amigable y motivador, como si fueras un compañero.
            - Responde SOLO con el texto del resumen, sin un formato JSON ni estructura adicional.
            - NO incluyas palabras como "mensaje:", "resumen:", o cualquier otro prefijo. Solo debe ser una conversacion normal y seguida.
            - NO uses comillas ni llaves en el texto.
            - El texto debe ser limpio. 
            - Asimismo, los parrafos no deben ser tan extensos"""

        return try {
            val respuesta = GPT4Service.generateResponse(prompt)
            // Limpiar la respuesta de cualquier formato JSON o prefijos
            val textoLimpio = respuesta
                .replace(Regex("""["{}]"""), "") // Eliminar comillas y llaves
                .replace(Regex("""(mensaje|resumen|texto):\s*""", RegexOption.IGNORE_CASE), "") // Eliminar prefijos comunes
                .replace(Regex("""\s+"""), " ") // Reemplazar múltiples espacios por uno solo
                .trim() // Eliminar espacios al inicio y final
            
            textoLimpio
        } catch (e: Exception) {
            "Error al generar el resumen: ${e.message}"
        }
    }

    private fun formatearDesafiosParaGPT(desafios: List<Map<String, Any>>): String {
        return desafios.joinToString("\n") { desafio ->
            if (esDesafioIA) {
                val desafioIA = DesafioIA.fromMap(desafio)
                """
                Fecha: ${desafioIA.fecha}
                Frase: ${desafioIA.fraseMotivadora}
                Desafío: ${desafioIA.desafio}
                Completado: ${if (desafioIA.completado) "Sí" else "No"}
                """.trimIndent()
            } else {
                val fecha = desafio["fecha"] as String
                val completado = desafio["completado"] as Boolean
                val index = desafio["desafio_index"] as Long
                val desafioObj = DesafiosPredeterminados.lista[index.toInt()]
                """
                Fecha: $fecha
                Frase: ${desafioObj.fraseMotivadora}
                Desafío: ${desafioObj.desafio}
                Completado: ${if (completado) "Sí" else "No"}
                """.trimIndent()
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_emotions

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_chat -> {
                    startActivity(android.content.Intent(this, ChatActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_settings -> {
                    startActivity(android.content.Intent(this, AjustesActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_emotions -> true
                else -> false
            }
        }
    }
} 