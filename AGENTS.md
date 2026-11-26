# Playcrastination

- Lee README.md y docs/PROJECT_STATUS.md al empezar. Consulta docs/DEVELOPMENT_PLAN.md para el alcance de cada fase.
- Trabaja sobre la estructura Android nativa existente. Conserva las fronteras entre dominio, datos e interfaz detalladas en docs/ARCHITECTURE.md.
- Mantén Kotlin y Jetpack Compose como base del módulo `app`. Consulta la [guía de Java y Android Studio](docs/ANDROID_JAVA.md) para el trabajo con Java o vistas XML y para las pautas de configuración, seguridad y verificación en Android.
- Room es la fuente de verdad de la biblioteca. Las escrituras relacionadas deben ser transaccionales. Versiona los esquemas exportados y añade una migración y una prueba al cambiar el esquema.
- Conserva los datos personales durante la sincronización. Almacena minutos enteros y representa el tiempo desconocido con null.
- Añade los textos de interfaz a los recursos de Android en inglés y español. Usa los componentes y tokens de diseño existentes.
- Usa referencias oficiales para verificar las APIs. Nunca registres credenciales ni las incluyas en Git, copias de seguridad, datos de ejemplo o capturas.
- No introduzcas datos ficticios en la biblioteca de producción. Las pruebas pueden usar juegos y respuestas simuladas.
- Verifica los cambios con el Gradle Wrapper. Ejecuta las pruebas de dispositivo si hay un emulador o dispositivo disponible. Registra las verificaciones, los límites y los cambios relevantes en docs/PROJECT_STATUS.md y docs/CHANGELOG.md.
