package com.example.proyectodepspot

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.proyectodepspot.data.ContactoApoyo

class ContactosAdapter(
    private var contactos: List<ContactoApoyo>,
    private val onContactoClick: (ContactoApoyo) -> Unit
) : RecyclerView.Adapter<ContactosAdapter.ContactoViewHolder>() {

    class ContactoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textViewNombre: TextView = view.findViewById(R.id.textViewNombre)
        val textViewTelefono: TextView = view.findViewById(R.id.textViewTelefono)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_contacto, parent, false)
        return ContactoViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactoViewHolder, position: Int) {
        val contacto = contactos[position]
        holder.textViewNombre.text = contacto.nombre
        holder.textViewTelefono.text = contacto.telefono
        holder.itemView.setOnClickListener { onContactoClick(contacto) }
    }

    override fun getItemCount() = contactos.size

    fun actualizarContactos(nuevosContactos: List<ContactoApoyo>) {
        contactos = nuevosContactos
        notifyDataSetChanged()
    }
} 