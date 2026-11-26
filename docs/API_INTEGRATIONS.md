# Integraciones externas

La app usa HTTPS para las imágenes y para las consultas de Steam cuando se configura la cuenta. Coil gestiona la caché de imágenes. Retrofit usa un cliente OkHttp sin redirecciones y con límites de conexión y lectura para las consultas.

| Servicio | Alcance previsto | Estado |
| --- | --- | --- |
| Steam Web API | Perfil, biblioteca, actividad reciente, horas y logros cuando la cuenta lo permita. | Implementado con clave personal y endpoints de lectura. |
| Steam Store | Metadatos bajo demanda y caché. | Disponible mediante el endpoint público no documentado `appdetails`, con edición manual como alternativa. |
| HowLongToBeat | ID, búsqueda y duraciones cuando exista un método permitido. | Disponible la introducción manual y un enlace al sitio. Investigar acceso y condiciones antes de implementar consultas. |
| YouTube | Búsqueda, validación de vídeos y reproducción visible. | Pendiente. Verificar cuota, condiciones de almacenamiento, vigencia de la selección y políticas del reproductor. |
| IGDB | Proveedor de metadatos alternativo. | Posible ampliación. |

Steam usa Retrofit, Kotlin Serialization, WorkManager y Android Keystore. Mantener credenciales en un dispositivo cliente ofrece menos protección que mantenerlas en un servidor. Las claves quedan fuera del código, los logs y las copias de seguridad. Las pruebas de la Web API usan MockWebServer.

## Steam

Introduce en la app el SteamID64 del perfil y una **clave personal** de Steam Web API. DataStore guarda el identificador y la clave cifrada con AES-GCM bajo Android Keystore. El encabezado `x-webapi-key` se envía solo a `api.steampowered.com`. La validación comprueba el perfil y el acceso a su biblioteca. Una lista explícitamente vacía se acepta como válida. Una respuesta sin el campo de juegos ni un recuento cero solicita revisar la visibilidad. [Autenticación de Steamworks](https://partner.steamgames.com/doc/webapi_overview/auth) · [Condiciones de la API](https://steamcommunity.com/dev/apiterms)

| Consulta | Requisito y tratamiento |
| --- | --- |
| `IPlayerService/GetOwnedGames/v1` | Clave personal y biblioteca visible. La falta de acceso conserva los datos locales. |
| `IPlayerService/GetRecentlyPlayedGames/v1` | Clave personal. La actividad visible complementa la biblioteca y sirve para decidir qué jugar manualmente. |
| `ISteamUser/GetPlayerSummaries/v2` | Clave personal. Solo se consulta el SteamID64 que se configuró. El nombre, avatar y juego en curso dependen de la respuesta. |
| `ISteamUserStats/GetPlayerAchievements/v1` | Clave personal, identificador del juego y acceso a sus datos. Se solicita por juego y se conserva el valor previo si falla. |

[IPlayerService](https://partner.steamgames.com/doc/webapi/IPlayerService) · [ISteamUser](https://partner.steamgames.com/doc/webapi/ISteamUser#GetPlayerSummaries) · [Logros](https://partner.steamgames.com/doc/webapi/ISteamUserStats#GetPlayerAchievements)

Se puede importar toda la biblioteca, los juegos recientes o una selección. La importación deduplica por cuenta y Steam App ID, escribe en lotes de 25 y conserva los lotes completados al cancelar. Importar mantiene vacío el puesto en el backlog. Desconectar conserva las fichas importadas y borra la credencial de las preferencias. El usuario puede ocultar importaciones en la biblioteca activa y volver a mostrarlas.

La sincronización se puede iniciar en Inicio, con el gesto de actualizar, desde Ajustes o desde la ficha de un juego vinculado. WorkManager añade trabajo con conexión al arrancar o volver al primer plano si el último resultado ha caducado, y un trabajo periódico con intervalo inicial de 30 minutos. El sistema determina la hora real de ejecución. Los errores 401 y 403 requieren revisar la cuenta o los permisos. Las respuestas 429 pausan las llamadas según `Retry-After`. Los errores de conexión y los 5xx reciben reintentos limitados. Solo se refrescan los títulos ya importados y se conservan las decisiones personales. Cada cambio observado de minutos acumulados genera un snapshot.

Completar metadatos es una acción por juego que usa `appdetails` de la tienda, con datos en inglés para mantener un formato de fecha estable. El proveedor utiliza una vigencia de 24 horas y completa campos vacíos. Los fallos conservan la ficha editable y el enlace a la tienda. En esta sesión la solicitud pública de prueba a la tienda devolvió HTTP 403 desde el entorno de desarrollo.

## HowLongToBeat

Mantén separados proveedor, fuente remota, repositorio y mapeador. Investiga el acceso y las condiciones publicadas antes de conectar. Solo se incorporará una consulta compatible con las condiciones del servicio. Si es viable, conserva ID, metadatos, las tres duraciones y aportaciones cuando estén disponibles. Usa nombre normalizado, año, plataformas, subtítulos y distinción entre ediciones, remakes y remasterizaciones para proponer coincidencias. Solicita selección manual si hay ambigüedad y cachea los resultados. La edición manual y el enlace deben seguir disponibles cuando falle el servicio.

## YouTube y audio local

Usa YouTube Data API v3 para obtener y verificar hasta cinco pistas pertinentes por juego. Favorece material oficial y embebible, evita duplicados y resultados de otros títulos, y guarda la URL asociada. La búsqueda se encolará al incorporar el juego, con límites, pausa en importaciones grandes y reintento al recuperar conexión. Respeta las reglas de almacenamiento y actualización de los datos de YouTube.

La ficha reproducirá mediante un reproductor IFrame visible. Mantén la selección durante la pantalla, alterna la pista si existen varias, y pausa o libera al dejar de verla. El autoplay se habilitará explícitamente en ajustes y se ofrecerá reproducción manual cuando se bloquee. Prevé controles para reproducir, pausar, cambiar de pista, silenciar y abrir en YouTube. Para archivos locales autorizados por el usuario, AndroidX Media3 será una integración aparte.
