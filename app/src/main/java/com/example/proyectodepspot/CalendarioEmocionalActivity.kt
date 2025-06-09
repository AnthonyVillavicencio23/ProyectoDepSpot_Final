package com.example.proyectodepspot

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.proyectodepspot.data.FirebaseChatRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import com.google.android.material.appbar.MaterialToolbar
import android.widget.ImageView
import android.widget.LinearLayout
import android.view.ViewOutlineProvider

class CalendarioEmocionalActivity : AppCompatActivity() {
    private lateinit var gridEmociones: GridLayout
    private lateinit var gridEmocionesSeleccionables: GridLayout
    private lateinit var textoSeleccionEmocion: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var chatRepository: FirebaseChatRepository
    private lateinit var textoRangoSemana: TextView
    private var semanaActual: Calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"), Locale("es", "PE"))
    private val emociones = mapOf(
        "feliz" to R.drawable.cara_feliz,
        "triste" to R.drawable.cara_triste,
        "depresivo" to R.drawable.cara_depresiva,
        "enojado" to R.drawable.cara_enojada,
        "cansado" to R.drawable.cara_cansada
    )
    private val emojiNeutral = R.drawable.cara_neutra

    private val emocionesEspecificas = mapOf(
        "feliz" to listOf("Extasiado", "Emocionado", "Alegre", "Contento"),
        "triste" to listOf("Melancólico", "Desanimado", "Afligido", "Desconsolado"),
        "depresivo" to listOf("Desesperanzado", "Vacío", "Aislado", "Desmotivado"),
        "enojado" to listOf("Frustrado", "Irritado", "Molesto", "Indignado"),
        "cansado" to listOf("Agotado", "Exhausto", "Fatigado", "Drenado")
    )

