package com.example.cerradurainteligente

/** Validaciones básicas de la interfaz, adicionales a Firebase Authentication. */
object ValidadorContrasenas {
    fun problemas(clave: String): List<String> {
        val errores = mutableListOf<String>()
        val minusculas = clave.any { it.isLowerCase() }
        val mayusculas = clave.any { it.isUpperCase() }
        val numeros = clave.any { it.isDigit() }
        val simbolos = clave.any { !it.isLetterOrDigit() }
        val comun = listOf("1234", "qwerty", "password", "admin123", "asdf", "abcd")
            .any { clave.contains(it, ignoreCase = true) }

        if (clave.length < 8 || clave.length > 24) {
            errores.add("La contraseña debe tener de 8 a 24 caracteres.")
        }
        if (!minusculas) errores.add("Falta una letra minúscula.")
        if (!mayusculas) errores.add("Falta una letra mayúscula.")
        if (!numeros) errores.add("Falta un número.")
        if (!simbolos) errores.add("Falta un carácter especial.")
        if (comun) errores.add("Evita patrones comunes como qwerty y 1234.")

        return errores
    }
}
