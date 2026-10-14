# AnimeTV 0.13.0

## Pantalla y mando

Sin margen exterior arriba, abajo o a la derecha. El único margen de pantalla es fijo, de 32 dp, a la izquierda del menú. Se elimina la opción de márgenes. La alineación vertical reserva espacio para mostrar el encabezado completo cuando se enfoca la primera portada; el encabezado continúa desplazándose con la lista.

Cuando el foco está en la barra de tiempo del reproductor, izquierda y derecha saltan 30 segundos por pulsación. El valor se cambia en Ajustes → Salto en la barra de tiempo. Los botones de retroceder/adelantar conservan sus ajustes independientes.

## Capítulos

Dos iconos juntos: flecha para alternar el orden y selector de miniaturas/lista compacta. Se recuerda el modo elegido. La lista compacta muestra el nombre del capítulo, progreso y marca de visto, sin cargar su miniatura.

Con 100 capítulos o menos no aparecen bloques. Desde 101, el selector muestra rangos de hasta 100. Pulsar un rango lo expande; volver a pulsarlo lo contrae. Se mantiene un solo bloque expandido para limitar el trabajo y memoria de la TV. Descendente abre inicialmente el último bloque. La reproducción conserva los índices originales de episodios.

## Navegación y caché

Inicio, Catálogo y Calendario reutilizan sus listas cargadas y selección durante cinco minutos. Se guardan como máximo tres apartados; los datos del catálogo tienen una caché de seis consultas, también de cinco minutos. Inicio se reconstruye si cambia el historial. Los errores no se guardan en caché. Favoritos y Ajustes leen los datos locales actuales.

La caché se libera si Android avisa de presión de memoria. Se conservan la carga de imágenes fuera del hilo principal y la virtualización de filas de Leanback. No se ha medido el rendimiento en la GPU de la Xiaomi; la mejora de micro pausas requiere comprobar el APK allí.

## Servidores

Solo en el selector del capítulo: Voe · 720p, Byse · 1080p y MP4Upload · 1080p. Estas etiquetas son calidades habituales, no una detección de la resolución de cada archivo. El selector de pistas sigue ofreciendo únicamente resoluciones reales.

MP4Upload dispone de 25 segundos de arranque. Si Media3 indica que ninguna pista de video es compatible, se informa específicamente; si la pista es AV1, se incluye ese dato. Un timeout de carga y la ausencia de fotogramas tienen mensajes diferentes. Se conservan la selección manual y el respaldo automático.

Esto no añade un decodificador AV1 por software ni garantiza resolver la pantalla negra en Android 9. Se necesita revisar el informe de reproducción del episodio que falle para distinguir compatibilidad, conexión y renderizado.

Incluye los cambios del primer bloque 0.12.0: esquinas, play circular, transporte centrado y botón de salto de canción.
