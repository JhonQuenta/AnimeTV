# Historial de desarrollo

Este documento conserva notas antiguas. Para el comportamiento y la instalación actuales, consulta README.md.

# Módulos internos 0.29.0 preparados, sin APK nuevo

Cinco módulos de fuente, `extractors` compartido y contrato/utilidades en `source-core`; interfaz, reproducción e historial permanecen en `app`. Sin estrellas ni instalación dinámica de extensiones. Ver ARCHITECTURE.md y ACTUALIZACION-0.29.0.md. APK entregado más reciente: 0.27.0.

# Versión 0.28.0 preparada, sin estrellas ni APK nuevo

Retiradas las puntuaciones del catálogo y fichas, los clientes de Jikan/AniList y su cola/caché. Se mantienen los últimos ajustes de diseño y reproducción. Ver ACTUALIZACION-0.28.0.md.

# APK 0.27.0 entregado para la TV

Disponible `../AnimeTV-0.27.0-armeabi-v7a.apk`, únicamente ARM de 32 bits. Compilación, firma y arquitectura verificadas; misma firma que 0.26.0. 293 pruebas aprobadas y lint sin errores. Ver ENTREGA-0.27.0.md.

# Ajustes 0.27.0

Separación visible entre episodios en lista, Serie ajustada al texto, margen derecho del buscador alineado con fuente y fecha/hora de publicación legibles en la zona de la TV. Ver ACTUALIZACION-0.27.0.md.

# APK 0.26.0 entregado para la TV

Disponible `../AnimeTV-0.26.0-armeabi-v7a.apk`, únicamente ARM de 32 bits. Compilación, firma y arquitectura verificadas. Validación previa: 288 pruebas aprobadas y lint sin errores. Ver ENTREGA-0.26.0.md. La reproducción debe comprobarse en la TV.

# Correcciones 0.26.0

Portadas reales de AnimeLatinoHD, fondos hasta los bordes, fuentes compactas, respaldo AniList para notas con Jikan inaccesible y mayor búfer para JKAnime/MP4Upload. Detección de AV1 sobre el formato real; sin ocultar HLS/H.264 por su nombre. Ver ACTUALIZACION-0.26.0.md.

# APK 0.25.0 entregado para la TV

Disponible `../AnimeTV-0.25.0-armeabi-v7a.apk`, únicamente ARM de 32 bits. Compilación, firma y arquitectura verificadas. Incluye las mejoras 0.21–0.25; 274 pruebas aprobadas y lint sin errores. Ver ENTREGA-0.25.0.md. Prueba física en TV pendiente.

# Paginación y puntuación en ficha 0.25.0

Estrella y nota MyAnimeList también en la ficha, compartiendo la caché del Catálogo. Siguiente solo tras confirmar más resultados, Anterior disponible también en páginas vacías o con error y Atrás del mando regresa por páginas conservando búsqueda/filtros. Comprobación asíncrona que se retoma al volver desde una ficha; JKAnime reutiliza su búsqueda completa. Incluye 0.21.0–0.24.0. Ver ACTUALIZACION-0.25.0.md.

# Mando y carga 0.24.0 preparados, sin APK

Recorrido con mando en el selector de fuentes, filtros sin solicitudes/ventanas duplicadas, precarga del episodio a reanudar separada del catálogo y cancelación de imágenes que dejan de necesitarse. Sin reducir resolución, filtros ni resultados. Incluye 0.21.0–0.23.0. Ver ACTUALIZACION-0.24.0.md.

# Imágenes 0.23.0 preparadas, sin APK

Logos oficiales de mejor resolución, Cancelar centrado, portada vertical correcta en las tarjetas recientes de Latanime y fondos de ficha independientes de las imágenes de episodios. AnimeAV1/Tioanime/AnimeLatinoHD usan fondos propios; Latanime solo arte compartido entre episodios; JKAnime conserva fondo neutro. Incluye 0.21.0 y 0.22.0. Ver ACTUALIZACION-0.23.0.md.

# Puntuaciones 0.22.0 preparadas, sin APK

Estrellas con notas de MyAnimeList mediante Jikan en el Catálogo, caché persistente, carga en segundo plano y coincidencias conservadoras. Incluye las optimizaciones de 0.21.0. Consultas reales a Jikan con tiempo de espera en este entorno; validación en la TV pendiente. Ver ACTUALIZACION-0.22.0.md. El APK publicado sigue siendo 0.20.0.

# Optimización 0.21.0 preparada, sin APK

Recuperación de CDN de Desu/Magi, caché de consultas, cancelación de llamadas antiguas, carga paralela limitada y sugerencias progresivas; avisos de salto acumulativos. Ver ACTUALIZACION-0.21.0.md. El APK publicado sigue siendo 0.20.0.

