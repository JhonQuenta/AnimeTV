# Correcciones 0.26.0 preparadas

Incluye los cambios 0.21–0.25. No se ha generado un nuevo APK.

- AnimeLatinoHD: los capítulos recientes mantienen su arte horizontal en el episodio. Las tarjetas usan el poster real de los datos de anime cuando existe; de lo contrario solicitan la ficha, con caché y dos trabajadores. No recortan el banner como si fuera una portada.
- Todas las fichas con fondo: imagen al borde superior, izquierdo y derecho. El contenido mantiene 24 dp de separación y los capítulos conservan márgenes seguros. Sin fondo se conserva el diseño anterior.
- Etiquetas de fuente más compactas, con logo y nombre; el selector conserva el foco y la acción de cambiar fuente.
- Notas: Jikan/MyAnimeList conserva prioridad. Ante error de conexión se omite durante 15 minutos y se usa AniList, con la procedencia real visible en ficha y accesibilidad del catálogo. La caché persiste procedencia, conserva notas anteriores y permite volver a comprobar ausencias guardadas antes del respaldo. No se inventan notas ni se mezclan secuelas. La cola limita a una consulta cada 2,1 segundos y respeta HTTP 429.
- JKAnime Desu/Magi: búfer inicial 12 s, recuperación 15 s, objetivo 25–45 s con límite 48 MiB; MP4Upload/HLS: inicio 8 s, recuperación 10 s, 20–40 s con 32 MiB. Otras alternativas mantienen su inicio rápido. Esto aumenta la espera inicial para absorber variaciones de descarga sin cambiar calidad o códec.
- Réplicas de JKAnime: la comprobación de bytes cubre las rutas publicadas `/ja/segs/` y `/jp/segs/`, siempre dentro de los hosts conocidos y con Referer válido.
- AV1: tras conocer el formato seleccionado se comprueba un decodificador por hardware compatible con ese formato. Si falta, se evita la reproducción por software y se oculta esa alternativa concreta del episodio durante 24 horas. No se oculta un servidor entero ni se confunde HLS/H.264 con AV1. La primera comprobación necesita abrir el archivo; no se sondea todo el catálogo al iniciar.

## Comprobaciones reales

Psyren, episodio 1 de JKAnime: Desu y Magi contienen H.264 High nivel 4.0 en TS. Primer segmento: 9.347.548 bytes para 5,088 s, descargas de 9,2 s y 3,5 s. Otro segmento activa la detección de nodo lento. MP4Upload contiene H.264 High nivel 4.0 a 1920×1080 y AAC; muestra de 1 MiB: HTTP 206 en aproximadamente 4 s. Estos archivos no usan AV1. El patrón apunta a suministro insuficiente/intermitente, no demuestra un fallo de decodificador.

La implementación real de notas consultó Frieren (9,1 AniList) tras el timeout de Jikan en 5,7 s; Sword Art Online (7,0 AniList) en 316 ms usando el respaldo. Las notas cambian con el proveedor y el tiempo.

Límite: más búfer no puede compensar indefinidamente una conexión que entregue menos datos de los que consume el vídeo. Hace falta validar los cortes en la TV; las pruebas de escritorio no demuestran reproducción fluida allí. Ni la lista de códecs ni una etiqueta de servidor permiten garantizar fluidez para todos los episodios.
