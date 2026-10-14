# AnimeTV 0.14.9: respaldo AV1 en MP4Upload

MP4Upload comparte con HLS el renderer de vídeo FFmpeg/dav1d. El renderer de Android continúa primero: cuando el dispositivo dispone de un decodificador AV1 compatible, la app lo utiliza antes del respaldo por software. Se conserva el audio de Android y la extracción MP4 con sus cabeceras propias; las cabeceras específicas de Zilla no se aplican a MP4Upload.

La prueba del usuario con HLS 0.14.8 en Xiaomi MiTV-MSSP3 confirma imagen, sonido y avance rápido, pero fuertes tirones en ambos. El vídeo probado es AV1 1920x1080 sin variantes alternativas. Añadir el mismo respaldo a MP4Upload no garantiza fluidez en esa televisión. Reproducción MP4Upload y rendimiento en un Xiaomi TV Stick 4K: pendientes de prueba física.

La versión mantiene el identificador pe.animetv, firma de desarrollo, ajustes y progreso. Las pruebas en Robolectric/API 28 verifican el renderer de Android antes del respaldo en HLS y MP4Upload, y ausencia de ese respaldo en Voe y Byse. No equivalen a una prueba de decodificación en hardware real.
