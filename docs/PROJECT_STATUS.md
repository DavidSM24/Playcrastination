# Estado del proyecto

## Fase 1 · Infraestructura y recorrido local

- Proyecto Android Kotlin con Compose, Material 3, Navigation Compose, Hilt, Room, DataStore, Coil y Coroutines.
- Las cinco secciones tienen contenido local. El recorrido de verificación incluye guardar un juego, abrir su ficha, registrar minutos, ajustar el objetivo y añadirlo al backlog.
- El catálogo empieza vacío. Las cifras de ejemplo se usan únicamente en pruebas.
- `assembleDebug`, `testDebugUnitTest`, `lintDebug` y `assembleDebugAndroidTest` ejecutados con JDK 21 y SDK 35. Lint no detectó errores. Quedan avisos sobre versiones fijadas y atributos de plataforma.
- Cinco pruebas de dominio pasaron. Tres pruebas instrumentadas de Room pasaron en un emulador de Android 15 (API 35), incluida la reapertura de una base de datos persistente.
- El recorrido de interfaz pasó con el emulador en modo avión. Se creó la ficha, se calculó el 60 % a partir de 18 h jugadas y 30 h de objetivo, se añadió al backlog y se recuperó tras reiniciar el proceso.
- La última compilación se instaló y se revisó visualmente en español en Inicio y en la ficha.
- El APK de depuración está en `app/build/outputs/apk/debug/app-debug.apk`. La compilación declara API 26 como versión mínima. La prueba de dispositivo se hizo en API 35.

## Próxima fase

Ampliar la biblioteca local y el detalle con campos y filtros avanzados, gestión de partidas, valoración y validación de rendimiento. La reordenación de la cola, las integraciones, el histórico y las copias JSON se entregarán en sus fases respectivas.
