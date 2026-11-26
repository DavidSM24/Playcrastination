# Estado del proyecto

## Fase 4 · Steam (0.4.0)

- Ajustes de Steam permite configurar SteamID64 y una clave personal cifrada, validar el acceso e importar la biblioteca completa, juegos recientes o una selección.
- Room 3 separa las observaciones de Steam de las partidas y preferencias personales. Los vínculos se deduplican por cuenta y App ID. Se registran snapshots del tiempo acumulado sin inferir sesiones. Los títulos importados se pueden ocultar y restaurar.
- La sincronización usa WorkManager y se puede iniciar manualmente desde Inicio, Ajustes o la ficha, con actualización por gesto en Inicio. Metadatos y logros se consultan por juego.
- Pasaron 21 pruebas unitarias y 11 instrumentadas en Android 15 (API 35). MockWebServer comprobó respuestas y fallos de Steam. Las pruebas instrumentadas cubrieron migración, cancelación y reanudación por lotes, deduplicación y conservación del progreso personal.
- La actualización sobre 0.3.0 en modo avión conservó el backlog. Se verificó la configuración cifrada con un valor simulado, su reapertura tras reiniciar, la programación de WorkManager con restricción de red y la retirada de la credencial al desconectar.
- La validación con una cuenta de Steam real requiere que el usuario introduzca su credencial en la app. La consulta pública de metadatos desde el entorno de desarrollo devolvió HTTP 403. La ficha mantiene la edición manual como alternativa.

## Fase 3 · Backlog (0.3.0)

- La cola permite añadir títulos desde la biblioteca, arrastrarlos en orden manual y moverlos con el menú contextual. Las vistas de duración conservan el orden guardado.
- Se pueden cambiar prioridad y objetivo, pasar un título a Jugando, devolverlo al final de la cola al pausar y retirarlo de la cola conservando la ficha. La cola reutiliza el esquema 2 de Room.
- Cada título tiene una previsión de inicio y fin cuando los tiempos lo permiten. Se planifican primero los juegos activos, se mantiene el subtotal conocido y se comparte la preferencia de 1 a 24 horas entre Backlog, Inicio y Ajustes.
- Pasaron 15 pruebas unitarias y 8 instrumentadas en Android 15 (API 35). Cubren fechas, tiempos ausentes, movimientos concurrentes, persistencia, cambios de estado y la migración anterior. El APK de depuración se generó y lint terminó con cero errores.
- La actualización sobre 0.2.0 en modo avión conservó el backlog. En el emulador se comprobaron el orden temporal, el arrastre y el comando de subir, la persistencia tras reiniciar, la prioridad, el objetivo, el recálculo al cambiar horas diarias, los cambios entre Jugando y En pausa y la opción de retirar y volver a añadir un juego.

## Fase 2 · Biblioteca local (0.2.0)

- La biblioteca busca por título y título original, combina cinco filtros y permite ordenar en ambos sentidos por seis criterios.
- El editor y la ficha incorporan metadatos de publicación, plataformas, fondo y valoración personal de 1 a 10. Se pueden guardar fechas, tiempo, progreso narrativo y desglose opcional de la partida.
- La ficha conserva un historial editable. La partida actual determina el progreso hacia el objetivo. Los tiempos registrados se acumulan para la biblioteca, Inicio y Estadísticas.
- La migración al esquema 2 de Room conserva los registros de la versión 1, incluidos los minutos, el progreso narrativo y el backlog. El proyecto incluye los esquemas de ambas versiones.
- Pasaron 10 pruebas unitarias y 5 instrumentadas en Android 15 (API 35), incluida la migración. La prueba de catálogo usa 5.000 juegos. El APK de depuración se generó y lint terminó con cero errores y avisos de versiones fijadas y atributos de plataforma.
- La actualización sobre 0.1.0 en modo avión conservó la ficha, su posición en la cola y el 60 % registrado. En el mismo emulador se probó la edición, la creación de otra partida, la corrección del historial y la combinación de filtros con la búsqueda por título original. El tiempo del historial y la valoración se recuperaron después de reiniciar el proceso.

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

Investigar el acceso permitido a HowLongToBeat y conservar la edición manual de duraciones como alternativa.
