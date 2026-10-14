# Puntuaciones del catálogo 0.22.0 — código preparado, sin APK

Incluye todos los cambios de optimización preparados en 0.21.0. El APK publicado sigue siendo 0.20.0; no se ha ejecutado assembleRelease ni se ha generado otro APK.

## Presentación

- Puntuación de MyAnimeList a través de Jikan v4, visible como estrella y un decimal (por ejemplo ★ 9,2) en la esquina superior izquierda de las tarjetas del Catálogo, incluida la búsqueda con filtros.
- Tipo y estado a la derecha, en dos líneas para impedir el solapamiento. No altera el tamaño de las portadas ni añade pasos al recorrido del mando.
- El subtítulo del Catálogo identifica MyAnimeList; el lector de accesibilidad anuncia la nota sobre 10. La estrella no recibe foco propio.
- No se añaden notas a Inicio, Seguir viendo, Calendario, Favoritos ni sugerencias en esta entrega.

## Solicitudes y persistencia

- Cliente dedicado, separado de los extractores de vídeo, sin dependencias nuevas. No espera a Jikan para abrir el Catálogo.
- Se solicita al adjuntarse una tarjeta y se retira la suscripción al desprenderse. La última suscripción cancela el trabajo en cola o la llamada HTTP activa. La destrucción de la actividad cancela todo el trabajo restante.
- Una consulta a la vez, separadas al menos 1,2 segundos: como máximo aproximadamente 50 por minuto en el proceso. Comparte el límite entre recreaciones de actividades.
- HTTP 429 aplica Retry-After, como mínimo un minuto; errores y tiempos de espera aplican una pausa de 30 segundos. No reintenta en bucle ni muestra avisos repetidos al usuario.
- Caché persistente independiente de las fuentes e historial, limitada a 600 búsquedas. Notas durante 7 días; coincidencias ausentes/ambiguas/sin votos durante 1 día. Los errores HTTP no se guardan como ausencia de puntuación.
- Una nota caducada se sigue mostrando mientras se intenta actualizar. Cambiar de fuente o reiniciar no obliga a descargar notas todavía vigentes.

## Identificación y límites

- Búsqueda de solo lectura: /v4/anime?q=...&limit=25&sfw=true. No se envían historial de reproducción, cookies de las fuentes ni datos de cuenta.
- Exige coincidencia exacta normalizada con un título oficial, inglés, japonés o sinónimo publicado por Jikan; contrasta tipo cuando la fuente lo proporciona. Normaliza acentos, puntuación, sufijos de doblaje y numeración de temporadas conservando sus números.
- Rechaza coincidencias aproximadas, múltiples identificadores coincidentes, películas con título de una serie y notas nulas, cero o fuera de 1–10. No inventa puntuaciones.
- Un título traducido que Jikan no reconozca puede quedar sin estrella. El resultado de búsqueda se limita a la primera página de 25 candidatos; no hace recorridos masivos del servicio. No garantiza puntuaciones en todos los animes.
- La clave de caché es título normalizado + tipo: reutiliza la nota entre fuentes cuando ambas publican el mismo título/tipo. Los títulos alternativos distintos pueden necesitar su propia búsqueda inicial.

## Verificación

Pruebas de coincidencias, remakes ambiguos, temporadas, películas, puntuaciones inexistentes, persistencia/caducidad, solicitudes compartidas, cancelación real HTTP, parámetros de búsqueda y HTTP 429. Pruebas visuales de tarjeta con nota y sin nota, solapamientos y foco del mando.

Se intentó consultar realmente Sousou no Frieren y Sword Art Online desde el cliente de producción en este entorno: ambas llamadas terminaron en Connect timed out. Por tanto, la conexión real con Jikan y la carga de notas en la TV permanecen pendientes de comprobar cuando el servicio sea accesible. Los casos de éxito/errores se verificaron con MockWebServer, sin sustituir resultados reales por notas de prueba en producción.

Datos y límites del proveedor: https://docs.api.jikan.moe/ y https://github.com/jikan-me/jikan-rest/blob/master/storage/api-docs/api-docs.json
