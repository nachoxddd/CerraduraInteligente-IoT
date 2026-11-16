package com.example.cerradurainteligente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class SeccionUsuario { INICIO, ACTIVADOR, BLUETOOTH, HISTORIAL }

/** Pantallas de un usuario normal. Nunca contiene botones de administración. */
@Composable
fun PantallaUsuario(
    perfil: PerfilUsuario,
    repositorio: RepositorioNube,
    onSalir: () -> Unit
) {
    var seccion by remember { mutableStateOf(SeccionUsuario.INICIO) }
    var registros by remember { mutableStateOf(emptyList<EventoCerradura>()) }
    var error by remember { mutableStateOf("") }
    var guardando by remember { mutableStateOf(false) }

    // Suscripción en tiempo real: la app del usuario consulta solo sus eventos.
    DisposableEffect(perfil.id) {
        val suscripcion = repositorio.escucharEventos(perfil) { nuevos, problema ->
            registros = nuevos
            if (problema != null) error = problema
        }
        onDispose { suscripcion.remove() }
    }

    val ultimo = registros.firstOrNull()
    val estado = when (ultimo?.accion) {
        "ABRIR" -> "Abierta"
        "CERRAR" -> "Cerrada"
        else -> "Sin operaciones"
    }

    when (seccion) {
        SeccionUsuario.ACTIVADOR -> PantallaActivador(
            estadoSimulado = estado,
            ultimoComando = ultimo?.accion ?: "Ninguno",
            guardando = guardando,
            mensajeError = error,
            onEjecutarSimulacion = { accion ->
                guardando = true
                error = ""
                repositorio.registrarAccion(perfil, accion) { problema ->
                    guardando = false
                    error = problema ?: ""
                }
            },
            onVolver = { seccion = SeccionUsuario.INICIO }
        )
        SeccionUsuario.BLUETOOTH -> PantallaBluetooth(
            onVolver = { seccion = SeccionUsuario.INICIO }
        )
        SeccionUsuario.HISTORIAL -> PantallaHistorialNube(
            eventos = registros,
            titulo = "Mis registros",
            onVolver = { seccion = SeccionUsuario.INICIO }
        )
        SeccionUsuario.INICIO -> Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Cerradura inteligente", style = MaterialTheme.typography.headlineMedium)
            Text("Sesión: ${perfil.nombre} (usuario)")
            Spacer(Modifier.height(8.dp))
            Text("MODO SIMULACIÓN", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(20.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp)) {
                    Text("Estado simulado: $estado")
                    Spacer(Modifier.height(6.dp))
                    Text("Último comando: ${ultimo?.accion ?: "Ninguno"}")
                    Text("Identificador: ${repositorio.dispositivoId.take(8)}...")
                }
            }
            if (error.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(error, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { seccion = SeccionUsuario.ACTIVADOR },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Abrir / cerrar (simulado)") }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { seccion = SeccionUsuario.HISTORIAL },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Mis registros en la nube") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { seccion = SeccionUsuario.BLUETOOTH },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Bluetooth") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onSalir, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar sesión")
            }
        }
    }
}
