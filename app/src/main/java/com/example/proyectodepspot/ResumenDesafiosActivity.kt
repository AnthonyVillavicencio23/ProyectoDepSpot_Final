package com.example.proyectodepspot

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.appbar.MaterialToolbar
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
import java.util.TimeZone

class ResumenDesafiosActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var textViewResumen: TextView
    private lateinit var textViewTitulo: TextView
    private lateinit var textViewFechasResumen: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnActualizar: Button
    private var esDesafioIA: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_resumen_desafios)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        textViewResumen = findViewById(R.id.textViewResumen)
        textViewTitulo = findViewById(R.id.textViewTitulo)
        textViewFechasResumen = findViewById(R.id.textViewFechasResumen)
        progressBar = findViewById(R.id.progressBar)
        btnActualizar = findViewById(R.id.btnActualizar)

        esDesafioIA = intent.getBooleanExtra("es_desafio_ia", false)
        textViewTitulo.text = if (esDesafioIA) "Resumen de Desafíos Personalizados" else "Resumen de Desafíos Diarios"

        // Configurar la barra superior
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupBottomNavigation()
        setupActualizarButton()
        cargarResumenSemana()
    }

    private fun setupActualizarButton() {
        btnActualizar.setOnClickListener {
            btnActualizar.visibility = View.GONE
            textViewFechasResumen.visibility = View.GONE
            textViewResumen.text = "Cargando resumen..."
            progressBar.visibility = View.VISIBLE
            cargarResumenSemana()
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

    private fun formatearFechasParaResumen(fechas: List<String>): String {
        if (fechas.isEmpty()) return ""
        
        val peruTimeZone = TimeZone.getTimeZone("America/Lima")
        val dateFormatInput = SimpleDateFormat("yyyy-MM-dd", Locale("es", "PE"))
        dateFormatInput.timeZone = peruTimeZone
        val dateFormatOutput = SimpleDateFormat("dd 'de' MMMM", Locale("es", "PE"))
        dateFormatOutput.timeZone = peruTimeZone
        
        return try {
            val fechasFormateadas = fechas.map { fecha ->
                val date = dateFormatInput.parse(fecha)
                dateFormatOutput.format(date ?: return "")
            }
            
            if (fechasFormateadas.size == 2) {
                "📅 Resumen correspondiente a los días ${fechasFormateadas[0]} y ${fechasFormateadas[1]}"
            } else {
                "📅 Resumen correspondiente a los días: ${fechasFormateadas.joinToString(", ")}"
            }
        } catch (e: Exception) {
            "📅 Resumen correspondiente a los días: ${fechas.joinToString(", ")}"
        }
    }

    private fun cargarResumenSemana() {
        val userId = auth.currentUser?.uid ?: return
        val peruTimeZone = TimeZone.getTimeZone("America/Lima")
        val calendar = Calendar.getInstance(peruTimeZone)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale("es", "PE"))
        dateFormat.timeZone = peruTimeZone
        
        // Obtener la fecha actual en Perú
        val fechaActualStr = getPeruDate()
        val fechaActual = dateFormat.parse(fechaActualStr)
        
        // Retroceder 30 días para obtener un historial más amplio
        calendar.time = fechaActual
        calendar.add(Calendar.DAY_OF_MONTH, -30)
        val fechaInicio = calendar.time
        
        val desafiosPredeterminados = mutableListOf<Map<String, Any>>()
        val desafiosIA = mutableListOf<Map<String, Any>>()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Recolectar todos los desafíos del período
                calendar.time = fechaInicio
                while (!calendar.time.after(fechaActual)) {
                    val fecha = dateFormat.format(calendar.time)
                    val esDiaActual = fecha == fechaActualStr
                    
                    // Obtener desafío predeterminado
                    val docPredeterminado = db.collection("usuarios")
                        .document(userId)
                        .collection("desafios")
                        .document("ultimo_desafio")
                        .collection(fecha)
                        .document("desafio")
                        .get()
                        .await()

                    if (docPredeterminado.exists()) {
                        val datos = docPredeterminado.data ?: null
                        if (datos != null) {
                            val completado = datos["completado"] as? Boolean ?: false
                            // Si es el día actual y no está completado, no se incluye para verificación de pares
                            // Si es un día anterior o está completado, se incluye
                            if (!esDiaActual || completado) {
                                desafiosPredeterminados.add(datos)
                                android.util.Log.d("ResumenDesafios", "Desafío predeterminado encontrado para fecha: $fecha, completado: $completado")
                            } else {
                                android.util.Log.d("ResumenDesafios", "Desafío predeterminado del día actual no completado, omitido: $fecha")
                            }
                        }
                    }

                    // Obtener desafío IA
                    val docIA = db.collection("usuarios")
                        .document(userId)
                        .collection("desafios")
                        .document("ultimo_desafio_ia")
                        .collection(fecha)
                        .document("desafio")
                        .get()
                        .await()

                    if (docIA.exists()) {
                        val datos = docIA.data ?: null
                        if (datos != null) {
                            val completado = datos["completado"] as? Boolean ?: false
                            // Si es el día actual y no está completado, no se incluye para verificación de pares
                            // Si es un día anterior o está completado, se incluye
                            if (!esDiaActual || completado) {
                                desafiosIA.add(datos)
                                android.util.Log.d("ResumenDesafios", "Desafío IA encontrado para fecha: $fecha, completado: $completado")
                            } else {
                                android.util.Log.d("ResumenDesafios", "Desafío IA del día actual no completado, omitido: $fecha")
                            }
                        }
                    }

                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }

                // Ordenar los desafíos por fecha (más recientes primero)
                desafiosPredeterminados.sortByDescending { it["fecha"] as String }
                desafiosIA.sortByDescending { it["fecha"] as String }

                android.util.Log.d("ResumenDesafios", "Total desafíos predeterminados: ${desafiosPredeterminados.size}")
                android.util.Log.d("ResumenDesafios", "Total desafíos IA: ${desafiosIA.size}")

                // Agrupar en pares (manteniendo el orden 1-2, 3-4, etc.) y filtrar solo los pares completos
                val paresPredeterminados = desafiosPredeterminados.reversed()
                    .chunked(2)
                    .filter { it.size == 2 }
                val paresIA = desafiosIA.reversed()
                    .chunked(2)
                    .filter { it.size == 2 }

                android.util.Log.d("ResumenDesafios", "Pares predeterminados completos: ${paresPredeterminados.size}")
                android.util.Log.d("ResumenDesafios", "Pares IA completos: ${paresIA.size}")

                // Función para verificar si un par es válido para generar resumen
                // Si ambos desafíos están en la lista, significa que ya pasaron su día (o están completados si son del día actual)
                // Por lo tanto, el par es válido independientemente de si están completados o no
                fun verificarParCompletado(par: List<Map<String, Any>>): Boolean {
                    if (par.size < 2) return false
                    
                    // Si ambos desafíos están en la lista, ya pasaron el filtro de recolección
                    // Esto significa que: son días anteriores (válidos) O son del día actual pero completados
                    // Por lo tanto, el par es válido para generar resumen
                    val fechas = par.map { desafio ->
                        val completado = desafio["completado"] as? Boolean ?: false
                        val fecha = desafio["fecha"] as String
                        
                        android.util.Log.d("ResumenDesafios", "Verificando desafío individual - Fecha: $fecha, Completado: $completado")
                        
                        fecha
                    }
                    
                    android.util.Log.d("ResumenDesafios", "Par válido con fechas: $fechas")
                    
                    // El par es válido porque ambos desafíos ya pasaron el filtro de recolección
                    return true
                }

                // Buscar el par más reciente que sea válido para generar resumen
                val paresARevisar = if (esDesafioIA) paresIA else paresPredeterminados
                var parCompletado: List<Map<String, Any>>? = null

                // Obtener el último resumen guardado
                val ultimoResumen = db.collection("usuarios")
                    .document(userId)
                    .collection("resumenes")
                    .document(if (esDesafioIA) "ultimo_resumen_ia" else "ultimo_resumen_predeterminado")
                    .get()
                    .await()

                var ultimoParFechas: List<String>? = null
                if (ultimoResumen.exists()) {
                    val resumen = ultimoResumen.data
                    if (resumen != null) {
                        ultimoParFechas = resumen["fechas"] as? List<String>
                    }
                }

                // Revisar los pares del más reciente al más antiguo
                for (par in paresARevisar.reversed()) {
                    android.util.Log.d("ResumenDesafios", "Revisando par con fechas: ${par.map { it["fecha"] }}")
                    
                    // Si hay un último resumen, verificar si este par es posterior
                    if (ultimoParFechas != null) {
                        val fechaUltimoPar = dateFormat.parse(ultimoParFechas.maxByOrNull { it })
                        val fechaParActual = dateFormat.parse((par.map { it["fecha"] as String }).maxByOrNull { it })
                        
                        if (fechaParActual.before(fechaUltimoPar) || fechaParActual.equals(fechaUltimoPar)) {
                            android.util.Log.d("ResumenDesafios", "Este par es anterior o igual al último resumen guardado")
                            continue
                        }
                    }

                    if (verificarParCompletado(par)) {
                        parCompletado = par
                        android.util.Log.d("ResumenDesafios", "Par encontrado con fechas: ${par.map { it["fecha"] }}")
                        break
                    }
                }

                if (parCompletado == null) {
                    // Si no se encontró un par nuevo, mostrar el último resumen guardado
                    if (ultimoResumen.exists()) {
                        val resumen = ultimoResumen.data
                        if (resumen != null) {
                            val resumenTexto = resumen["resumen"] as? String ?: ""
                            val fechas = resumen["fechas"] as? List<String> ?: emptyList()
                            withContext(Dispatchers.Main) {
                                if (fechas.isNotEmpty()) {
                                    textViewFechasResumen.text = formatearFechasParaResumen(fechas)
                                    textViewFechasResumen.visibility = View.VISIBLE
                                } else {
                                    textViewFechasResumen.visibility = View.GONE
                                }
                                textViewResumen.text = resumenTexto
                                progressBar.visibility = View.GONE
                            }
                            return@launch
                        }
                    }

                    withContext(Dispatchers.Main) {
                        textViewFechasResumen.visibility = View.GONE
                        textViewResumen.text = "No hay pares de desafíos completados disponibles. Completa dos desafíos consecutivos para ver tu resumen."
                        progressBar.visibility = View.GONE
                    }
                    return@launch
                }

                android.util.Log.d("ResumenDesafios", "Desafíos del par completado: ${parCompletado.size}")

                // Generar resumen con GPT
                val resumen = try {
                    generarResumenGPT(parCompletado)
                } catch (e: Exception) {
                    android.util.Log.e("ResumenDesafios", "Error al generar resumen: ${e.message}")
                    if (e.message?.contains("429") == true) {
                        withContext(Dispatchers.Main) {
                            textViewFechasResumen.visibility = View.GONE
                            textViewResumen.text = "Lo sentimos, hemos alcanzado el límite de solicitudes. Por favor, intenta nuevamente en unos minutos."
                            progressBar.visibility = View.GONE
                            btnActualizar.visibility = View.VISIBLE
                        }
                        return@launch
                    } else {
                        withContext(Dispatchers.Main) {
                            textViewFechasResumen.visibility = View.GONE
                            textViewResumen.text = "Lo sentimos, hubo un error al generar el resumen. Por favor, intenta nuevamente más tarde."
                            progressBar.visibility = View.GONE
                            btnActualizar.visibility = View.VISIBLE
                        }
                        return@launch
                    }
                }

                android.util.Log.d("ResumenDesafios", "Resumen generado exitosamente")

                // Guardar el resumen generado con las fechas del par
                val fechasPar = parCompletado.map { it["fecha"] as String }
                val resumenData = hashMapOf(
                    "resumen" to resumen,
                    "fechas" to fechasPar,
                    "fecha_generacion" to getPeruDate()
                )

                db.collection("usuarios")
                    .document(userId)
                    .collection("resumenes")
                    .document(if (esDesafioIA) "ultimo_resumen_ia" else "ultimo_resumen_predeterminado")
                    .set(resumenData)
                    .await()

                withContext(Dispatchers.Main) {
                    val fechasPar = parCompletado.map { it["fecha"] as String }
                    textViewFechasResumen.text = formatearFechasParaResumen(fechasPar)
                    textViewFechasResumen.visibility = View.VISIBLE
                    textViewResumen.text = resumen
                    progressBar.visibility = View.GONE
                    android.util.Log.d("ResumenDesafios", "Texto asignado al TextView")
                }
            } catch (e: Exception) {
                android.util.Log.e("ResumenDesafios", "Error en cargarResumenSemana: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    textViewFechasResumen.visibility = View.GONE
                    textViewResumen.text = "Error al cargar el resumen: ${e.message}"
                    progressBar.visibility = View.GONE
                }
            }
        }
    }

    private suspend fun generarResumenGPT(desafios: List<Map<String, Any>>): String {
        val prompt = """Analiza estos desafíos de los últimos 2 días:\n${formatearDesafiosParaGPT(desafios)}\n\nHabla como un compañero amistoso y genera un resumen motivador para el adolescente dividido en 3 partes:\nParrafo1: resumen general (menciona los desafíos claramente, aun si no los completo el usuario, máx 4 líneas)\nParrafo2: Rescata los puntos fuertes y mejoras sobre los desafios al usuario (Sea que los hayan completado o no,máx 3 líneas)\nParrafo3: Sugerencias de apoyo para aplicar en los proximos desafios (Aún si completaron los desafios o no,máx 3 líneas)\n\nReglas:\n- Tono amigable y de compañero\n- Sin comillas ni llaves en el texto\n- Dirigete siempre al usuario como si hablaras con él\n- Estructura el JSON así: {\"resumen\":{\"Parrafo1\":\"...\",\"Parrafo2\":\"...\",\"Parrafo3\":\"...\"}}"""

        return try {
            val respuesta = GPT4Service.generateResponse(prompt)
            android.util.Log.d("ResumenDesafios", "Respuesta GPT: $respuesta")
            
            // Parsear el JSON anidado
            val resumenJson = JSONObject(respuesta)
            val resumen = resumenJson.getJSONObject("resumen")
            
            val parrafo1 = resumen.getString("Parrafo1")
            val parrafo2 = resumen.getString("Parrafo2")
            val parrafo3 = resumen.getString("Parrafo3")
            
            android.util.Log.d("ResumenDesafios", "Párrafo 1: $parrafo1")
            android.util.Log.d("ResumenDesafios", "Párrafo 2: $parrafo2")
            android.util.Log.d("ResumenDesafios", "Párrafo 3: $parrafo3")
            
            // Combinar los párrafos con doble salto de línea entre ellos
            val textoFinal = "$parrafo1\n\n$parrafo2\n\n$parrafo3"
            android.util.Log.d("ResumenDesafios", "Texto final: $textoFinal")
            
            textoFinal
        } catch (e: Exception) {
            android.util.Log.e("ResumenDesafios", "Error: ${e.message}", e)
            throw e // Re-lanzamos la excepción para manejarla en el nivel superior
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 