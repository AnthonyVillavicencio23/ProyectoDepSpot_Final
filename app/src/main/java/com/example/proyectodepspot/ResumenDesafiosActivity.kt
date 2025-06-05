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
import java.util.TimeZone

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

    private fun getPeruDate(): String {
        val peruTimeZone = TimeZone.getTimeZone("America/Lima")
        val calendar = Calendar.getInstance()
        calendar.timeZone = peruTimeZone
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale("es", "PE"))
        dateFormat.timeZone = peruTimeZone
        return dateFormat.format(calendar.time)
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
                            desafiosPredeterminados.add(datos)
                            android.util.Log.d("ResumenDesafios", "Desafío predeterminado encontrado para fecha: $fecha")
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
                            desafiosIA.add(datos)
                            android.util.Log.d("ResumenDesafios", "Desafío IA encontrado para fecha: $fecha")
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

                // Función para verificar si un par ha pasado su fecha límite
                fun verificarParCompletado(par: List<Map<String, Any>>): Boolean {
                    if (par.size < 2) return false
                    
                    // Verificar cada desafío individualmente
                    val resultados = par.map { desafio ->
                        val fechaDesafio = dateFormat.parse(desafio["fecha"] as String)
                        val fechaLimite = Calendar.getInstance(peruTimeZone).apply {
                            time = fechaDesafio
                            set(Calendar.HOUR_OF_DAY, 12)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }.time
                        
                        val haPasadoFechaLimite = fechaActual.after(fechaLimite)
                        
                        android.util.Log.d("ResumenDesafios", "Verificando desafío individual - Fecha: ${dateFormat.format(fechaDesafio)}")
                        android.util.Log.d("ResumenDesafios", "Verificando desafío individual - Límite: ${dateFormat.format(fechaLimite)}")
                        android.util.Log.d("ResumenDesafios", "Verificando desafío individual - Actual: ${dateFormat.format(fechaActual)}")
                        android.util.Log.d("ResumenDesafios", "Verificando desafío individual - ¿Pasó límite?: $haPasadoFechaLimite")
                        
                        haPasadoFechaLimite
                    }
                    
                    // El par está completo si todos los desafíos han pasado su fecha límite
                    val parCompletado = resultados.all { it }
                    android.util.Log.d("ResumenDesafios", "Resultado final del par: $parCompletado")
                    
                    return parCompletado
                }

                // Buscar el par más reciente que haya completado su fecha límite
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
                            withContext(Dispatchers.Main) {
                                textViewResumen.text = resumenTexto
                            }
                            return@launch
                        }
                    }

                    withContext(Dispatchers.Main) {
                        textViewResumen.text = "No hay pares de desafíos que hayan completado su fecha límite. Por favor, espera hasta que un par de desafíos haya expirado para ver el resumen."
                    }
                    return@launch
                }

                android.util.Log.d("ResumenDesafios", "Desafíos del par completado: ${parCompletado.size}")

                // Generar resumen con GPT
                val resumen = generarResumenGPT(parCompletado)
                android.util.Log.d("ResumenDesafios", "Resumen generado: $resumen")

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
        val prompt = """Analiza los siguientes desafíos de los últimos 2 días y genera un resumen ameno y motivador.
            Incluye:
            1. Un resumen general del período. Menciona los desafios claramente no importa si se completaron (máximo 4 líneas)
            2. Puntos fuertes y áreas de mejora (máximo 3 líneas)
            3. Sugerencias para los próximos días (máximo 3 líneas)
            
            Desafíos del período:
            ${formatearDesafiosParaGPT(desafios)}
            
            IMPORTANTE: 
            - Responde en un tono amigable y motivador, como si fueras un compañero.
            - Estructura tu respuesta en 3 párrafos separados, cada uno con su etiqueta:
              Parrafo1: [aquí el resumen general del período]
              Parrafo2: [aquí los puntos fuertes y áreas de mejora]
              Parrafo3: [aquí las sugerencias para los próximos días]
            - Cada párrafo debe ser conciso y directo, máximo 3 líneas.
            - NO uses comillas ni llaves en el texto
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