package com.example.cerradurainteligente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class SeccionAdministrador { INICIO, REGISTROS, CUENTAS }

/** Panel exclusivo de administradores: eventos de todos los usuarios y gestión básica. */
@Composable
fun PantallaAdministrador(
    perfil: PerfilUsuario,
    repositorio: RepositorioNube,
    onSalir: () -> Unit
) {
    var seccion by remember { mutableStateOf(SeccionAdministrador.INICIO) }
    var registros by remember { mutableStateOf(emptyList<EventoCerradura>()) }
    var usuarios by remember { mutableStateOf(emptyList<PerfilUsuario>()) }
    var mensaje by remember { mutableStateOf("") }

    DisposableEffect(perfil.id) {
        val eventos = repositorio.escucharEventos(perfil) { lista, error ->
            registros = lista
            if (error != null) mensaje = error
        }
        val cuentas = repositorio.escucharUsuarios { lista, error ->
            usuarios = lista
            if (error != null) mensaje = error
        }

        onDispose {
            eventos.remove()
            cuentas.remove()
        }
    }

    when (seccion) {
        SeccionAdministrador.REGISTROS -> PantallaHistorialNube(
            eventos = registros,
            titulo = "Historial de todos los usuarios",
            onVolver = { seccion = SeccionAdministrador.INICIO }
        )
        SeccionAdministrador.CUENTAS -> PantallaCuentasAdmin(
            usuarios = usuarios,
            repositorio = repositorio,
            onVolver = { seccion = SeccionAdministrador.INICIO }
        )
        SeccionAdministrador.INICIO -> Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Text("Panel de administración", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Conectado: ${perfil.correo}")
            Text("Rol verificado en Firestore: administrador")
            Spacer(Modifier.height(20.dp))

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Usuarios registrados: ${usuarios.size}")
                    Spacer(Modifier.height(8.dp))
                    Text("Eventos registrados: ${registros.size}")
                    Spacer(Modifier.height(8.dp))
                    Text("Última acción: ${registros.firstOrNull()?.accion ?: "Ninguna"}")
                }
            }
            if (mensaje.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(mensaje, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { seccion = SeccionAdministrador.REGISTROS },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Ver historial compartido") }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { seccion = SeccionAdministrador.CUENTAS },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Administrar cuentas") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSalir, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar sesión")
            }
        }
    }
}

/** El administrador puede consultar, renombrar y activar/desactivar usuarios normales. */
@Composable
private fun PantallaCuentasAdmin(
    usuarios: List<PerfilUsuario>,
    repositorio: RepositorioNube,
    onVolver: () -> Unit
) {
    var editando by remember { mutableStateOf<PerfilUsuario?>(null) }
    var nombreEditado by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
    ) {
        Text("Administrar cuentas", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Roles administrados en el servidor; sin permisos de cambio desde el teléfono.")
        Spacer(Modifier.height(14.dp))

        if (mensaje.isNotBlank()) {
            Text(mensaje, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
        }

        usuarios.forEach { usuario ->
            Card(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(usuario.nombre, style = MaterialTheme.typography.titleMedium)
                    Text(usuario.correo)
                    Text("Rol: ${usuario.rol}")
                    Text(if (usuario.activo) "Cuenta activa" else "Cuenta desactivada")

                    if (usuario.rol == "usuario") {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            mensaje = ""
                            repositorio.cambiarEstadoCuenta(usuario.id, !usuario.activo) { error ->
                                mensaje = error ?: "Estado actualizado."
                            }
                        }) {
                            Text(if (usuario.activo) "Desactivar cuenta" else "Activar cuenta")
                        }
                        OutlinedButton(onClick = {
                            editando = usuario
                            nombreEditado = usuario.nombre
                        }) {
                            Text("Editar nombre")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Volver al panel")
        }
    }

    val elegido = editando
    if (elegido != null) {
        AlertDialog(
            onDismissRequest = { editando = null },
            title = { Text("Editar nombre de usuario") },
            text = {
                OutlinedTextField(
                    value = nombreEditado,
                    onValueChange = { nombreEditado = it },
                    label = { Text("Nuevo nombre") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    repositorio.cambiarNombreCuenta(elegido.id, nombreEditado) { error ->
                        mensaje = error ?: "Nombre actualizado."
                    }
                    editando = null
                }) { Text("Guardar") }
            },
            dismissButton = {
                TextButton(onClick = { editando = null }) { Text("Cancelar") }
            }
        )
    }
}
