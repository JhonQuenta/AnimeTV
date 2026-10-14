# Verificación 0.19.0

Informe final: AnimeTV-0.19.0-verificacion.json. Estado real de Latanime y procedencia de logos: AnimeTV-0.19.0-red.txt y SOURCE_LOGOS.md. Se comprueban alineación del buscador, fuente única, geometría de la ficha, categorías y preferencias independientes, idiomas separados y tiempos en extremos. Solo se genera ARM32; el APK se firma con el mismo certificado. La validación física en TV queda a cargo de la prueba del usuario.

# Verificación 0.18.0

Resultados reales de red y filtros: AnimeTV-0.18.0-red.txt. Informe de tests, lint, arquitecturas y hashes: AnimeTV-0.18.0-verificacion.json. El catálogo dinámico de AnimeLatinoHD se verificó en navegador de escritorio; su renderizado WebView en TV física requiere prueba. Las pruebas automatizadas cubren intersección de filtros y paginación, relaciones explícitas, prioridad de saga, cabeceras de portadas, selector por mando y velocidades guardadas.

## Historial

# Verificación 0.17.0

AnimeLatinoHD: datos públicos de React Flight, directorio/filtros/calendario, capítulos reales, SUB/LAT/ESP sin mezclar enlaces, Delta/Epsilon, control por mando del puntero y dominios de verificación. En la prueba de red de extractores, las ocho muestras de Tempal 3 y Azumanga 26 entregaron HLS con segmento accesible. Ver AnimeTV-AnimeLatinoHD-0.17.0-red.txt.

La navegación del sitio funcionó en el navegador de escritorio tras la verificación de Cloudflare. El flujo WebView, el mando y la reproducción en la TV física siguen pendientes; los tests del puntero no prueban que Cloudflare acepte un determinado motor de WebView.

Informe final: AnimeTV-0.17.0-verificacion.json.

## Verificación histórica 0.16.0

Latanime: catálogo, búsqueda con doblajes agrupados, filtros, calendario, últimos capítulos y servidores. Se comprueban números de episodio reales, LAT/CAST separados en el selector, restauración sin caché, temporadas/años distintos y Referer de la versión elegida. Se comprueba también el lector iterativo con una carga grande de script, sin ejecución de JavaScript.

Las cuatro prioridades del usuario superaron comprobaciones de red en La Caja Azul y Frieren. El flujo agrupado real devuelve LAT/CAST: Dsvplay en ambos doblajes de La Caja Azul; Filemoon/Lulu en ambos de Frieren, Vide0 latino y Do7go castellano. Los resultados de red y límites están en AnimeTV-Latanime-0.16.0-red.txt y ACTUALIZACION-0.16.0.md. Reproducción física de esta versión: pendiente.

Informe final de tests, compilación, lint, APK, arquitecturas y hashes: AnimeTV-0.16.0-verificacion.json.

## Verificación histórica 0.15.1

Se prueban selector superior con mando, tarjetas de historial con fuentes distintas, identificador en ficha, preferencias por fuente y migración. La geometría mantiene portada y acciones estables sin reservar espacio para una segunda línea de título inexistente. Capturas locales en outputs/redesign-verification.

La respuesta Mega del capítulo Magical★Explorer 3 tardó 21,66 s, por encima del anterior límite de 10 s. Las comprobaciones de red accedieron por HTTPS a rangos inicial/final y descifraron el inicio MP4 AVC. YourUpload: HTTP 206 y AVC 1280x720. Se agrega una prueba real de respuesta API con cuerpo retrasado 12 segundos para comprobar el nuevo límite. La nueva reproducción física de Mega está pendiente.

Informe final de tests, compilación, lint, APK, arquitecturas y hashes: AnimeTV-0.15.1-verificacion.json.

## Verificación histórica 0.15.0

Pruebas de catálogo/ficha/servidores con HTML real de Tioanime, calendario futuro por id de portada, persistencia del selector, origen de favoritos y descifrado AES-CTR con lecturas parciales/saltos. El adaptador Media3/OkHttp se prueba con HTTP 206. Informe de compilación, tests, lint, APK, arquitecturas y hashes: AnimeTV-0.15.0-verificacion.json.

Prueba de red con código de la app: Voe HLS 720p (lista y segmento), YourUpload MP4 y Mega MP4 descifrado, ambos con rangos HTTP 206 inicial y a 1 MiB. Ver AnimeTV-Tioanime-0.15.0-red.txt. Esta verificación no confirma decodificación ni rendimiento en una TV física.

## Verificación histórica 0.14.10

