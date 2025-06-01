package com.example.proyectodepspot

import android.app.Dialog
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectodepspot.data.ContactoApoyo
import com.example.proyectodepspot.data.ContactosRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class RedApoyoActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ContactosAdapter
    private val contactos = mutableListOf<ContactoApoyo>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_red_apoyo)

        recyclerView = findViewById(R.id.recyclerViewContactos)
        val fabAgregarContacto = findViewById<FloatingActionButton>(R.id.fabAgregarContacto)

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
        val dialogView = layoutInflater.inflate(R.layout.dialog_agregar_contacto, null)
        val editTextNombre = dialogView.findViewById<TextInputEditText>(R.id.editTextNombre)
        val editTextTelefono = dialogView.findViewById<TextInputEditText>(R.id.editTextTelefono)

        AlertDialog.Builder(this)
            .setTitle("Agregar Contacto de Apoyo")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = editTextNombre.text.toString()
                val telefono = editTextTelefono.text.toString()

                if (nombre.isNotBlank() && telefono.isNotBlank()) {
                    val nuevoContacto = ContactoApoyo(
                        nombre = nombre,
                        telefono = telefono
                    )
                    guardarContacto(nuevoContacto)
                } else {
                    Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun mostrarDialogoEditarContacto(contacto: ContactoApoyo) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_agregar_contacto, null)
        val editTextNombre = dialogView.findViewById<TextInputEditText>(R.id.editTextNombre)
        val editTextTelefono = dialogView.findViewById<TextInputEditText>(R.id.editTextTelefono)

        editTextNombre.setText(contacto.nombre)
        editTextTelefono.setText(contacto.telefono)

        AlertDialog.Builder(this)
            .setTitle("Editar Contacto")
            .setView(dialogView)
            .setPositiveButton("Guardar") { _, _ ->
                val nombre = editTextNombre.text.toString()
                val telefono = editTextTelefono.text.toString()

                if (nombre.isNotBlank() && telefono.isNotBlank()) {
                    val contactoActualizado = contacto.copy(
                        nombre = nombre,
                        telefono = telefono
                    )
                    guardarContacto(contactoActualizado)
                } else {
                    Toast.makeText(this, "Por favor completa todos los campos", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancelar", null)
            .setNeutralButton("Eliminar") { _, _ ->
                eliminarContacto(contacto)
            }
            .show()
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