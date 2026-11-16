# Cerradura Inteligente — versión Firebase (Unidad 2)

Esta es una **copia separada** del proyecto que funcionaba con SQLite.

## Lo que hace esta versión

- Registro e inicio de sesión con correo y contraseña por Firebase Authentication.
- Dos tipos de acceso: `usuario` y `administrador`.
- Usuario: abre y cierra una cerradura **simulada** y consulta sus propios eventos.
- Administrador: ve los registros de todos, activa/desactiva cuentas normales y cambia nombres.
- Cada evento incluye correo, acción, fecha/hora del servidor e ID de la instalación.
- Los cambios se sincronizan por Cloud Firestore en ambos dispositivos.
- Se mantiene la pantalla Bluetooth para futuras pruebas con HC-05.

**NO controla Arduino**. No envía comandos de apertura al HC-05. La conexión física corresponde a otra evaluación, según el docente.

**Importante:** esta versión **no implementa todavía** la creación y eliminación de cuentas por el administrador, la reconexión automática Bluetooth, ni un bloqueo por intentos fallidos aplicado desde el servidor. El restablecimiento de contraseña se hace por correo mediante Firebase. No afirmar que el CRUD de cuentas esté completo.

## Paso 1: Crear Firebase

1. En https://console.firebase.google.com crea un proyecto (p. ej. `cerradura-iot-sumativa`).
2. Registra una app Android con `com.example.cerradurainteligente` como nombre de paquete (exacto).
3. Descarga `google-services.json` y guárdalo en `cerradura_firebase/app/google-services.json`.
4. En **Authentication → Sign-in method**, activa **Correo electrónico/Contraseña**.
5. En **Firestore Database** crea una base de datos en **modo producción**.
6. En la pestaña **Reglas**, reemplaza las reglas por el contenido de `firestore.rules`, y **Publicar**.

**No uses el modo de prueba abierto en Firestore**: permitiría leer o modificar información sin control.

No compartas contraseñas reales ni llaves privadas de cuentas de servicio de Firebase. `google-services.json` es configuración de la app, pero evita subir copias de pruebas innecesarias.

## Paso 2: Abrir y ejecutar

1. Descomprime este ZIP en una carpeta **nueva**, no encima de la anterior.
2. Comprueba que `google-services.json` esté en la carpeta `app/`, al mismo nivel que `build.gradle.kts`.
3. Abre `cerradura_firebase` desde Android Studio con **File → Open**.
4. Espera la sincronización de Gradle. Necesitas Internet para descargar dependencias.
5. Instala la app primero en el emulador. Si falta el archivo JSON, Gradle fallará: es normal hasta configurarlo.

## Paso 3: Crear administrador (no desde la app)

1. En el **emulador**, pulsa **Registrar cuenta nueva** y registra una cuenta con un correo real.
2. En Firebase Console → Authentication → Users, busca esa cuenta y copia el **UID**.
3. En Firestore → Datos → colección `usuarios` → documento cuyo ID es ese UID.
4. Cambia el campo `rol` de `usuario` a `administrador` **desde Firebase Console**.
5. Cierra sesión en el emulador y vuelve a entrar; aparecerá el panel de administración.

La edición manual de ese primer rol es intencional. El cliente **no tiene permiso** para crear administradores, de acuerdo con las reglas de Firestore.

## Paso 4: Crear usuario en celular

1. Instala la misma versión Firebase en tu celular Android desde Android Studio.
2. Registra una cuenta nueva con otro correo; quedará con rol `usuario`.
3. Pulsa abrir y cerrar en la pantalla de simulación.
4. Revisa **Panel de administración → Ver historial compartido** en el emulador.
5. Prueba desde el administrador desactivar al usuario normal; intenta un nuevo acceso en el celular y comprueba la restricción.

**No uses la misma cuenta en ambos dispositivos para esta demostración**.

## Arquitectura en palabras simples

- `MainActivity.kt`: elige pantalla según la cuenta autenticada y su rol.
- `PantallaAccesoNube.kt`: login, registro y recuperación de contraseña.
- `RepositorioNube.kt`: centraliza las operaciones con Firebase Authentication y Firestore.
- `PantallaUsuario.kt`: simulación e historial de un usuario normal.
- `PantallaAdministrador.kt`: supervisión de registros y gestión básica de cuentas.
- `PantallaHistorialNube.kt`: muestra usuario, acción, fecha y dispositivo.
- `ValidadorContrasenas.kt`: valida longitud y complejidad de contraseñas.
- `PantallaBluetooth.kt`, `ConexionHC05.kt`: herramientas existentes para HC-05, sin envío de comando de cerradura aún.
- `firestore.rules`: reglas de seguridad del servidor que controlan roles y acciones permitidas.

## Advertencias importantes

- Si no hay Internet, no prometas que las acciones se han aceptado por el servidor. La app espera confirmación de Firestore para considerar guardado un evento.
- El identificador de instalación es un UUID guardado localmente. No es la dirección MAC del teléfono y cambia si se borran los datos de la app.
- Firebase Authentication gestiona y protege las contraseñas. No se implementó un hash personalizado en el teléfono para esta versión.
- El código cliente solo valida **formato** de la contraseña. Protección real frente a ataques distribuidos y gestión privilegiada completa requieren políticas de backend adicionales.
- Los registros antiguos de SQLite **NO se migran** automáticamente a Firestore. Guarda la versión anterior como respaldo.
- No se ha podido compilar y ejecutar esta variante contra tu proyecto Firebase real desde este entorno. La primera compilación debe validarse en tu Android Studio.