Las pruebas abarcan las etiquetas visibles de HLS/MP4Upload, el orden de renderers y la caché HTTP de imágenes, además de extracción, controles y navegación. El informe AnimeTV-0.14.10-verificacion.json registra los resultados finales de testDebugUnitTest, assembleRelease y lintRelease, tamaños, hashes y arquitecturas.

La entrega release elimina código y recursos sin uso con R8 y conserva miembros JNI. Incluye APK universal ARM (32/64 bits) y APK específicos para cada arquitectura. Firma de desarrollo igual a la anterior; no se incluyen bibliotecas Intel. La caché de portadas respeta Cache-Control y no-store, sin reducir resolución ni calidad.

Prueba física del APK optimizado: pendiente. En 0.14.8 el usuario confirmó que HLS carga imagen/audio y permite avanzar rápido, pero AV1 1080p por software presenta fuertes tirones en la Xiaomi MiTV-MSSP3. MP4Upload y futuros dispositivos con AV1 por hardware siguen pendientes de prueba.

## Verificación histórica 0.14.8

110 pruebas sin fallos, errores ni omitidas. `testDebugUnitTest`, `assembleDebug` y `lintDebug`: BUILD SUCCESSFUL. Se comprueba orden de renderers (Android antes de FFmpeg), cabeceras de segmentos a través del adaptador Media3, detección de errores en inicialización/fragmentos y MIME HLS para enlaces sin extensión. El APK incluye libdav1d en libavcodec para armeabi-v7a; minSdk 23. Informe detallado: AnimeTV-0.14.8-verificacion.json.

El extractor Java/OkHttp accedió con HTTP 200 a inicialización y segmentos 000, 020 y 060 del capítulo 2 proporcionado. Las capturas AIDA64 de la Xiaomi no incluyen AV1, por lo que esta versión añade decodificación AV1 de software para HLS. Aún no se ha probado la reproducción ni su rendimiento en la TV física. No se afirma reproducción fluida a partir de pruebas HTTP o de Robolectric.

## Verificación histórica 0.13.0

79 pruebas pasan; testDebugUnitTest, assembleDebug y lintDebug completados.

La versión actual verifica reutilización de listas y foco entre apartados, encabezado completo al enfocar la primera portada, margen izquierdo fijo, vista compacta de episodios, bloques expandibles desde 101, saltos configurables de la barra de tiempo y etiquetas de calidad habitual. Las pruebas de compatibilidad AV1 distinguen pistas no soportadas de pistas soportadas sin concluir nada cuando aún no existen pistas. Gradle ejecuta testDebugUnitTest, assembleDebug y lintDebug.

No se ha comprobado la decodificación MP4Upload en la Xiaomi ni se ha medido su GPU. Esta versión no incorpora decodificador AV1 por software. Ver ACTUALIZACION-0.13.0.md para cambios y límites.

## Verificación histórica 0.10.0

62 pruebas automáticas pasan en API 28 / viewport 1920 × 1080. Gradle testDebugUnitTest, assembleDebug y lintDebug completados.

- Encabezados como primera fila desplazable en Inicio, Calendario, Catálogo y Ajustes, con margen de menú 0%, 2%, 5% y 10%; salen del viewport al enfocar las filas inferiores.
- Márgenes exteriores del contenido en cero; solo el menú recibe margen izquierdo. Navegación hacia abajo y restauración de foco verificadas; primera tarjeta a la izquierda devuelve al menú.
- Tarjetas de capítulos 16:9, nombre dentro de la imagen y play visible solo al recibir foco. Tarjetas de anime 2:3, recorte de todos los hijos al contorno redondeado y etiquetas con ancho según texto.
- Reproductor simplificado: tres controles de transporte, Episodios y tuerca; pruebas de pausa, reanudación y búsqueda temporal nativa.
- Fuente, resolución de Voe/Byse, selección de calidad real, caché y políticas de reproducción conservan sus pruebas.

No hay TV física conectada. Las pruebas de interfaz usan Robolectric, no medición de fluidez de la GPU Xiaomi. El estado puede requerir una primera consulta adicional cuando el listado omite el dato. El fondo reutiliza la imagen horizontal de recientes guardada por anime; tiene alternativa si falta. No se incorporó detección automática de canciones. Se probaron las solicitudes de HLS y las cabeceras de video MP4Upload de dos episodios; resultados y limitaciones en REVISION-SERVIDORES.md.


