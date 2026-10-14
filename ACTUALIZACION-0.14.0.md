# AnimeTV 0.14.0 — rediseño completo

La referencia visual son las propuestas aprobadas, con fondo carbón, paneles discretos y acento azul. Las etiquetas de tipo y estado están alineadas a la derecha en todas las portadas.

- Inicio: tarjetas horizontales en Seguir viendo, progreso guardado y tiempo restante; portadas para los capítulos recientes.
- Catálogo: cuadrícula de hasta seis columnas según el ancho disponible, búsqueda y filtros accesibles con el mando. Derecha desde el menú conserva el acceso directo a la primera portada.
- Calendario: pestañas de lunes a domingo; cambiar el día filtra los datos cargados.
- Favoritos: categorías, Todos, creación y gestión en una misma barra horizontal.
- Ajustes: filas con nombre y valor, selección de segundos con pasos de 1 o 5, guardado inmediato y panel lateral de opciones avanzadas.
- Ficha: imagen horizontal de fondo, sinopsis de dos líneas ampliable, reproducción/reanudación y favorito. Miniaturas o lista compacta, orden mediante flechas y bloques expandibles desde 101 episodios.
- Reproductor: degradado negro, progreso azul, controles centrales y acceso a +85 seg (o el tiempo configurado), Episodios y opciones.
- Servidores: diálogo compacto, calidad junto al nombre y opciones desplazables sin perder el botón Cerrar.
- Filtros y opciones de reproducción: paneles laterales derechos, con desplazamiento cuando hacen falta más opciones.
- Identidad: gato de pelaje crema, ojos azules y tres rayas en la frente; transparente dentro de la app y sobre fondo oscuro en el launcher de Android TV, con el nombre AnimeTV.

El menú muestra iconos y se expande cuando recibe foco. Los iconos conservan proporción cuadrada y respetan el espacio interior al dibujarse.

Se conserva el paquete pe.animetv, la firma de desarrollo, favoritos, categorías, ajustes, progreso y comportamiento de servidores. Esta actualización cambia la interfaz; no incorpora nuevas fuentes ni resuelve las limitaciones de decodificación AV1 de MP4Upload en una TV sin soporte compatible.

## Instalación

Instalar AnimeTV-0.14.0-debug.apk sobre la versión anterior. No desinstalar para conservar los datos.

## Verificación

Compilación, pruebas automatizadas y lint del proyecto. Las pruebas de interfaz usan Android 9/API 28, pantalla de 960 × 540 dp, navegación con mando y gráficos nativos de Robolectric. Las capturas de comprobación usan datos de prueba y portadas vacías, no una fuente real.

La reproducción y el aspecto final en la Xiaomi requieren comprobarse en la TV; no se dispone de un dispositivo conectado.
