# Diseño de Playcrastination

La identidad usa azul noche, violeta y cian. La marca combina un hexágono con un símbolo de reproducción. El motivo orbital se dibuja en Compose y se usa en la pieza principal de Inicio y como sustitución visual cuando falta una imagen.

| Uso | Color |
| --- | --- |
| Fondo | `#0B0F19` |
| Navegación | `#111827` |
| Superficie | `#1B2435` |
| Violeta de marca | `#8B5CF6` |
| Violeta de texto y controles | `#C4B0FF` |
| Cian | `#22D3EE` |
| Texto y texto secundario | `#F1F5F9` · `#94A3B8` |
| Completado, aviso y error | `#22C55E` · `#F59E0B` · `#F87171` |

El violeta claro y el rojo claro se usan en elementos pequeños para reforzar el contraste. Las tarjetas combinan bordes discretos, degradados y esquinas de 20 dp. El sistema usa la tipografía sans del dispositivo, con tamaños centrales de 40 sp para la pieza principal, 32 sp para pantalla y 22 sp para títulos de sección. La barra inferior da acceso a las cinco secciones. Las portadas se disponen en una cuadrícula adaptativa y el contenido limita su ancho a 820 dp.

La ficha separa progreso personal, duraciones por objetivo e información. La cabecera puede mostrar una ilustración horizontal, con un degradado que protege la legibilidad del título. El origen se indica como manual cuando la estimación la ha introducido el usuario. Las transiciones y el progreso animado toman su duración de los tokens del tema.

El historial de partidas usa las mismas tarjetas de la ficha y deja siempre visible la acción de guardar en el editor principal. Biblioteca agrupa facetas y ordenación en un cuadro de diálogo. El buscador y el selector de portadas o lista permanecen en la cabecera.

En Backlog, el tirador cian permite arrastrar y la línea del mismo color anticipa dónde se soltará la ficha. El menú contextual ofrece subir y bajar para controlar el orden sin arrastrar. Las previsiones se distinguen de las fechas registradas de cada partida por su etiqueta de inicio y fin estimados. [Gestos en Compose](https://developer.android.com/develop/ui/compose/touch-input/pointer-input/understand-gestures)

Las referencias del encargo orientan la distribución de la información. Clair Obscur y las cifras del ejemplo sirven como caso de prueba de cálculo y no se precargan en la biblioteca.
