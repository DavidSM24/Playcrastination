# Plan de desarrollo

| Fase | Entrega | Validación |
| --- | --- | --- |
| 1. Infraestructura | Proyecto Android, Gradle Wrapper, Hilt, Room v1, repositorios, navegación, identidad visual y recorrido local de verificación. | Compilar APK, ejecutar pruebas de progreso y verificar las operaciones de Room. |
| 2. Biblioteca local | Completar modelo y editor, búsqueda, filtros, ordenación, gestión de partidas, valoraciones y detalle. | Probar operaciones offline y conservación de datos durante las actualizaciones. |
| 3. Backlog | Reordenación por arrastre, orden temporal, prioridades, transiciones de estado y cálculo de fechas por juego. | Probar orden persistente, duraciones ausentes y cambios concurrentes. |
| 4. Steam | Keystore, Retrofit, importación seleccionable, metadatos, WorkManager y snapshots. | Simular lotes, fallos HTTP, duplicados y preservación de cambios manuales. |
| 5. HowLongToBeat | Investigar acceso permitido, mantener edición manual, añadir búsqueda y coincidencias si existe un método admisible. | Probar homónimos, distintas ediciones y pérdida de disponibilidad. |
| 6. Bandas sonoras | Cola de búsqueda y validación, selección, reproducción visible y gestión del ciclo de vida. | Probar resultados ambiguos, cuotas, reproducción manual y pausa por invisibilidad. |
| 7. Estadísticas y configuración | Histórico, gráficas, ajustes completos, exportación e importación JSON y accesibilidad del idioma. | Probar restauración, validación de formato, exclusión de claves y datos históricos reales. |
| 8. Pulido | Rendimiento, animaciones, accesibilidad, Android 8 y APK. | Ejecutar la batería de pruebas y comprobar la instalación en dispositivos. |

Al terminar cada fase revisa compilación y pruebas, corrige regresiones y actualiza el estado real. El alcance de una fase se verifica antes de iniciar la siguiente.
