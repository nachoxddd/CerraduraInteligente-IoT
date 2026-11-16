# Pruebas rápidas para presentar al profesor

| Caso | Pasos | Resultado esperado |
|---|---|---|
| Registro | Crear usuario nuevo con correo | Aparece perfil en Firestore, rol usuario |
| Credenciales | Intentar clave incorrecta | No permite entrar |
| Password débil | Crear cuenta con qwerty1234 | Bloquea registro por reglas de la interfaz |
| Dos roles | Iniciar admin en emulador y usuario en celular | Pantallas distintas |
| Registro en la nube | Usuario pulsa Abrir | El registro aparece en pantalla de admin |
| Atribución | Abrir historial | Muestra correo, fecha, ID dispositivo y acción |
| Desactivación | Admin desactiva a usuario | Firestore impide nuevos eventos del usuario |
| Recuperación | Pulsar olvidé contraseña | Envía correo mediante Firebase Auth si se procesa la solicitud |
| Bluetooth | Abrir sección y revisar permisos | Solicita permisos y consulta emparejados |

## Explicación corta

«El usuario inicia sesión a través de Firebase Authentication. Firestore conserva un perfil con su rol y estado; las reglas de seguridad del servidor impiden que un usuario común gestione otras cuentas. Al simular una apertura o cierre, se guarda en Firestore un evento asociado a su UID y un identificador de dispositivo. El administrador, desde el emulador, ve el historial compartido y puede activar o desactivar cuentas normales. Por ahora el control del Arduino sigue simulado.»

## Limitaciones que debes mencionar si te preguntan

- No hay conexión real con Arduino ni reconexión Bluetooth terminada.
- No existe CRUD completo de usuarios en Firebase Authentication desde el cliente; hay lista, cambio de nombre, y activación/desactivación mediante perfil Firestore.
- No se implementó bloqueo centralizado tras 5 fallos (una contraseña fuerte y Firebase Auth no equivalen a una política de bloqueo configurada).
- No se desarrolló un QR dinámico.
- Se requiere Internet para comprobación de rol desde servidor y para confirmar eventos.
