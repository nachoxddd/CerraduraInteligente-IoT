package com.example.cerradurainteligente


import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import java.io.IOException
import java.util.UUID

class ConexionHC05 {

    // Identificador estándar del servicio Bluetooth Serial
    private val uuidSPP: UUID = UUID.fromString(
        "00001101-0000-1000-8000-00805F9B34FB"
    )

    private val manejador = Handler(Looper.getMainLooper())

    @Volatile
    private var socket: BluetoothSocket? = null

    @Volatile
    private var socketPendiente: BluetoothSocket? = null

    // La pantalla debe comprobar los permisos antes de llamar
    @SuppressLint("MissingPermission")
    fun conectar(
        dispositivo: BluetoothDevice,
        onResultado: (Boolean, String) -> Unit
    ) {
        Thread {
            var nuevoSocket: BluetoothSocket? = null

            try {
                // Crear canal Bluetooth serial autenticado
                nuevoSocket =
                    dispositivo.createRfcommSocketToServiceRecord(
                        uuidSPP
                    )

                socketPendiente = nuevoSocket

                // Intentar conectar fuera del hilo principal
                nuevoSocket.connect()

                socket = nuevoSocket

                manejador.post {
                    onResultado(
                        true,
                        "Conectado correctamente al dispositivo"
                    )
                }

            } catch (e: SecurityException) {
                manejador.post {
                    onResultado(
                        false,
                        "Permiso Bluetooth insuficiente"
                    )
                }

            } catch (e: IOException) {
                manejador.post {
                    onResultado(
                        false,
                        "No se pudo establecer la conexión"
                    )
                }

            } finally {
                socketPendiente = null

                if (socket !== nuevoSocket) {
                    try {
                        nuevoSocket?.close()
                    } catch (_: IOException) {
                    }
                }
            }
        }.start()
    }

    fun estaConectado(): Boolean {
        return socket?.isConnected == true
    }

    fun desconectar() {
        try {
            socketPendiente?.close()
        } catch (_: IOException) {
        }

        try {
            socket?.close()
        } catch (_: IOException) {
        }

        socketPendiente = null
        socket = null
    }
}
