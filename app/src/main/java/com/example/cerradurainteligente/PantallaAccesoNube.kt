package com.example.cerradurainteligente

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

/** Una pantalla sencilla para entrar, registrarse y recuperar acceso. */
@Composable
fun PantallaAccesoNube(
    repositorio: RepositorioNube,
    mensajeInicial: String,
    onAccesoCorrecto: (PerfilUsuario) -> Unit
) {
    var modoRegistro by remember { mutableStateOf(false) }
    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var procesando by remember { mutableStateOf(false) }
    var mensaje by remember { mutableStateOf(mensajeInicial) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Cerradura Inteligente", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("Cuentas compartidas con Firebase")
        Spacer(Modifier.height(24.dp))

        if (modoRegistro) {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del usuario") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("Correo electrónico") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = contrasena,
            onValueChange = { contrasena = it },
            label = { Text("Contraseña") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (modoRegistro) {
            Spacer(Modifier.height(12.dp))
            Text("Requisitos de la contraseña", style = MaterialTheme.typography.titleSmall)
            Text("• Entre 8 y 24 caracteres")
            Text("• Mayúscula, minúscula, número y símbolo")
            Text("• Sin patrones comunes (qwerty, 1234, etc.)")
        }

        if (mensaje.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(mensaje, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        Button(
            enabled = !procesando,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                procesando = true
                mensaje = ""

                val alTerminar: (PerfilUsuario?, String?) -> Unit = { perfil, error ->
                    procesando = false
                    if (perfil == null) {
                        mensaje = error ?: "No se pudo completar la operación."
                    } else {
                        onAccesoCorrecto(perfil)
                    }
                }

                if (modoRegistro) {
                    repositorio.registrarCuenta(nombre, correo, contrasena, alTerminar)
                } else {
                    repositorio.iniciarSesion(correo, contrasena, alTerminar)
                }
            }
        ) {
            Text(if (procesando) "Comprobando..." else if (modoRegistro) "Crear cuenta" else "Iniciar sesión")
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            enabled = !procesando,
            onClick = {
                modoRegistro = !modoRegistro
                mensaje = ""
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (modoRegistro) "Ya tengo una cuenta" else "Registrar cuenta nueva")
        }

        if (!modoRegistro) {
            TextButton(
                onClick = { repositorio.recuperarContrasena(correo) { mensaje = it } }
            ) {
                Text("¿Olvidaste tu contraseña?")
            }
        }
    }
}
