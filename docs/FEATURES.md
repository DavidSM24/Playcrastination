# Especificación funcional

## Recorrido local inicial

- **Inicio** muestra métricas de biblioteca, juegos en curso, los siguientes títulos de la cola y The Pile of Shame. La previsión aparece cuando se conocen todos los tiempos aplicables.
- **Biblioteca** permite alta manual, búsqueda por título, filtro de favoritos y vistas de portadas o lista.
- **Detalle** presenta portada, cabecera con parallax, información personal, estado, objetivo, tiempos y tres estimaciones manuales. Permite editar, añadir o quitar de la cola y eliminar con confirmación.
- **Backlog** presenta orden de inserción, objetivo en tiempo y saldo conocido. La eliminación de una posición no elimina el videojuego de la biblioteca.
- **Estadísticas** muestra minutos registrados y proporción de juegos completados. Los gráficos históricos quedan pendientes de datos registrados.
- **Ajustes** permite conservar un presupuesto de 1 a 24 horas de juego al día.

## Biblioteca y detalle

- La biblioteca ofrecerá cuadrícula y lista, búsqueda instantánea, filtros combinados por género, plataforma, estado, año y favoritos, y ordenación por título, horas, duración, progreso, actividad o lanzamiento. Las fichas distinguirán el origen Steam del alta manual y permitirán ocultar títulos importados de la biblioteca activa.
- Se podrán registrar cualquier plataforma y editar los campos que carezcan de información externa. La ficha completa incorporará nombre original, descripción, géneros, plataformas, fecha de lanzamiento, desarrollador, distribuidor e imágenes.
- Cada ficha distinguirá minutos acumulados en Steam, tiempo de la partida actual, minutos manuales y adicionales, progreso narrativo, logros y estado de finalización. La valoración, notas, favoritos, prioridad y objetivo son decisiones personales.
- La ficha mostrará el tiempo estimado y restante de cada objetivo, con su fuente y el objetivo seleccionado resaltado. Las acciones incluirán editar, cambiar estado, sincronizar horas, actualizar estimaciones, administrar bandas sonoras, gestionar la cola, abrir Steam o HowLongToBeat y eliminar la ficha.

## Backlog e Inicio

- Añadir o sacar un juego de la cola será independiente de poseerlo o importarlo. La cola permitirá arrastrar, ajustar prioridad y objetivo, pasar a Jugando, pausar y volver al backlog, ordenar temporalmente por duración y recuperar el orden manual.
- El calendario calculará fechas de inicio y final previstas con el orden de la cola, tiempo pendiente de los juegos activos y horas diarias. Las duraciones desconocidas quedarán identificadas, junto con el subtotal de tiempos conocidos.
- Inicio incorporará el perfil y avatar de Steam, botón y hora de sincronización, recuentos de biblioteca, pendientes, activos y completados, horas registradas y horas pendientes. Las tarjetas de juegos en curso mostrarán plataforma, género, objetivo, horas y progreso. The Pile of Shame concentra la estimación global y el tono humorístico.

## Estadísticas y ajustes

- Las estadísticas mostrarán cambios observados durante la semana, mes y año, juegos por estado, distribución de géneros y plataformas, títulos más jugados, duración media de juegos terminados y evolución de la cola. Se mostrarán estados vacíos mientras falten registros históricos suficientes.
- Los ajustes incorporarán conexión e importación de Steam, frecuencia de sincronización, preferencias de coincidencias de duración, audio y autoplay, idioma, tema, objetivo por defecto, visualización, animaciones y bajo consumo de datos. También ofrecerán exportar e importar JSON, limpiar caché, restaurar preferencias y eliminar datos con confirmación.
- La biblioteca, notas, progreso, estados, valoraciones, cola y estadísticas ya registradas se podrán consultar o modificar offline cuando corresponda. Las consultas remotas y la reproducción de YouTube requerirán conexión. Las imágenes podrán mostrarse desde caché.

La aplicación está destinada al uso personal y la distribución mediante APK. El diseño reserva el humor y las celebraciones para momentos puntuales y mantiene la gestión de la colección como tarea principal.