# AnimeTV 0.20.0

JKAnime se integra como quinta fuente: catálogo, búsqueda combinada con filtros, fichas, episodios paginados, calendario, sagas y servidores propios y externos. Se corrigen además las fichas de AnimeLatinoHD con bloques de texto React y se agilizan los estados de tarjetas agrupadas en Latanime. Incluye menú al mantener OK y limpieza de Seguir viendo en Ajustes → Reproducción. La entrega final autorizada se genera únicamente como APK armeabi-v7a para la TV del usuario. Ver ACTUALIZACION-0.20.0.md.

Nuevo diseño de catálogo, logos oficiales, ficha sin espacios reservados, idioma antes del servidor, tiempos separados y Ajustes por categorías. Se entrega únicamente APK ARM32 para la TV del usuario. Ver ACTUALIZACION-0.19.0.md y BUILD_PREFERENCES.md.

Se corrigen imágenes de Latanime, búsqueda + filtros de Tioanime/Latanime y catálogo dinámico de AnimeLatinoHD. Se refuerzan sagas y estados, se añade velocidad y se renueva el selector de fuentes accesible también desde Catálogo. Ver ACTUALIZACION-0.18.0.md.

AnimeLatinoHD se añade como cuarta fuente, con Delta/Epsilon y audios Subtitulado, Latino y Castellano por episodio. Incluye navegador de verificación Cloudflare con puntero para mando; compatibilidad física de WebView en la TV pendiente. Ver ACTUALIZACION-0.17.0.md.

AnimeAV1, Tioanime y Latanime: cambia la fuente con el botón superior «Fuente». Latanime agrupa los doblajes de una serie y permite elegir Latino/Castellano en el menú de servidores de cada episodio. Ajustes conserva un servidor predeterminado independiente para cada fuente y permite elegir el audio. La ficha y Seguir viendo indican el origen del anime. Ver ACTUALIZACION-0.16.0.md. Se mantiene la distribución optimizada ARM y el respaldo AV1 de HLS/MP4Upload.

Confirmación de capítulos recientes con Cerrar e investigación HLS: ver ACTUALIZACION-0.14.6.md. Géneros completos, saga antes de sugerencias, Finalizado en rojo y filtrado del logo: ver ACTUALIZACION-0.14.5.md. Ficha con posiciones estables, banner por densidades y recuperación de reproducción: ver ACTUALIZACION-0.14.4.md. Cápsulas compactas con intervalos sin signos +/−: ver ACTUALIZACION-0.14.3.md. Logo original, progreso de lista con relleno, recortes y elección desde capítulos recientes: ver ACTUALIZACION-0.14.2.md. Caché y reproductor: ver ACTUALIZACION-0.14.1.md. Rediseño general: ver ACTUALIZACION-0.14.0.md. Fondo carbón, menú compacto, logo del gato, etiquetas a la derecha y paneles laterales.

Aplicación nativa para Android TV con AnimeAV1. SDK mínimo 23; compatible con Android 9 y ARM de 32 bits.

## Interfaz de televisión

Menú lateral y portadas verticales de proporción 2:3: título dentro de la imagen con degradado, etiquetas superiores de tipo y estado. El estado usa el dato textual de la lista cuando existe, o la ficha consultada en segundo plano; caché de un día y solicitudes compartidas por anime. En la primera consulta puede aparecer después de la portada si la fuente lo omite en la lista; no se inventa. Fuente indicada una sola vez en el encabezado. Publicación y horario aparecen en la ficha, con fecha completa cuando existe en el calendario. Inicio, calendario y favoritos usan filas horizontales de AndroidX Leanback. El catálogo conserva cuatro columnas. Los encabezados compactos forman parte de la lista y desaparecen al bajar. Izquierda desde la primera tarjeta devuelve el foco al menú; dentro de una fila sigue navegando entre tarjetas. VerticalGridView y HorizontalGridView administran el foco, la alineación y el desplazamiento con el mando; se eliminaron las solicitudes manuales de desplazamiento que competían con la navegación. Las filas miden la altura completa de sus tarjetas y solo las filas próximas al viewport quedan adjuntas.

Las secciones principales no tienen margen exterior superior, inferior ni derecho; la ficha del anime tiene 24 dp en los cuatro lados. El margen fijo de 32 dp se aplica solo a la izquierda del menú. Se eliminó su opción de configuración; no reduce el alto del contenido. Fondo oscuro, panel de sección y foco visible; sin videos de fondo, desenfoques ni ampliaciones animadas. Referencias consultadas: repositorio local de SmartTube y galería Google TV de Dribbble. No se copió código de SmartTube.

