package com.example.proyectodepspot

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectodepspot.data.ContactoApoyo
import com.example.proyectodepspot.data.ContactosRepository
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import android.view.View
import android.widget.TextView
import kotlinx.coroutines.launch
import android.widget.ImageButton

class RedApoyoActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactosAdapter
    private val contactos = mutableListOf<ContactoApoyo>()
    private lateinit var currentPopupWindow: PopupWindow

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_red_apoyo)

        // Configurar la barra superior
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        recyclerView = findViewById(R.id.recyclerViewContactos)
        val fabAgregarContacto = findViewById<ExtendedFloatingActionButton>(R.id.fabAgregarContacto)

        adapter = ContactosAdapter(contactos) { contacto ->
            mostrarDialogoEditarContacto(contacto)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@RedApoyoActivity)
            adapter = this@RedApoyoActivity.adapter
        }

        fabAgregarContacto.setOnClickListener {
            mostrarDialogoAgregarContacto()
        }

        cargarContactos()
    }

    private fun cargarContactos() {
        lifecycleScope.launch {
            ContactosRepository.obtenerContactos(
                onSuccess = { listaContactos ->
                    contactos.clear()
                    contactos.addAll(listaContactos)
                    adapter.actualizarContactos(contactos)
                },
                onError = { mensaje ->
                    Toast.makeText(this@RedApoyoActivity, mensaje, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun mostrarDialogoAgregarContacto() {
        val popupView = layoutInflater.inflate(R.layout.dialog_agregar_contacto, null)
        
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
        popupView.findViewById<TextView>(R.id.titleTextView).text = "Agregar Contacto"

        // Ocultar botón de eliminar
        popupView.findViewById<ImageButton>(R.id.buttonEliminar).visibility = View.GONE

        // Configurar campos
        val editTextNombre = popupView.findViewById<TextInputEditText>(R.id.editTextNombre)
        val editTextCorreo = popupView.findViewById<TextInputEditText>(R.id.editTextCorreo)

        // Configurar botones
        popupView.findViewById<MaterialButton>(R.id.buttonCancelar).setOnClickListener {
            popupWindow.dismiss()
        }

        popupView.findViewById<MaterialButton>(R.id.buttonGuardar).setOnClickListener {
            val nombre = editTextNombre.text.toString().trim()
            val correo = editTextCorreo.text.toString().trim()

            if (nombre.isNotEmpty() && correo.isNotEmpty()) {
                lifecycleScope.launch {
                    val contacto = ContactoApoyo(nombre = nombre, correo = correo)
                    guardarContacto(contacto)
                    popupWindow.dismiss()
                }
                } else {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
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

    private fun mostrarDialogoEditarContacto(contacto: ContactoApoyo) {
        val popupView = layoutInflater.inflate(R.layout.dialog_agregar_contacto, null)
        
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
        popupView.findViewById<TextView>(R.id.titleTextView).text = "Editar Contacto"

        // Mostrar y configurar botón de eliminar
        val buttonEliminar = popupView.findViewById<ImageButton>(R.id.buttonEliminar)
        buttonEliminar.visibility = View.VISIBLE
        buttonEliminar.setOnClickListener {
            lifecycleScope.launch {
                eliminarContacto(contacto)
                popupWindow.dismiss()
            }
        }

        // Configurar campos
        val editTextNombre = popupView.findViewById<TextInputEditText>(R.id.editTextNombre)
        val editTextCorreo = popupView.findViewById<TextInputEditText>(R.id.editTextCorreo)

        editTextNombre.setText(contacto.nombre)
        editTextCorreo.setText(contacto.correo)

        // Configurar botones
        popupView.findViewById<MaterialButton>(R.id.buttonCancelar).setOnClickListener {
            popupWindow.dismiss()
        }

        popupView.findViewById<MaterialButton>(R.id.buttonGuardar).setOnClickListener {
            val nombre = editTextNombre.text.toString().trim()
            val correo = editTextCorreo.text.toString().trim()

            if (nombre.isNotEmpty() && correo.isNotEmpty()) {
                lifecycleScope.launch {
                    val contactoActualizado = contacto.copy(nombre = nombre, correo = correo)
                    guardarContacto(contactoActualizado)
                    popupWindow.dismiss()
                }
                } else {
                Toast.makeText(this, "Por favor complete todos los campos", Toast.LENGTH_SHORT).show()
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

    private fun guardarContacto(contacto: ContactoApoyo) {
        lifecycleScope.launch {
            ContactosRepository.guardarContacto(
                contacto = contacto,
                onSuccess = {
                    Toast.makeText(this@RedApoyoActivity, "Contacto guardado exitosamente", Toast.LENGTH_SHORT).show()
                    cargarContactos()
                },
                onError = { mensaje ->
                    Toast.makeText(this@RedApoyoActivity, mensaje, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun eliminarContacto(contacto: ContactoApoyo) {
        lifecycleScope.launch {
            ContactosRepository.eliminarContacto(
                contactoId = contacto.id,
                onSuccess = {
                    Toast.makeText(this@RedApoyoActivity, "Contacto eliminado exitosamente", Toast.LENGTH_SHORT).show()
                    cargarContactos()
                },
                onError = { mensaje ->
                    Toast.makeText(this@RedApoyoActivity, mensaje, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
} 