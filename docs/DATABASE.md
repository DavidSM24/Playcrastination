# Base de datos

Room crea `playcrastination.db`. La versión 3 conserva la biblioteca y añade tres tablas de Steam:

| Tabla | Clave y contenido |
| --- | --- |
| `games` | ID entero autogenerado, título indexado, metadatos, datos personales y marcas de creación y actualización. Incluye título original, fecha de publicación opcional, desarrollador, distribuidor, plataformas disponibles, fondo y valoración opcional. |
| `playthroughs` | ID entero autogenerado y clave foránea al juego. Índice por juego e ID. Cada fila guarda minutos, fechas, progreso narrativo y un desglose de tiempo opcional. |
| `game_durations` | ID del juego como clave primaria y foránea. Tres estimaciones en minutos, todas opcionales. |
| `backlog_entries` | ID del juego como clave primaria y foránea. Posición única en la cola, asignada de forma transaccional. |
| `steam_links` | ID local como clave primaria y foránea. Pareja única SteamID64 y Steam App ID, minutos observados, actividad, ocultación, logros y fecha de metadatos. |
| `steam_profiles` | SteamID64 como clave. Nombre, avatar, juego en curso cuando está disponible y última sincronización de la biblioteca. |
| `steam_playtime_snapshots` | ID de observación y clave foránea al vínculo Steam. Registra minutos acumulados y hora de observación sin deducir sesiones de juego. |

La eliminación explícita en cascada borra los registros dependientes. El guardado actualiza la ficha, la partida actual y las estimaciones en una transacción. La partida con el ID más alto es la actual. Crear otra partida inserta una fila vacía y conserva los datos acumulados de las anteriores. Las fechas de lanzamiento y de partida se guardan como días de calendario a partir de la época Unix. Las posiciones pueden quedar discontinuas después de quitar un juego. El puesto visible se calcula a partir del orden efectivo. El dominio deriva porcentajes temporales, totales y tiempos restantes de las partidas y del objetivo.

La migración de la versión 1 a la 2 añade campos opcionales a las fichas y traslada los minutos y el progreso narrativo de `game_progress` a una partida inicial de cada juego. Conserva los IDs, estados, notas, favoritos, objetivos, estimaciones y posiciones de la cola. Cuando la ficha anterior carece de progreso, su partida inicial conserva valores desconocidos. Los esquemas de ambas versiones se exportan en `app/schemas`. [Migraciones de Room](https://developer.android.com/training/data-storage/room/migrating-db-versions)

El backlog avanzado reutiliza el esquema 2. El movimiento coloca el juego antes o después de otro juego según el orden leído en la transacción. Se sustituye el conjunto de posiciones en esa misma transacción para respetar el índice único. Pasar a Jugando actualiza el estado y retira la entrada. Pausar actualiza el estado y añade el juego al final si todavía no estaba en la cola. Los cambios rápidos de objetivo o prioridad actualizan sus propias columnas.

Los estados, prioridades y objetivos se almacenan con el nombre de su enum. Conserva el valor almacenado o migra la columna si cambia el nombre. Los géneros y plataformas se guardan como texto y se interpretan como listas para las facetas de la biblioteca. DataStore guarda por separado las horas de juego diarias y la configuración cifrada de Steam.

La migración 2 → 3 crea las tablas de Steam y sus índices. Las fichas existentes conservan sus datos y empezarán como títulos manuales. Importar un Steam App ID no fusiona fichas existentes por similitud de título. Los contadores de Steam se escriben por separado del progreso manual. El histórico gráfico de observaciones queda previsto en la fase de estadísticas.

Android tiene desactivada la copia automática de los datos privados de la app. La fase 7 incorporará la exportación explícita a JSON desde el selector de documentos. [Reglas de copia de Android](https://developer.android.com/identity/data/autobackup)