## Reproducción

AnimeAV1 ofrece Voe, Byse, MP4Upload y HLS/Zilla, separados en subtitulado/doblado cuando existen. Tioanime ofrece Voe, YourUpload y Mega en subtitulado. El acceso Zilla incorpora las cabeceras verificadas para listas y fragmentos. HLS y MP4Upload pueden usar decodificación AV1 de software cuando Android no soporta ese códec; el decodificador de Android tiene prioridad. La fluidez depende del dispositivo y del vídeo.

Voe tiene prioridad automática y Byse es el respaldo. Byse adapta FilemoonExtractor de Yuzono, con licencia Apache-2.0 conservada. No se abre un reproductor web ni se ignoran certificados HTTPS.

El selector agrupa pistas de igual resolución, excluye pistas de previsualización y ofrece Automático solo cuando existen varias resoluciones reales. Si solo hay una, muestra esa calidad sin inventar otras. En los capítulos 1 y 2 compartidos, Voe ofreció 720p y Byse 1080p.

Se evitan solicitudes de verificación redundantes antes de entregar el video a Media3. Caché temporal de servidores (30 segundos) y enlaces extraídos (90 segundos), con trabajo concurrente compartido; los errores invalidan el enlace. Al entrar a una ficha se prepara silenciosamente el primer episodio y servidor preferido, sin iniciar reproducción. Esto puede reducir la espera posterior, según conexión y proveedor.

## Instalar

Instalar AnimeTV-0.17.0-armeabi-v7a.apk en la Xiaomi TV 4S de 32 bits, o AnimeTV-0.17.0-universal.apk para ARM32/ARM64, encima de la versión anterior para conservar favoritos, progreso y ajustes. Conserva el identificador y certificado de desarrollo.

## Comprobación y límites

Ver VERIFICACION.md. Las pruebas de interfaz se ejecutan con Robolectric/API 28; no hay TV física ni emulador completo conectado. Confirmar en la Xiaomi navegación, imagen/audio, calidad y velocidad. Ajustes > Informe de reproducción conserva errores.

La ficha usa un fondo carbón liso; no carga miniaturas de episodios como fondo. Portadas cargadas al adjuntarse a la lista, descargas repetidas compartidas y medidas de filas reutilizadas cuando sus datos y anchura no cambian. Nuevo logo del gato para la aplicación y el launcher, con iconos dibujados sin deformación. La reducción de micro pausas requiere confirmación física; no se midieron FPS en la Xiaomi.


## Cambios 0.9.0

Capítulos 16:9 con nombre dentro de la imagen y play al recibir foco; sin texto «Reproducir capítulo». Portadas recortadas a un contorno redondeado completo, incluido degradado. Etiquetas sin ancho expandido. Fondo de ficha: se reutiliza la imagen horizontal recibida en capítulos recientes para ese anime; si no existe en caché, se usa la última miniatura disponible, no siempre el episodio 1. Corrección de codificación de «Última publicación».

Barra del reproductor: retroceder, pausa/play mayor, adelantar, selector Episodios y engranaje. Opciones incluye «Saltar canción»: salto manual configurable (clave anterior opening para conservar ajustes). No detecta tiempos de opening/ending ni afirma que ese salto corresponda exactamente a la canción. La navegación horizontal del mando conserva saltos cortos y los botones multimedia siguen funcionando.

## Cambios 0.10.0

Orden ascendente/descendente en las fichas. Series de más de 100 capítulos muestran un selector horizontal de bloques de hasta 100; solo se construyen las tarjetas del bloque visible. Descendente empieza por el último bloque; ascendente por el primero. El índice original se conserva al reproducir, por lo que «siguiente episodio» sigue la secuencia habitual. Con 100 capítulos o menos no aparece el selector de bloques.

Derecha desde el menú del catálogo enfoca directamente la primera portada. Arriba desde la primera fila llega a la búsqueda y abajo desde la barra regresa a la primera portada. Si el catálogo está cargando, se conserva la solicitud de entrar a las portadas hasta que estén disponibles.

Revisión del proyecto Anime1v API y pruebas limitadas de HLS/MP4Upload documentadas en REVISION-SERVIDORES.md. No se añadió un servidor cuya reproducción en la Xiaomi no se haya demostrado.


## Cambios 0.13.0

Ver ACTUALIZACION-0.13.0.md: navegación reutilizada durante la sesión, lista compacta o miniaturas de episodios, orden mediante flechas, bloques expandibles desde 101 episodios, salto configurable de la barra de tiempo, etiquetas de calidad habitual y diagnóstico de pistas de video incompatibles. MP4Upload sigue disponible para prueba; no se promete reproducción AV1 en la Xiaomi.
