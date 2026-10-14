# Revisión de servidores para 0.10.0

Actualización 0.13.0: se conservan aquí las pruebas históricas. Desde 0.11.1 MP4Upload sí está integrado. La prueba del usuario en su TV entregó audio sin imagen y tiempos de espera en otros episodios. En 0.13.0 se añade diagnóstico de pistas incompatibles (AV1 cuando Media3 lo identifica) y más tiempo de arranque. Su reproducción en la Xiaomi continúa sin confirmación. HLS/Zilla sigue excluido.

Referencia leída: proyecto Anime1v API facilitado por el usuario, `src/services/download.service.js`, funciones de normalización del enlace Zilla y `downloadHlsVideo`. No se copió código de ese proyecto ni se ejecutaron sus scripts.

El proyecto transforma `/play/{id}` a `/m3u8/{id}` y pasa a FFmpeg un User-Agent de Chrome 124 y Referer del servidor. No se encontró allí un tratamiento específico del HTTP 403 de las listas secundarias.

Pruebas de red con los episodios 1 y 2 de Kusuriya no Hitorigoto 3rd Season de AnimeAV1:

- HLS/Zilla: manifiesto principal HTTP 200, con `#EXTM3U`. Primera petición enlazada: HTTP 403 y HTML, usando los encabezados del método de referencia. La accesibilidad del manifiesto principal no confirma reproducción. HLS sigue sin habilitarse en esta versión.
- MP4Upload: se obtuvo un enlace MP4 directo desde `player.src`, utilizando Referer `https://mp4upload.com/`. Lectura parcial HTTP 206 en el episodio 1. Hubo un timeout TLS inicial en el episodio 2; una consulta posterior obtuvo su encabezado de video.
- Ambos MP4 contienen una entrada de video `av01` de 1920 × 1080. El contenedor MP4 no implica H.264. No se comprobó su decodificación en la Xiaomi y MP4Upload no se añadió a los servidores de la app.

Android documenta soporte de plataforma para AV1 desde Android 10; puede variar según dispositivo y no está garantizado en Android 9. Referencia: https://developer.android.com/media/platform/supported-formats . Que otro reproductor reproduzca el archivo requiere investigar también sus decodificadores, no solo cómo obtiene la URL.

La versión 0.10.0 conserva Voe y Byse. No se afirma haber solucionado HLS ni demostrado reproducción MP4Upload en el televisor.
