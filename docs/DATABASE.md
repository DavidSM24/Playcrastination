# Base de datos

Room crea `playcrastination.db`. La versión inicial usa cuatro tablas:

| Tabla | Clave y contenido |
| --- | --- |
| `games` | ID entero autogenerado, título indexado, metadatos, datos personales y marcas de creación y actualización. |
| `game_progress` | ID del juego como clave primaria y foránea. Minutos de partida y porcentaje narrativo manual opcionales. |
| `game_durations` | ID del juego como clave primaria y foránea. Tres estimaciones en minutos, todas opcionales. |
| `backlog_entries` | ID del juego como clave primaria y foránea. Posición única en la cola, asignada de forma transaccional. |

La eliminación explícita en cascada borra los registros dependientes. El guardado actualiza la ficha, el progreso y las estimaciones en una transacción. Las posiciones pueden quedar discontinuas después de quitar un juego. El puesto visible se calcula a partir del orden efectivo. La base no guarda porcentajes temporales, totales ni tiempo restante porque el dominio los deriva de los minutos y el objetivo.

Los estados, prioridades y objetivos se almacenan con su nombre de enum. Si alguno se renombra, preserva el valor almacenado o migra la columna. Los cambios de versión requieren un esquema exportado en `app/schemas` y una migración. Las partidas, el histórico, el catálogo de géneros y plataformas y las bandas sonoras se añadirán cuando sus flujos estén implementados. DataStore guarda las horas de juego diarias por separado.

Android tiene desactivada la copia automática de los datos privados de la app. La fase 7 incorporará la exportación explícita a JSON desde el selector de documentos. [Reglas de copia de Android](https://developer.android.com/identity/data/autobackup)
