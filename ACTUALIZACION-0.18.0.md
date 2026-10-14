# AnimeTV 0.18.0

El catálogo permite cambiar la fuente desde la barra junto a búsqueda y filtros; al cambiar conserva la sección Catálogo y reinicia los filtros de la fuente anterior. El selector tiene una ventana gris redondeada, icono de pieza, identificadores de color, nombres, idiomas y marca de fuente activa. Se separa la etiqueta de origen del botón de favoritos en la ficha.

Las imágenes de Latanime se solicitan con User-Agent y Referer: el servidor respondió 403 con okhttp/4.12.0 y 200 con cabeceras de navegador. La comprobación con el cliente de la app devuelve una imagen válida. Se conserva la caché de imágenes y el respaldo DNS existente.

Tioanime ignora los filtros de su directorio cuando hay búsqueda. La app aplica la intersección sobre las fichas reales, conserva la paginación después del filtrado y guarda los resultados cinco minutos. Se hace lo mismo para búsquedas de Latanime con filtros, incluyendo disponibilidad real de Latino/Castellano. Los filtros sin búsqueda siguen utilizando el directorio del proveedor. Una búsqueda excesivamente amplia pide concretar el título después de doce páginas, en lugar de ofrecer resultados parciales como completos.

AnimeLatinoHD usa `search`, no `q`. Su HTML inicial incluye un directorio sin filtrar aunque la URL tenga filtros: el sitio obtiene los resultados posteriormente en JavaScript. El catálogo se lee tras terminar esa carga en Android System WebView, con caché de cinco minutos. El navegador permanece oculto salvo que se requiera intervención en Cloudflare. No se incluye otro motor en el APK. Las etiquetas se completan desde las fichas en segundo plano; si se requiere una verificación manual, esa tarea no abre un diálogo ni inventa un estado. Los resultados reales de búsqueda y año se comprobaron en el navegador de escritorio; el flujo final de WebView sigue necesitando validación en la TV.

Las sugerencias priorizan relaciones explícitas cuando el proveedor las publica. AnimeLatinoHD entrega prequelas, secuelas y relacionados; Sword Art Online enlaza directamente Sword Art Online II. Las demás fuentes usan búsquedas de saga, con mejor reconocimiento de entregas con subtítulo y sufijos de doblaje. Se mantienen deduplicación, exclusión del anime actual, límite de diez sugerencias y relleno por género. La coincidencia por nombre es una aproximación: no garantiza identificar todas las franquicias ni su cronología.

El reproductor incorpora un icono de velocidad con 0,25×, 0,5×, 0,75×, 1×, 1,25×, 1,5×, 1,75× y 2×. La selección se guarda y se aplica también al cambiar de capítulo o reconstruir el reproductor; no cambia la posición al elegir una velocidad.

Las comprobaciones de red incluyen Tioanime/Sword Art Online con Finalizado + Serie, prioridad de saga, Latanime/Frieren con año 2023 e imagen de catálogo. Ver AnimeTV-0.18.0-red.txt e informe de pruebas. No se afirma reproducción ni renderizado final en la TV física a partir de comprobaciones de escritorio.

Se mantienen APK ARM32, ARM64 y universal con minificación y reducción de recursos, el mismo certificado y las bibliotecas AV1 existentes. Se distribuyen fuentes de la app, fuentes de terceros y avisos GPL.
