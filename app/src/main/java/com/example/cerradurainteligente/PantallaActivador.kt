package com.example.cerradurainteligente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Ejecuta acciones VIRTUALES: no envía órdenes a Arduino. */
@Composable
fun PantallaActivador(
    estadoSimulado: String,
    ultimoComando: String,
    guardando: Boolean,
    mensajeError: String,
    onEjecutarSimulacion: (String) -> Unit,
    onVolver: () -> Unit
) {
    var accionSeleccionada by remember { mutableStateOf("ABRIR") }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Activador de Cerradura", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("MODO SIMULACIÓN", color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(24.dp))
        Text("Selecciona la acción que deseas simular")
        Spacer(Modifier.height(12.dp))

        Row {
            RadioButton(
                selected = accionSeleccionada == "ABRIR",
                onClick = { accionSeleccionada = "ABRIR" }
            )
            Text("Abrir cerradura", modifier = Modifier.padding(top = 12.dp))
        }
        Row {
            RadioButton(
                selected = accionSeleccionada == "CERRAR",
                onClick = { accionSeleccionada = "CERRAR" }
            )
            Text("Cerrar cerradura", modifier = Modifier.padding(top = 12.dp))
        }

        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onEjecutarSimulacion(accionSeleccionada) },
            enabled = !guardando,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (guardando) "Guardando..." else "Ejecutar simulación")
        }
        if (mensajeError.isNotEmpty()) {
            Text(mensajeError, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("Estado virtual de la cerradura")
                Text(estadoSimulado, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text("Último comando: $ultimoComando")
            }
        }
        Spacer(Modifier.height(24.dp))
        OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth()) {
            Text("Volver a Monitoreo")
        }
    }
}
