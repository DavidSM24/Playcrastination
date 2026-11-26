# Arquitectura

El proyecto usa un módulo Android `app`. Los paquetes separan responsabilidades y permiten añadir módulos Gradle cuando haya una necesidad concreta.

| Paquete | Responsabilidad |
| --- | --- |
| `domain.model` | Videojuegos, estados, objetivos, progreso temporal y previsión de final de la cola. |
| `domain.repository` | Contratos de la biblioteca y preferencias. |
| `core.database` | Esquema Room, relaciones y DAO, que la capa de datos utiliza para persistir y observar cambios. |
| `data.mapper` y `data.repository` | Conversión entre persistencia y dominio, escrituras transaccionales y preferencias DataStore. |
| `di` | Configuración de Hilt. |
| `core.navigation` y `core.designsystem` | Navegación, tema y componentes Compose reutilizables. |
| `feature` | Pantallas y ViewModels. Las pantallas reciben datos de presentación y emiten eventos. |

Los DAO publican `Flow` y los ViewModels exponen `StateFlow` observado con el ciclo de vida. La interfaz muestra los cambios devueltos por Room después de cada transacción. El progreso temporal se calcula en el dominio. El estado de finalización y el progreso narrativo se guardan como datos personales independientes.

El mecanismo de restauración de Navigation Compose conserva el estado de cada sección. Las consultas remotas de imágenes pasan por Coil y no condicionan la carga de contenido local. Los contratos concretos de proveedores externos se incorporarán con la primera implementación que los consuma.
