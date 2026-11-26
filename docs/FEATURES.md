# Especificación funcional

## Recorrido local inicial

- **Inicio** muestra métricas de biblioteca, juegos en curso, los siguientes títulos de la cola y The Pile of Shame. La previsión aparece cuando se conocen todos los tiempos aplicables.
- **Biblioteca** permite alta manual, búsqueda por título, filtro de favoritos y vistas de portadas o lista.
- **Detalle** presenta portada, cabecera con parallax, información personal, estado, objetivo, tiempos y tres estimaciones manuales. Permite editar, añadir o quitar de la cola y eliminar con confirmación.
- **Backlog** muestra el orden guardado, los objetivos en tiempo, los saldos y las fechas previstas. Retirar un juego de la cola conserva su ficha y sus partidas en la biblioteca.
- **Estadísticas** muestra minutos registrados y proporción de juegos completados. Los gráficos históricos quedan pendientes de datos registrados.
- **Ajustes** permite conservar un presupuesto de 1 a 24 horas de juego al día.

## Biblioteca y detalle local

- La biblioteca ofrece cuadrícula y lista, búsqueda instantánea por título y título original, filtros combinables por género, plataforma, estado, año y favoritos. Es posible ordenar por título, tiempo acumulado, duración del objetivo, progreso de la partida actual, última fecha registrada de juego y lanzamiento. Los valores desconocidos van al final en ambos sentidos de ordenación.
- La ficha permite registrar cualquier plataforma, el título original, la descripción, los géneros, la plataforma jugada, las plataformas disponibles, el lanzamiento, el desarrollador, el distribuidor, la portada, la cabecera y el fondo.
- Cada partida registra sus minutos, fechas y progreso narrativo de forma independiente. Se puede desglosar el tiempo de forma opcional. La partida actual determina el progreso temporal y el tiempo restante. Se pueden consultar y corregir partidas anteriores, mientras que biblioteca, Inicio y Estadísticas reúnen los tiempos registrados de todas las partidas.
- El objetivo seleccionado resalta su estimación manual. Se pueden cambiar el estado, el objetivo, las estimaciones, la valoración de 1 a 10, las notas, los favoritos y la prioridad. También se puede gestionar la cola, abrir HowLongToBeat o eliminar la ficha con confirmación.
- Las fichas importadas de Steam muestran su procedencia, horas acumuladas y actividad reciente, con logros cuando Steam los proporciona. Permiten abrir la tienda, completar metadatos, sincronizar y ocultar o volver a mostrar el título en la biblioteca activa. La gestión de bandas sonoras llegará con el reproductor.

## Backlog e Inicio

- La cola permite añadir desde la biblioteca, ordenar por arrastre o con los comandos de subir y bajar, ajustar prioridad y objetivo, pasar a Jugando y devolver el juego al backlog al pausarlo. Las vistas de duraciones más cortas o largas conservan el orden manual, que se recupera al seleccionarlo de nuevo.
- Las previsiones usan de 1 a 24 horas diarias y asignan el presupuesto completo a partir de hoy. Reservan primero el tiempo de los juegos en curso. Los títulos con tiempo desconocido muestran el inicio si puede estimarse. Su fin y las fechas que dependen de él quedan pendientes. La cola mantiene el subtotal de los tiempos conocidos.
- Inicio muestra recuentos de biblioteca, pendientes, activos y completados, horas registradas y horas pendientes. Las tarjetas de juegos en curso muestran plataforma, género, objetivo, horas y progreso. **A continuación** sigue el orden manual de la cola. **The Pile of Shame** reúne el saldo y el fin estimado. Cuando Steam está conectado, Inicio muestra la información del perfil que Steam devuelve y ofrece la sincronización por botón o gesto.

## Estadísticas y ajustes

- Las estadísticas mostrarán cambios observados durante la semana, mes y año, juegos por estado, distribución de géneros y plataformas, títulos más jugados, duración media de juegos terminados y evolución de la cola. Se mostrarán estados vacíos mientras falten registros históricos suficientes.
- Los ajustes incorporarán conexión e importación de Steam, frecuencia de sincronización, preferencias de coincidencias de duración, audio y autoplay, idioma, tema, objetivo por defecto, visualización, animaciones y bajo consumo de datos. También ofrecerán exportar e importar JSON, limpiar caché, restaurar preferencias y eliminar datos con confirmación.
- La biblioteca, notas, progreso, estados, valoraciones, cola y estadísticas ya registradas se podrán consultar o modificar offline cuando corresponda. Las consultas remotas y la reproducción de YouTube requerirán conexión. Las imágenes podrán mostrarse desde caché.

La aplicación está destinada al uso personal y la distribución mediante APK. El diseño reserva el humor y las celebraciones para momentos puntuales y mantiene la gestión de la colección como tarea principal.
