# Arquitectura

El proyecto usa un módulo Android `app`. Los paquetes separan responsabilidades y permiten añadir módulos Gradle cuando haya una necesidad concreta.

| Paquete | Responsabilidad |
| --- | --- |
| `domain.model` | Videojuegos, partidas, estados, objetivos, catálogo filtrable, progreso temporal y previsión de final de la cola. |
| `domain.repository` | Contratos de la biblioteca y preferencias. |
| `core.database` | Esquema Room, relaciones y DAO, que la capa de datos utiliza para persistir y observar cambios. |
| `data.mapper` y `data.repository` | Conversión entre persistencia y dominio, escrituras transaccionales y preferencias DataStore. |
| `di` | Configuración de Hilt. |
| `core.navigation` y `core.designsystem` | Navegación, tema y componentes Compose reutilizables. |
| `feature` | Pantallas y ViewModels. Las pantallas reciben datos de presentación y emiten eventos. |

Los DAO publican `Flow` y los ViewModels exponen `StateFlow` observado con el ciclo de vida. La interfaz muestra los cambios devueltos por Room después de cada transacción. El progreso temporal se calcula en el dominio. El estado de finalización y el progreso narrativo se guardan como datos personales independientes.

La biblioteca construye un índice local de búsqueda a partir de la biblioteca observada y aplica filtros y ordenación fuera del hilo de interfaz. El catálogo contiene las claves normalizadas, las opciones de los filtros y los datos numéricos de ordenación. Las vistas de portadas y lista comparten la consulta y conservan su posición de scroll.

La ficha usa la partida más reciente como partida actual, y cada partida conserva su tiempo y su progreso narrativo. El repositorio valida la identidad de la partida actual dentro de la transacción de guardado para impedir que una ficha desactualizada sobrescriba una partida nueva.

El dominio calcula un mismo calendario para Backlog e Inicio. Dentro de los juegos activos se atiende primero la última fecha de juego registrada y después el ID como desempate. Cada fecha posterior consume el presupuesto diario restante. El ajuste de horas usa la preferencia de DataStore que comparten Backlog, Inicio y Ajustes.

El mecanismo de restauración de Navigation Compose conserva el estado de cada sección. Las consultas remotas de imágenes pasan por Coil y no condicionan la carga de contenido local.

`SteamRepository` separa la capa de presentación de la Web API. Los contratos de la fuente Steam y del proveedor de tienda son independientes de Retrofit. Room publica las fichas y perfiles locales. Una exclusión mutua coordina importaciones y sincronizaciones y cada lote se confirma mediante una sola transacción. WorkManager delega la sincronización al mismo repositorio que utiliza la interfaz.
