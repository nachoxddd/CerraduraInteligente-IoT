package com.example.cerradurainteligente

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import java.util.Date
import java.util.UUID

/** Perfil almacenado en Firestore. La cuenta real se autentica en Firebase Auth. */
data class PerfilUsuario(
    val id: String,
    val nombre: String,
    val correo: String,
    val rol: String,
    val activo: Boolean
)

/** Registro compartido de una apertura o cierre simulado. */
data class EventoCerradura(
    val id: String,
    val usuarioId: String,
    val usuarioCorreo: String,
    val accion: String,
    val fecha: Date?,
    val dispositivoId: String
)

class RepositorioNube(contexto: Context) {
    private val autenticacion = FirebaseAuth.getInstance()
    private val baseDatos = FirebaseFirestore.getInstance()

    // Identifica esta instalación. NO intenta leer la MAC privada del teléfono.
    val dispositivoId: String

    init {
        val preferencias = contexto.getSharedPreferences("equipo", Context.MODE_PRIVATE)
        val guardado = preferencias.getString("identificador", null)

        if (guardado != null) {
            dispositivoId = guardado
        } else {
            val nuevo = UUID.randomUUID().toString()
            preferencias.edit().putString("identificador", nuevo).apply()
            dispositivoId = nuevo
        }
    }

    fun cerrarSesion() {
        autenticacion.signOut()
    }

    /** La sesión se comprueba en el servidor para no confiar en un rol local viejo. */
    fun recuperarSesion(resultado: (PerfilUsuario?, String?) -> Unit) {
        val usuario = autenticacion.currentUser

        if (usuario == null) {
            resultado(null, null)
            return
        }

        consultarPerfil(usuario.uid, resultado)
    }

    fun registrarCuenta(
        nombre: String,
        correo: String,
        contrasena: String,
        resultado: (PerfilUsuario?, String?) -> Unit
    ) {
        val problemas = ValidadorContrasenas.problemas(contrasena)
        if (problemas.isNotEmpty()) {
            resultado(null, problemas.first())
            return
        }
        if (nombre.trim().length < 3) {
            resultado(null, "El nombre debe tener al menos 3 caracteres.")
            return
        }

        val correoNormalizado = correo.trim().lowercase()
        autenticacion.createUserWithEmailAndPassword(correoNormalizado, contrasena)
            .addOnSuccessListener { respuesta ->
                val nuevoUsuario = respuesta.user
                if (nuevoUsuario == null) {
                    resultado(null, "No se pudo crear la cuenta.")
                    return@addOnSuccessListener
                }

                // El cliente SIEMPRE crea cuentas con rol usuario.
                // El primer administrador se configura manualmente en Firebase Console.
                val datos = hashMapOf(
                    "nombre" to nombre.trim(),
                    "correo" to correoNormalizado,
                    "rol" to "usuario",
                    "activo" to true,
                    "creadoEn" to FieldValue.serverTimestamp()
                )

                baseDatos.collection("usuarios")
                    .document(nuevoUsuario.uid)
                    .set(datos)
                    .addOnSuccessListener {
                        consultarPerfil(nuevoUsuario.uid, resultado)
                    }
                    .addOnFailureListener { error ->
                        autenticacion.signOut()
                        resultado(null, "No se pudo guardar el perfil: ${error.localizedMessage}")
                    }
            }
            .addOnFailureListener { error ->
                resultado(null, "Registro fallido: ${error.localizedMessage}")
            }
    }

    fun iniciarSesion(
        correo: String,
        contrasena: String,
        resultado: (PerfilUsuario?, String?) -> Unit
    ) {
        autenticacion.signInWithEmailAndPassword(correo.trim(), contrasena)
            .addOnSuccessListener { respuesta ->
                val usuario = respuesta.user
                if (usuario == null) {
                    resultado(null, "No se encontró la cuenta.")
                    return@addOnSuccessListener
                }
                consultarPerfil(usuario.uid, resultado)
            }
            .addOnFailureListener { error ->
                val codigo = if (error is com.google.firebase.auth.FirebaseAuthException) {
                    error.errorCode
                } else {
                    error.javaClass.simpleName
                }

                resultado(null, "Error de inicio de sesión: $codigo")
            }

    }

    private fun consultarPerfil(
        usuarioId: String,
        resultado: (PerfilUsuario?, String?) -> Unit
    ) {
        baseDatos.collection("usuarios").document(usuarioId)
            .get(Source.SERVER)
            .addOnSuccessListener { documento ->
                if (!documento.exists()) {
                    autenticacion.signOut()
                    resultado(null, "Tu cuenta no tiene un perfil configurado.")
                    return@addOnSuccessListener
                }

                val perfil = PerfilUsuario(
                    id = documento.id,
                    nombre = documento.getString("nombre") ?: "Sin nombre",
                    correo = documento.getString("correo") ?: "",
                    rol = documento.getString("rol") ?: "usuario",
                    activo = documento.getBoolean("activo") == true
                )

                if (!perfil.activo) {
                    autenticacion.signOut()
                    resultado(null, "Tu cuenta está desactivada. Contacta al administrador.")
                } else {
                    resultado(perfil, null)
                }
            }
            .addOnFailureListener {
                autenticacion.signOut()
                resultado(null, "No se pudo validar la sesión. Revisa tu conexión a Internet.")
            }
    }