Comprobaciones nuevas:
- Ventanas de capítulos para 0, 1, 99, 100, 101, 200 y 1151 elementos, sin pérdidas ni duplicados.
- Ficha con 205 capítulos: 100 tarjetas iniciales; último bloque con 5. Al invertir el orden se abre el episodio 205 con índice original 204 y lista de reproducción completa.
- Ficha con exactamente 100 capítulos sin selector de bloques.
- Catálogo: derecha desde menú enfoca la primera portada, arriba alcanza búsqueda y abajo regresa a la portada.


## JKAnime 0.20.0, preparación sin APK

208 pruebas automatizadas aprobadas; lint sin errores. Comprobaciones de red y enlaces individuales en VERIFICACION-JKANIME.txt. Registro resumido en VERIFICACION-JKANIME.json. No se ha generado un APK; el APK previo 0.19.0 conserva exactamente su SHA-256. Prueba física pendiente.


## Entrega final 0.20.0

222 pruebas aprobadas y lint sin errores. APK armeabi-v7a, certificado idéntico al APK anterior, versión 33. Incluye JKAnime, correcciones de fichas/estados y limpieza de Seguir viendo. Prueba de pulsación corta/larga, repetición, cancelación, foco, liberación del botón, eliminación individual/completa y persistencia del progreso. No se generó APK universal ni ARM64. Ver AnimeTV-0.20.0-verificacion.json.


## Optimización 0.21.0 preparada

238 pruebas aprobadas y lint sin errores. Recuperación real de segmento HLS (CDN lento 43.063 ms; réplica validada 3.440 ms), caché de búsqueda, cancelación de sockets, paralelismo limitado y sugerencias progresivas. Avisos de salto acumulativos verificados. Sin generar APK; el APK 0.20.0 conserva exactamente su SHA-256. Prueba en la TV pendiente. Ver VERIFICACION-0.21.0.json y VERIFICACION-RED-0.21.0.txt.


## 0.22.0 preparada, sin APK

250 pruebas aprobadas; lint: 0 errores, 53 advertencias. Puntuaciones Jikan en Catálogo, caché persistente, limitación/cancelación y comprobación visual. Consultas reales Frieren/SAO: Connect timed out; éxito de servicio y TV pendientes. El APK 0.20.0 sigue intacto. Ver VERIFICACION-0.22.0.json y ACTUALIZACION-0.22.0.md.


## 0.23.0 preparada, sin APK

257 pruebas aprobadas; lint: 0 errores, 56 advertencias. Logos oficiales más nítidos, Cancelar centrado y fijo, portadas verticales de Latanime y fondos independientes. Latanime: DNS no disponible, comprobado con HTML real guardado y pruebas locales. APK publicado 0.20.0 intacto; prueba física en TV pendiente. Ver ACTUALIZACION-0.23.0.md y VERIFICACION-0.23.0.json.


## 0.24.0 preparada, sin APK

265 pruebas aprobadas; lint: 0 errores, 56 advertencias. Eventos de mando, filtros y precarga, cancelación real y sustitución de imágenes comprobados. Sin reducción de calidad ni generación de APK. APK 0.20.0 intacto. Prueba física en TV pendiente. Ver ACTUALIZACION-0.24.0.md y VERIFICACION-0.24.0.json.


## 0.25.0 preparada, sin APK

274 pruebas aprobadas; lint: 0 errores, 56 advertencias. Paginación real confirmada, regreso desde páginas vacías/con error, foco del mando y búsqueda/filtros conservados. APK publicado 0.20.0 intacto; prueba física en TV pendiente. Ver ACTUALIZACION-0.25.0.md y VERIFICACION-0.25.0.json.


## 0.26.0 preparada, sin APK

288 pruebas aprobadas; lint: 0 errores, 56 advertencias. Respaldo AniList comprobado con solicitudes reales, portadas ALHD, fondo al borde, selector compacto, búfer limitado y exclusión por formato real. Archivo APK 0.25.0 conservado. Validación de reproducción física en TV pendiente. Ver ACTUALIZACION-0.26.0.md y VERIFICACION-0.26.0.json.


## 0.27.0 preparada, sin APK

293 pruebas aprobadas, lint sin errores (56 advertencias). Separación real de capítulos, foco del mando, ancho del tipo, margen derecho de controles y fecha/hora local. APK 0.26.0 intacto. Ver ACTUALIZACION-0.27.0.md y VERIFICACION-0.27.0.json.


## 0.29.0 preparada, módulos internos

279 pruebas aprobadas. Lint sin errores en app y las siete bibliotecas. R8 release correcto, sin empaquetar APK. Las bibliotecas no referencian clases de la app. IDs, contratos de servidor y precarga verificados; todas las pruebas de comportamiento anteriores conservadas. APK entregado 0.27.0 intacto. Ver ARCHITECTURE.md y VERIFICACION-0.29.0.json.
