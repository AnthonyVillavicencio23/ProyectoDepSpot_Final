package com.example.proyectodepspot

import android.os.Bundle
import android.widget.Button
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
        setupRegresarButton()
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

                android.util.Log.d("ResumenDesafios", "Desafíos recolectados: ${desafiosSemana.size}")

                // Generar resumen con GPT
                val resumen = generarResumenGPT(desafiosSemana)
                android.util.Log.d("ResumenDesafios", "Resumen generado: $resumen")
                
                withContext(Dispatchers.Main) {
                    textViewResumen.text = resumen
                    android.util.Log.d("ResumenDesafios", "Texto asignado al TextView")
                }
            } catch (e: Exception) {
                android.util.Log.e("ResumenDesafios", "Error en cargarResumenSemana: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    textViewResumen.text = "Error al cargar el resumen: ${e.message}"
                }
            }
        }
    }

    private suspend fun generarResumenGPT(desafios: List<Map<String, Any>>): String {
        val prompt = """Analiza los siguientes desafíos de la semana y genera un resumen ameno y motivador.
            Incluye:
            1. Un resumen general de la semana (máximo 4 líneas)
            2. Puntos fuertes y áreas de mejora (máximo 4 líneas)
            3. Sugerencias para la próxima semana (máximo 4 líneas)
            
            Desafíos de la semana:
            ${formatearDesafiosParaGPT(desafios)}
            
            IMPORTANTE: 
            - Responde en un tono amigable y motivador, como si fueras un compañero.
            - Estructura tu respuesta en 3 párrafos separados, cada uno con su etiqueta:
              Parrafo1: [aquí el resumen general de la semana]
              Parrafo2: [aquí los puntos fuertes y áreas de mejora]
              Parrafo3: [aquí las sugerencias para la próxima semana]
            - Cada párrafo debe ser conciso y directo, máximo 4 líneas.
            - NO uses comillas ni llaves en el texto.
            - El objeto JSON debe tener el nombre "resumen"."""

        return try {
            val respuesta = GPT4Service.generateResponse(prompt)
            android.util.Log.d("ResumenDesafios", "Respuesta GPT: $respuesta")
            
            // Parsear el JSON anidado
            val jsonObject = JSONObject(respuesta)
            
            // Intentar obtener el objeto con diferentes nombres posibles
            val respuestaObj = when {
                jsonObject.has("resumen") -> jsonObject.getJSONObject("resumen")
                jsonObject.has("respuesta") -> jsonObject.getJSONObject("respuesta")
                jsonObject.has("resultado") -> jsonObject.getJSONObject("resultado")
                else -> jsonObject // Si no encuentra ninguno, usar el objeto raíz
            }
            
            val parrafo1 = respuestaObj.optString("Parrafo1", "").trim()
            val parrafo2 = respuestaObj.optString("Parrafo2", "").trim()
            val parrafo3 = respuestaObj.optString("Parrafo3", "").trim()
            
            android.util.Log.d("ResumenDesafios", "Párrafo 1: $parrafo1")
            android.util.Log.d("ResumenDesafios", "Párrafo 2: $parrafo2")
            android.util.Log.d("ResumenDesafios", "Párrafo 3: $parrafo3")
            
            // Combinar los párrafos con doble salto de línea entre ellos
            val textoFinal = "$parrafo1\n\n$parrafo2\n\n$parrafo3"
            android.util.Log.d("ResumenDesafios", "Texto final: $textoFinal")
            
            textoFinal
        } catch (e: Exception) {
            android.util.Log.e("ResumenDesafios", "Error: ${e.message}", e)
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

    private fun setupRegresarButton() {
        findViewById<Button>(R.id.btnRegresar).setOnClickListener {
            finish()
        }
    }
} 