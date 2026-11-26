# Playcrastination

**Too many games. Not enough time.**

Gestor personal de biblioteca y backlog de videojuegos para Android. La base del proyecto funciona con datos locales y usa Kotlin, Jetpack Compose, Material 3, Room, Hilt y DataStore.

## Ejecutar

1. Abre esta carpeta en Android Studio y sincroniza Gradle.
2. Usa un JDK 17 o 21 para Gradle e instala las herramientas de compilación 35.0.0 y el SDK de Android 35. La aplicación admite Android 8.0 (API 26) en adelante.
3. Selecciona un emulador o dispositivo y ejecuta el módulo `app`.

Desde Windows se puede compilar y verificar con:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

En macOS o Linux usa `bash ./gradlew`. El APK de depuración se genera en `app/build/outputs/apk/debug/app-debug.apk`. La aplicación está pensada para instalación personal.

## Primera ejecución

La biblioteca empieza vacía. Añade un juego desde Inicio o Biblioteca. En su ficha puedes registrar notas y tiempo de partida, asignar un objetivo, actualizar el estado y añadirlo a la cola. Las estimaciones se introducen en minutos. Deja un campo en blanco si el valor es desconocido o registra cero si sabes que todavía no has jugado.

Consulta [el estado del proyecto](docs/PROJECT_STATUS.md), [el plan por fases](docs/DEVELOPMENT_PLAN.md) y [las integraciones previstas](docs/API_INTEGRATIONS.md).
