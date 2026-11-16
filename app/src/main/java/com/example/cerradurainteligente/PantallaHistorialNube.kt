package com.example.cerradurainteligente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Locale

/** Un registro solo significa que el comando fue SIMULADO, no ejecutado por Arduino. */
@Composable
fun PantallaHistorialNube(
    eventos: List<EventoCerradura>,
    titulo: String,
    onVolver: () -> Unit
) {
    val formato = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.forLanguageTag("es-CL"))

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(titulo, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text("Acciones simuladas, almacenadas en Cloud Firestore")
        Spacer(Modifier.height(16.dp))

        if (eventos.isEmpty()) {
            Text("Todavía no se registran acciones.")
        }

        eventos.forEach { evento ->
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Acción: ${evento.accion}", style = MaterialTheme.typography.titleMedium)
                    Text("Usuario: ${evento.usuarioCorreo}")
                    Text("Fecha: ${evento.fecha?.let { formato.format(it) } ?: "Pendiente de sincronización"}")
                    Text("Dispositivo: ${evento.dispositivoId.take(8)}...")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Volver")
        }
    }
}