    private val contextos = listOf("Estudio", "Trabajo", "Familia", "Amigos", "Pareja", "Emergencia")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendario_emocional)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        chatRepository = FirebaseChatRepository(this)
        gridEmociones = findViewById(R.id.gridEmociones)
        gridEmocionesSeleccionables = findViewById(R.id.gridEmocionesSeleccionables)
        textoSeleccionEmocion = findViewById(R.id.textoSeleccionEmocion)
        textoRangoSemana = findViewById(R.id.textoRangoSemana)

        // Configurar la barra superior
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        setupBottomNavigation()
        setupNavegacionSemana()
        setupGridEmocionesSeleccionables()
        actualizarVistaSemana()
    }

    private fun setupNavegacionSemana() {
        findViewById<ImageButton>(R.id.btnSemanaAnterior).setOnClickListener {
            semanaActual.add(Calendar.WEEK_OF_YEAR, -1)
            actualizarVistaSemana()
        }

        findViewById<ImageButton>(R.id.btnSemanaSiguiente).setOnClickListener {
            semanaActual.add(Calendar.WEEK_OF_YEAR, 1)
            actualizarVistaSemana()
        }
    }

    private fun setupGridEmocionesSeleccionables() {
        emociones.forEach { (emocion, imagenResId) ->
            // Crear un contenedor vertical para la imagen y el texto
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
            }

            // Crear el texto de la emoción
            val textView = TextView(this).apply {
                text = emocion.capitalize()
                textSize = 14f
                setTextColor(resources.getColor(R.color.colorPrimary, theme))
                gravity = Gravity.CENTER
                alpha = 1.0f
                setPadding(0, 0, 0, 8)
            }

            // Crear la imagen con fondo circular
            val imageView = ImageView(this).apply {
                setImageResource(imagenResId)
                scaleType = ImageView.ScaleType.FIT_CENTER
                adjustViewBounds = true
                maxHeight = resources.getDimensionPixelSize(R.dimen.emotion_image_size)
                maxWidth = resources.getDimensionPixelSize(R.dimen.emotion_image_size)
                setPadding(2, 2, 2, 2)
                background = getDrawable(R.drawable.bg_emotion_circle)
                clipToOutline = true
                outlineProvider = ViewOutlineProvider.BACKGROUND
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    if (isEnabled) {
                        actualizarEmocionHoy(emocion)
                    }
                }
            }

            // Agregar la imagen y el texto al contenedor
            container.addView(textView)
            container.addView(imageView)

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }

            gridEmocionesSeleccionables.addView(container, params)
        }
    }

    private fun actualizarVistaSemana() {
        // Actualizar el texto del rango de la semana
        val dateFormat = SimpleDateFormat("dd.MM", Locale("es", "PE"))
        dateFormat.timeZone = TimeZone.getTimeZone("America/Lima")
        val calendar = semanaActual.clone() as Calendar
        
        // Ir al lunes de la semana
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        val inicioSemana = dateFormat.format(calendar.time)
        
        // Ir al domingo de la semana
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        val finSemana = dateFormat.format(calendar.time)
        
        // Mostrar el rango de la semana
        textoRangoSemana.text = "$inicioSemana - $finSemana ${calendar.get(Calendar.YEAR)}"

        // Verificar si estamos en la semana actual
        val esSemanaActual = esMismaSemana(Calendar.getInstance(TimeZone.getTimeZone("America/Lima"), Locale("es", "PE")), semanaActual)
        
        // Actualizar las fechas y emociones
        setupFechas()
        setupGridEmociones(esSemanaActual)
        actualizarVisibilidadSeleccionEmocion(esSemanaActual)
        cargarEmocionesGuardadas()
    }

    private fun setupFechas() {
        val calendar = semanaActual.clone() as Calendar
        val dateFormat = SimpleDateFormat("dd/MM", Locale("es", "PE"))
        dateFormat.timeZone = TimeZone.getTimeZone("America/Lima")

        // Ajustar al lunes de la semana
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        // Asignar fechas a cada día
        for (i in 0..6) {
            val fechaTextView = findViewById<TextView>(resources.getIdentifier(
                "fecha${getDiaSemana(i)}",
                "id",
                packageName
            ))
            fechaTextView.text = dateFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
    }

    private fun getDiaSemana(index: Int): String {
        return when (index) {
            0 -> "Lun"
            1 -> "Mar"
            2 -> "Mie"
            3 -> "Jue"
            4 -> "Vie"
            5 -> "Sab"
            6 -> "Dom"
            else -> ""
        }
    }

    private fun setupGridEmociones(esSemanaActual: Boolean) {
        // Limpiar el grid de emojis (mantener solo los días y fechas)
        while (gridEmociones.childCount > 14) { // 14 = 7 días + 7 fechas
            gridEmociones.removeViewAt(14)
        }

        // Obtener el día actual (1-7, donde 1 es lunes)
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"), Locale("es", "PE"))
        val diaActual = calendar.get(Calendar.DAY_OF_WEEK)
        val diaSemanaActual = if (diaActual == Calendar.SUNDAY) 7 else diaActual - 1

        // Agregar las imágenes al grid
        for (i in 1..7) {
            // Crear un contenedor para la imagen
            val container = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(4, 4, 4, 4)
            }

            // Crear la imagen
            val imageView = ImageView(this).apply {
                setImageResource(emojiNeutral)
                scaleType = ImageView.ScaleType.CENTER_CROP
                adjustViewBounds = true
                maxHeight = resources.getDimensionPixelSize(R.dimen.emotion_image_size)
                maxWidth = resources.getDimensionPixelSize(R.dimen.emotion_image_size)
                setPadding(2, 2, 2, 2)
                background = getDrawable(R.drawable.bg_emotion_circle)
                clipToOutline = true
                outlineProvider = ViewOutlineProvider.BACKGROUND
                alpha = 1.0f
                isEnabled = true
            }

            // Agregar la imagen al contenedor
            container.addView(imageView)

            // Solo agregar el fondo si estamos en la semana actual y es el día actual
            if (esSemanaActual && i == diaSemanaActual) {
                container.setBackgroundResource(R.drawable.bg_dia_actual)
            }

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }

            gridEmociones.addView(container, params)
        }
    }

    private fun actualizarVisibilidadSeleccionEmocion(esSemanaActual: Boolean) {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"), Locale("es", "PE"))
        val esHoy = esMismaSemana(calendar, semanaActual) && 
                   calendar.get(Calendar.DAY_OF_WEEK) == semanaActual.get(Calendar.DAY_OF_WEEK)

        if (esSemanaActual && esHoy) {
            textoSeleccionEmocion.text = "¿Cómo te sientes en este momento?"
            textoSeleccionEmocion.alpha = 1.0f
            gridEmocionesSeleccionables.alpha = 1.0f
            // Habilitar todos los contenedores
            for (i in 0 until gridEmocionesSeleccionables.childCount) {
                val container = gridEmocionesSeleccionables.getChildAt(i) as LinearLayout
                container.isEnabled = true
                container.alpha = 1.0f
                // Habilitar la imagen dentro del contenedor
                val imageView = container.getChildAt(1) as ImageView
                imageView.isEnabled = true
                imageView.alpha = 1.0f
            }
        } else {
            textoSeleccionEmocion.text = "Navega a la semana actual para seleccionar una emoción"
            textoSeleccionEmocion.alpha = 0.7f
            gridEmocionesSeleccionables.alpha = 1.0f
            // Deshabilitar todos los contenedores
            for (i in 0 until gridEmocionesSeleccionables.childCount) {
                val container = gridEmocionesSeleccionables.getChildAt(i) as LinearLayout
                container.isEnabled = false
                container.alpha = 0.5f
                // Deshabilitar la imagen dentro del contenedor
                val imageView = container.getChildAt(1) as ImageView
                imageView.isEnabled = false
                imageView.alpha = 0.5f
            }
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

    private fun actualizarEmocionHoy(emocion: String) {
        val userId = auth.currentUser?.uid ?: return
        val fecha = getPeruDate()
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("America/Lima"), Locale("es", "PE"))

        // Solo permitir actualizar si estamos en la semana actual
        if (esMismaSemana(calendar, semanaActual)) {
            val diaActual = calendar.get(Calendar.DAY_OF_WEEK)
            val diaSemanaActual = if (diaActual == Calendar.SUNDAY) 7 else diaActual - 1

            // Mostrar primer pop-up con emociones específicas
            mostrarPopupEmocionEspecifica(emocion, userId, fecha, diaSemanaActual)
        } else {
            Toast.makeText(this, "Solo puedes seleccionar emociones para la semana actual", Toast.LENGTH_SHORT).show()
        }
    }

    private fun mostrarPopupEmocionEspecifica(emocion: String, userId: String, fecha: String, diaSemanaActual: Int) {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val dialogView = layoutInflater.inflate(R.layout.dialog_emocion_especifica, null)
        val gridLayout = dialogView.findViewById<GridLayout>(R.id.gridEmocionesEspecificas)
        
        val emocionesEspecificasList = emocionesEspecificas[emocion] ?: return
        
        // Limpiar el grid
        gridLayout.removeAllViews()
        
        // Crear el diálogo
        val dialog = builder.setView(dialogView).create()
        
        // Agregar botones al grid
        emocionesEspecificasList.forEach { emocionEspecifica ->
            val button = Button(this).apply {
                text = emocionEspecifica
                background = getDrawable(R.drawable.bg_dialog_button)
                setTextColor(resources.getColor(R.color.colorPrimary, theme))
                setOnClickListener {
                    mostrarPopupContexto(emocion, emocionEspecifica, userId, fecha, diaSemanaActual)
                    dialog.dismiss()
                }
            }
            
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            
            gridLayout.addView(button, params)
        }
        
        // Configurar el botón de cancelar
        dialogView.findViewById<Button>(R.id.btnCancelar).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun mostrarPopupContexto(emocion: String, emocionEspecifica: String, userId: String, fecha: String, diaSemanaActual: Int) {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val dialogView = layoutInflater.inflate(R.layout.dialog_contexto, null)
        val gridLayout = dialogView.findViewById<GridLayout>(R.id.gridContextos)
        
        // Limpiar el grid
        gridLayout.removeAllViews()
        
        // Crear el diálogo
        val dialog = builder.setView(dialogView).create()
        
        // Agregar botones al grid
        contextos.forEach { contexto ->
            val button = Button(this).apply {
                text = contexto
                background = getDrawable(R.drawable.bg_dialog_button)
                setTextColor(resources.getColor(R.color.colorPrimary, theme))
                setOnClickListener {
                    // Actualizar la imagen en el grid inmediatamente
                    val container = gridEmociones.getChildAt(diaSemanaActual + 13) as LinearLayout
                    val imageView = container.getChildAt(0) as ImageView
                    imageView.setImageResource(emociones[emocion] ?: emojiNeutral)
                    // Mostrar diálogo de confirmación inmediatamente
                    mostrarDialogoConfirmacion(emocion, emocionEspecifica, contexto, userId, fecha)
                    dialog.dismiss()
                }
            }
            
            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                setMargins(8, 8, 8, 8)
            }
            
            gridLayout.addView(button, params)
        }
        
        // Configurar el botón de cancelar
        dialogView.findViewById<Button>(R.id.btnCancelar).setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun mostrarDialogoConfirmacion(emocion: String, emocionEspecifica: String, contexto: String, userId: String, fecha: String) {
        val builder = AlertDialog.Builder(this, R.style.DialogTheme)
        val dialogView = layoutInflater.inflate(R.layout.dialog_confirmacion_emocion, null)
        
        // Configurar el texto del mensaje
        val mensajeTextView = dialogView.findViewById<TextView>(R.id.mensajeConfirmacion)
        mensajeTextView.text = "Me siento $emocionEspecifica con mi $contexto"

        // Configurar la imagen
        val emojiImageView = dialogView.findViewById<ImageView>(R.id.emojiConfirmacion)
        emojiImageView.setImageResource(emociones[emocion] ?: emojiNeutral)

        // Configurar los botones
        val btnContarleDeppy = dialogView.findViewById<Button>(R.id.btnContarleDeppy)
        val btnTerminar = dialogView.findViewById<Button>(R.id.btnTerminar)

        builder.setView(dialogView)
        val dialog = builder.create()

        btnContarleDeppy.setOnClickListener {
            guardarEmocionCompleta(emocion, emocionEspecifica, contexto, userId, fecha) {
                dialog.dismiss()
                // Procesar el mensaje sin guardarlo y obtener respuesta de la IA
                val mensaje = "Me siento $emocionEspecifica con mi $contexto"
                lifecycleScope.launch {
                    chatRepository.processMessageWithoutSaving(userId, mensaje)
                }
                startActivity(Intent(this, ChatActivity::class.java))
            }
        }

        btnTerminar.setOnClickListener {
            guardarEmocionCompleta(emocion, emocionEspecifica, contexto, userId, fecha) {
                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun guardarEmocionCompleta(emocion: String, emocionEspecifica: String, contexto: String, userId: String, fecha: String, onSuccess: () -> Unit) {
        db.collection("usuarios")
            .document(userId)
            .collection("emociones")
            .document(fecha)
            .set(mapOf(
                "emocion" to emocion,
                "emocion_especifica" to emocionEspecifica,
                "contexto" to contexto,
                "fecha" to fecha
            ))
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al guardar la emoción", Toast.LENGTH_SHORT).show()
            }
    }

    private fun esMismaSemana(cal1: Calendar, cal2: Calendar): Boolean {
        val cal1Clone = cal1.clone() as Calendar
        val cal2Clone = cal2.clone() as Calendar

        // Ajustar ambos al lunes de su semana
        while (cal1Clone.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal1Clone.add(Calendar.DAY_OF_MONTH, -1)
        }
        while (cal2Clone.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            cal2Clone.add(Calendar.DAY_OF_MONTH, -1)
        }

        return cal1Clone.get(Calendar.YEAR) == cal2Clone.get(Calendar.YEAR) &&
               cal1Clone.get(Calendar.WEEK_OF_YEAR) == cal2Clone.get(Calendar.WEEK_OF_YEAR)
    }

    private fun cargarEmocionesGuardadas() {
        val userId = auth.currentUser?.uid ?: return
        val calendar = semanaActual.clone() as Calendar
        
        // Ajustar al lunes de la semana
        while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        // Cargar emociones para cada día de la semana
        for (i in 0..6) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale("es", "PE"))
            dateFormat.timeZone = TimeZone.getTimeZone("America/Lima")
            val fecha = dateFormat.format(calendar.time)
            
            db.collection("usuarios")
                .document(userId)
                .collection("emociones")
                .document(fecha)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val emocion = document.getString("emocion")
                        if (emocion != null) {
                            val container = gridEmociones.getChildAt(i + 14) as LinearLayout
                            val imageView = container.getChildAt(0) as ImageView
                            imageView.apply {
                                setImageResource(emociones[emocion] ?: emojiNeutral)
                                scaleType = ImageView.ScaleType.CENTER_CROP
                                adjustViewBounds = true
                                alpha = 1.0f
                                isEnabled = true
                            }
                        }
                    } else {
                        // Si no existe documento para esta fecha, mostrar imagen neutra
                        val container = gridEmociones.getChildAt(i + 14) as LinearLayout
                        val imageView = container.getChildAt(0) as ImageView
                        imageView.apply {
                            setImageResource(emojiNeutral)
                            scaleType = ImageView.ScaleType.CENTER_CROP
                            adjustViewBounds = true
                            alpha = 1.0f
                            isEnabled = true
                        }
                    }
                }
            calendar.add(Calendar.DAY_OF_MONTH, 1)
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 