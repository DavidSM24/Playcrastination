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

La biblioteca empieza vacía. Añade un juego desde Inicio o Biblioteca. En su ficha puedes registrar metadatos, una valoración de 1 a 10, notas, fechas y tiempo de partida. También puedes elegir el objetivo, actualizar el estado y añadir el juego a la cola.

**Filtros y orden** combina género, plataforma, estado, año y favoritos. La búsqueda incluye el título original. Usa listas separadas por comas para los géneros y plataformas disponibles.

Introduce las fechas con formato `AAAA-MM-DD` y los tiempos en minutos. Deja en blanco los valores desconocidos o registra cero si sabes que todavía no has jugado. Desde **Historial de partidas** puedes crear otra partida conservando la anterior.

Desde **Backlog** puedes añadir juegos de tu biblioteca, arrastrarlos en la vista **Manual** y cambiar su prioridad u objetivo. El menú de cada ficha también permite subirla o bajarla, empezar a jugar, volver a ponerla en cola al pausar y retirarla. Las vistas por duración muestran una previsión alternativa. Ajusta las horas diarias desde Backlog, Inicio o Ajustes.

Desde **Ajustes → Steam** introduce tu SteamID64 y tu clave personal de Steam Web API en el dispositivo. Pulsa **Validar conexión** para comprobar el acceso. Puedes importar la biblioteca completa, una selección o los juegos recientes. La sincronización actualiza los títulos ya importados y las horas de Steam se muestran por separado de las partidas manuales. [Registro de claves personales de Steam](https://steamcommunity.com/dev/apikey)

Consulta [el estado del proyecto](docs/PROJECT_STATUS.md), [el plan por fases](docs/DEVELOPMENT_PLAN.md) y [las integraciones previstas](docs/API_INTEGRATIONS.md).
