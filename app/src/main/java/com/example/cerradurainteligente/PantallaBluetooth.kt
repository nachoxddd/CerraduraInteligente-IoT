package com.example.cerradurainteligente

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

// Comprobar permisos Bluetooth
private fun tienePermisoBluetooth(contexto: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                contexto,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
}

// Consultar dispositivos emparejados
@SuppressLint("MissingPermission")
private fun obtenerDispositivos(
    contexto: Context
): Pair<String, List<BluetoothDevice>> {

    val gestor = contexto.getSystemService(
        Context.BLUETOOTH_SERVICE
    ) as? BluetoothManager

    val adaptador = gestor?.adapter
        ?: return "Bluetooth no disponible" to emptyList()

    if (!tienePermisoBluetooth(contexto)) {
        return "Permiso Bluetooth pendiente" to emptyList()
    }

    return try {
        if (!adaptador.isEnabled) {
            "Bluetooth desactivado" to emptyList()
        } else {
            val lista = adaptador.bondedDevices.toList()

            val estado = if (lista.isEmpty()) {
                "Bluetooth activo, sin dispositivos emparejados"
            } else {
                "Bluetooth activo"
            }

            estado to lista
        }
    } catch (e: SecurityException) {
        "Error de permisos Bluetooth" to emptyList()
    }
}

@SuppressLint("MissingPermission")
private fun nombreDispositivo(
    contexto: Context,
    dispositivo: BluetoothDevice
): String {
    if (!tienePermisoBluetooth(contexto)) {
        return "Dispositivo sin permiso"
    }

    return try {
        dispositivo.name ?: "Dispositivo sin nombre"
    } catch (e: SecurityException) {
        "Dispositivo sin permiso"
    }
}

@Composable
fun PantallaBluetooth(onVolver: () -> Unit) {

    val contexto = LocalContext.current

    val conexion = remember {
        ConexionHC05()
    }

    var estadoBluetooth by remember {
        mutableStateOf("Sin comprobar")
    }

    var dispositivos by remember {
        mutableStateOf<List<BluetoothDevice>>(emptyList())
    }

    var estadoConexion by remember {
        mutableStateOf("Desconectado")
    }

    var conectando by remember {
        mutableStateOf(false)
    }

    var conectado by remember {
        mutableStateOf(false)
    }

    // Cerrar la conexión al abandonar la pantalla
    DisposableEffect(conexion) {
        onDispose {
            conexion.desconectar()
        }
    }

    // Solicitar permiso Bluetooth
    val solicitarPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->

        if (concedido) {
            val resultado = obtenerDispositivos(contexto)
            estadoBluetooth = resultado.first
            dispositivos = resultado.second
        } else {
            estadoBluetooth = "Permiso denegado"
            dispositivos = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {

        Text(
            text = "Conexión Bluetooth",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text("Dispositivo objetivo: Arduino HC-05")

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (!tienePermisoBluetooth(contexto)) {
                    solicitarPermiso.launch(
                        Manifest.permission.BLUETOOTH_CONNECT
                    )
                } else {
                    val resultado = obtenerDispositivos(contexto)
                    estadoBluetooth = resultado.first
                    dispositivos = resultado.second
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Buscar dispositivos emparejados")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Estado Bluetooth")
                Text(
                    text = estadoBluetooth,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Estado de conexión")
                Text(
                    text = estadoConexion,
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Dispositivos emparejados",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (dispositivos.isEmpty()) {
            Text("No hay dispositivos disponibles")
        }

        dispositivos.forEach { dispositivo ->

            val nombre = nombreDispositivo(
                contexto,
                dispositivo
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {

                    Text(
                        text = nombre,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        enabled = !conectando && !conectado,
                        onClick = {

                            if (!tienePermisoBluetooth(contexto)) {
                                estadoConexion =
                                    "Falta permiso Bluetooth"
                            } else {
                                conectando = true
                                estadoConexion =
                                    "Conectando con $nombre..."

                                conexion.conectar(dispositivo) {
                                        exito, mensaje ->

                                    conectando = false
                                    conectado = exito

                                    estadoConexion = if (exito) {
                                        "Conectado a $nombre"
                                    } else {
                                        mensaje
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Conectar")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (conectado) {
            Button(
                onClick = {
                    conexion.desconectar()
                    conectado = false
                    estadoConexion = "Desconectado"
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Desconectar")
            }
        }

        if (conectando) {
            CircularProgressIndicator()
            Text("Estableciendo conexión...")
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = onVolver,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Volver a Monitoreo")
        }
    }
}