    fun recuperarContrasena(correo: String, resultado: (String) -> Unit) {
        if (correo.isBlank()) {
            resultado("Primero escribe el correo de tu cuenta.")
            return
        }
        autenticacion.sendPasswordResetEmail(correo.trim())
            .addOnSuccessListener {
                resultado("Si el correo está registrado, revisa el mensaje de recuperación.")
            }
            .addOnFailureListener { error ->
                val codigo = if (error is com.google.firebase.auth.FirebaseAuthException) {
                    error.errorCode
                } else {
                    error.javaClass.simpleName
                }

                resultado("Error de recuperación: $codigo")
            }

    }

    /** Guarda una acción en la nube. No mueve un servomotor real. */
    fun registrarAccion(
        perfil: PerfilUsuario,
        accion: String,
        resultado: (String?) -> Unit
    ) {
        if (accion != "ABRIR" && accion != "CERRAR") {
            resultado("Acción no permitida.")
            return
        }

        val datos = hashMapOf(
            "usuarioId" to perfil.id,
            "usuarioCorreo" to perfil.correo,
            "accion" to accion,
            "fecha" to FieldValue.serverTimestamp(),
            "dispositivoId" to dispositivoId,
            "simulado" to true
        )

        baseDatos.collection("eventos")
            .add(datos)
            .addOnSuccessListener { resultado(null) }
            .addOnFailureListener { resultado("No se pudo guardar el evento en la nube.") }
    }

    fun escucharEventos(
        perfil: PerfilUsuario,
        resultado: (List<EventoCerradura>, String?) -> Unit
    ): ListenerRegistration {
        val consulta = if (perfil.rol == "administrador") {
            baseDatos.collection("eventos")
                .orderBy("fecha", Query.Direction.DESCENDING)
                .limit(100)
        } else {
            baseDatos.collection("eventos")
                .whereEqualTo("usuarioId", perfil.id)
                .limit(100)
        }

        return consulta.addSnapshotListener { documentos, error ->
            if (error != null) {
                resultado(emptyList(), "No se pudo consultar el historial.")
                return@addSnapshotListener
            }

            val eventos = documentos?.documents?.map { documento ->
                EventoCerradura(
                    id = documento.id,
                    usuarioId = documento.getString("usuarioId") ?: "",
                    usuarioCorreo = documento.getString("usuarioCorreo") ?: "",
                    accion = documento.getString("accion") ?: "",
                    fecha = documento.getTimestamp("fecha")?.toDate(),
                    dispositivoId = documento.getString("dispositivoId") ?: ""
                )
            } ?: emptyList()

            resultado(eventos.sortedByDescending { it.fecha?.time ?: 0L }, null)
        }
    }

    fun escucharUsuarios(
        resultado: (List<PerfilUsuario>, String?) -> Unit
    ): ListenerRegistration {
        return baseDatos.collection("usuarios")
            .orderBy("nombre")
            .addSnapshotListener { documentos, error ->
                if (error != null) {
                    resultado(emptyList(), "No se pudo cargar la lista de usuarios.")
                    return@addSnapshotListener
                }

                val perfiles = documentos?.documents?.map { documento ->
                    PerfilUsuario(
                        id = documento.id,
                        nombre = documento.getString("nombre") ?: "",
                        correo = documento.getString("correo") ?: "",
                        rol = documento.getString("rol") ?: "usuario",
                        activo = documento.getBoolean("activo") == true
                    )
                } ?: emptyList()

                resultado(perfiles, null)
            }
    }

    /** El servidor decide si la cuenta actual puede modificar a otra. */
    fun cambiarEstadoCuenta(
        usuarioId: String,
        habilitar: Boolean,
        resultado: (String?) -> Unit
    ) {
        baseDatos.collection("usuarios").document(usuarioId)
            .update("activo", habilitar)
            .addOnSuccessListener { resultado(null) }
            .addOnFailureListener { resultado("No tienes permiso o falló la actualización.") }
    }

    fun cambiarNombreCuenta(
        usuarioId: String,
        nuevoNombre: String,
        resultado: (String?) -> Unit
    ) {
        if (nuevoNombre.trim().length < 3) {
            resultado("El nombre debe tener al menos 3 caracteres.")
            return
        }
        baseDatos.collection("usuarios").document(usuarioId)
            .update("nombre", nuevoNombre.trim())
            .addOnSuccessListener { resultado(null) }
            .addOnFailureListener { resultado("No tienes permiso o falló la actualización.") }
    }
}
