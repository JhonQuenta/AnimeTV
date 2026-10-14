# AnimeTV 0.14.4

- Portada fija de 164 × 246 dp. Título de hasta dos líneas, metadatos, publicación y sinopsis reservan alturas constantes. Una fecha ausente o sinopsis corta ya no cambia la posición de las acciones. Se mantienen los márgenes y la navegación del rediseño.
- Se conserva el gato y la tipografía del logo aprobado. Banner exportado desde el original con interpolación bicúbica, aproximadamente 9 % más grande dentro del fondo, sin estirar sus proporciones. Variantes 160×90, 240×135, 320×180, 480×270 y 640×360 para mdpi a xxxhdpi; icono de 512×512 y versión adaptativa conservada.
- HTTP de video con conexiones reutilizables, solicitudes Range y Referer. Caché local LRU de 64 MiB: solo guarda fragmentos solicitados y elimina los antiguos; no descarga episodios completos. Buffer máximo de 30 segundos con límite de 24 MiB y buffer anterior de 10 segundos. Las calidades disponibles se conservan.
- Al salir se guarda posición y pausa y se libera el decodificador. Al volver antes de un minuto se reutiliza el enlace; después se resuelve el mismo servidor, sin volver a pedir toda la lista. Si el enlace falla se renueva una vez para Voe, Byse o MP4Upload. Media3 admite hasta cuatro reintentos de carga; los errores de códec no disparan una renovación inútil del enlace.
- Saltos de 30 segundos o más buscan el fotograma clave más cercano para reducir decodificación y espera: la posición puede diferir ligeramente del segundo pedido. Los saltos pequeños conservan precisión. Byse dispone de 20 segundos para iniciar y MP4Upload de 25; Voe conserva 12. Recuperación de buffering: 25 segundos.

## Verificación

Pruebas unitarias y Robolectric, compilación APK y lint. Incluyen posiciones de ficha con título largo, publicación ausente y sinopsis de distinta longitud; caché de video probada con servidor HTTP local, Range y Referer. Capturas nativas de UI con datos de prueba en outputs/redesign-verification.

No se ha probado esta versión en una TV física ni medido tiempos con los tres proveedores reales. Un servidor caído, enlace bloqueado o códec que la TV no admite sigue impidiendo la reproducción; estas mejoras no añaden un decodificador AV1 por software. El launcher también controla el escalado de sus tarjetas y puede conservar un banner anterior en caché.

Guía de recursos de Android TV: https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines (320×180 xhdpi y 640×360 xxxhdpi). El archivo 512×512 es un recurso adicional de icono, no una exigencia universal del launcher de TV.
