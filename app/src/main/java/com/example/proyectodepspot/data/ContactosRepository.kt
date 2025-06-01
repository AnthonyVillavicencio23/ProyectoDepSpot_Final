package com.example.proyectodepspot.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object ContactosRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private const val CONTACTOS_COLLECTION = "contactos_apoyo"

    suspend fun guardarContacto(contacto: ContactoApoyo, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            val userEmail = auth.currentUser?.email ?: throw Exception("Usuario no autenticado")
            val contactoConUsuario = contacto.copy(userEmail = userEmail)
            
            if (contacto.id.isEmpty()) {
                // Nuevo contacto
                val docRef = db.collection(CONTACTOS_COLLECTION).document()
                val nuevoContacto = contactoConUsuario.copy(id = docRef.id)
                docRef.set(nuevoContacto).await()
            } else {
                // Actualizar contacto existente
                db.collection(CONTACTOS_COLLECTION)
                    .document(contacto.id)
                    .set(contactoConUsuario)
                    .await()
            }
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Error al guardar el contacto")
        }
    }

    suspend fun obtenerContactos(onSuccess: (List<ContactoApoyo>) -> Unit, onError: (String) -> Unit) {
        try {
            val userEmail = auth.currentUser?.email ?: throw Exception("Usuario no autenticado")
            
            val snapshot = db.collection(CONTACTOS_COLLECTION)
                .whereEqualTo("userEmail", userEmail)
                .get()
                .await()

            val contactos = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ContactoApoyo::class.java)
            }.sortedBy { it.nombre }
            
            onSuccess(contactos)
        } catch (e: Exception) {
            onError(e.message ?: "Error al obtener los contactos")
        }
    }

    suspend fun eliminarContacto(contactoId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            db.collection(CONTACTOS_COLLECTION)
                .document(contactoId)
                .delete()
                .await()
            onSuccess()
        } catch (e: Exception) {
            onError(e.message ?: "Error al eliminar el contacto")
        }
    }
} 