# Integraciones externas

Actualmente la app solo consulta las imágenes HTTPS configuradas en cada ficha. Coil gestiona la caché y usa OkHttp con límites de conexión y lectura. Se desactivan el tráfico HTTP en claro y las redirecciones hacia un esquema distinto.

| Servicio | Alcance previsto | Estado |
| --- | --- | --- |
| Steam Web API | Perfil, biblioteca, actividad reciente, horas y logros cuando la cuenta lo permita. | Pendiente. Verificar privacidad y credenciales por endpoint. |
| Steam Store | Metadatos bajo demanda y caché. | Pendiente. Evaluar acceso y preparar fallback si se usa appdetails, que carece de documentación oficial. |
| HowLongToBeat | ID, búsqueda y duraciones cuando exista un método permitido. | Disponible la introducción manual y un enlace al sitio. Investigar acceso y condiciones antes de implementar consultas. |
| YouTube | Búsqueda, validación de vídeos y reproducción visible. | Pendiente. Verificar cuota, condiciones de almacenamiento, vigencia de la selección y políticas del reproductor. |
| IGDB | Proveedor de metadatos alternativo. | Posible ampliación. |

Retrofit, Kotlin Serialization, WorkManager y Android Keystore se incorporarán con las integraciones correspondientes. Mantener credenciales en un dispositivo cliente ofrece menos protección que mantenerlas en un servidor. Las claves nunca se incluirán en el código ni en exportaciones. Las pruebas de integración deberán usar respuestas simuladas, por ejemplo con MockWebServer.

## Steam

El usuario configurará un Steam ID de 64 bits y una clave Web API cifrada con Android Keystore. Verifica los requisitos de autenticación y privacidad de `IPlayerService/GetOwnedGames`, `IPlayerService/GetRecentlyPlayedGames`, `ISteamUser/GetPlayerSummaries` y los endpoints de logros antes de habilitar cada operación. Implementa la fuente remota y el repositorio por separado. El proveedor de metadatos de tienda tendrá su propia interfaz.

La importación debe ser progresiva y cancelable y admitir biblioteca completa, juegos seleccionados y actividad reciente. Descarga metadatos individuales bajo demanda. Mantén importación y alta en el backlog como decisiones independientes. La actividad reciente puede sugerir un estado para revisión del usuario.

Inicia la sincronización manualmente, mediante gesto de actualización, al arrancar, al volver al primer plano si caducó y con WorkManager cuando haya red. Usa 30 minutos como intervalo configurable inicial y considera la planificación del sistema aproximada. Deduplica por Steam App ID y actualiza solo los campos de Steam. Conserva la última versión válida y las decisiones personales ante errores. Gestiona explícitamente 401, 403, 429 y 5xx con límites de tiempo y reintentos apropiados. Registra cambios en contadores acumulados como snapshots observados.

## HowLongToBeat

Mantén separados proveedor, fuente remota, repositorio y mapeador. Investiga el acceso y las condiciones publicadas antes de conectar. Solo se incorporará una consulta compatible con las condiciones del servicio. Si es viable, conserva ID, metadatos, las tres duraciones y aportaciones cuando estén disponibles. Usa nombre normalizado, año, plataformas, subtítulos y distinción entre ediciones, remakes y remasterizaciones para proponer coincidencias. Solicita selección manual si hay ambigüedad y cachea los resultados. La edición manual y el enlace deben seguir disponibles cuando falle el servicio.

## YouTube y audio local

Usa YouTube Data API v3 para obtener y verificar hasta cinco pistas pertinentes por juego. Favorece material oficial y embebible, evita duplicados y resultados de otros títulos, y guarda la URL asociada. La búsqueda se encolará al incorporar el juego, con límites, pausa en importaciones grandes y reintento al recuperar conexión. Respeta las reglas de almacenamiento y actualización de los datos de YouTube.

La ficha reproducirá mediante un reproductor IFrame visible. Mantén la selección durante la pantalla, alterna la pista si existen varias, y pausa o libera al dejar de verla. El autoplay se habilitará explícitamente en ajustes y se ofrecerá reproducción manual cuando se bloquee. Prevé controles para reproducir, pausar, cambiar de pista, silenciar y abrir en YouTube. Para archivos locales autorizados por el usuario, AndroidX Media3 será una integración aparte.
