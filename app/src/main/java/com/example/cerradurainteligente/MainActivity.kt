package com.example.cerradurainteligente

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/** Punto de entrada: decide qué pantalla corresponde según el rol autenticado. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val repositorio = remember { RepositorioNube(applicationContext) }
                var perfil by remember { mutableStateOf<PerfilUsuario?>(null) }
                var comprobando by remember { mutableStateOf(true) }
                var mensajeAcceso by remember { mutableStateOf("") }

                LaunchedEffect(Unit) {
                    repositorio.recuperarSesion { encontrado, problema ->
                        perfil = encontrado
                        mensajeAcceso = problema ?: ""
                        comprobando = false
                    }
                }

                if (comprobando) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (perfil == null) {
                    PantallaAccesoNube(
                        repositorio = repositorio,
                        mensajeInicial = mensajeAcceso,
                        onAccesoCorrecto = { perfil = it }
                    )
                } else if (perfil?.rol == "administrador") {
                    PantallaAdministrador(
                        perfil = perfil!!,
                        repositorio = repositorio,
                        onSalir = {
                            repositorio.cerrarSesion()
                            perfil = null
                            mensajeAcceso = ""
                        }
                    )
                } else {
                    PantallaUsuario(
                        perfil = perfil!!,
                        repositorio = repositorio,
                        onSalir = {
                            repositorio.cerrarSesion()
                            perfil = null
                            mensajeAcceso = ""
                        }
                    )
                }
            }
        }
    }
}
