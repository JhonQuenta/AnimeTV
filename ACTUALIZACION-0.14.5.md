# AnimeTV 0.14.5

- Los géneros de la ficha conservan la altura de 26 dp. Se eliminan sus márgenes y relleno verticales que recortaban los textos. Los botones mantienen las posiciones fijas de 0.14.4.
- Finalizado usa rojo oscuro, distinto al gris de Serie. En emisión conserva verde y Próximamente conserva ámbar.
- Saga y sugerencias busca primero títulos de la misma familia en el catálogo de la fuente y después rellena con recomendaciones del primer género. Hasta diez tarjetas, sin el anime actual ni duplicados, búsqueda limitada a tres páginas y caché de diez minutos. Si falla una consulta se aprovechan los resultados de la otra. La sección existe también para animes sin géneros.
- La familia se identifica por el nombre base: se normalizan acentos, subtítulos y sufijos de temporadas, películas, especiales y OVA. No constituye metadatos canónicos de relaciones: títulos completamente distintos pueden quedar fuera y coincidencias de nombres pueden requerir ajustes. Se verificó la búsqueda de Sword Art Online en AnimeAV1: incluye II, Alicization, Alternative, Progressive, Ordinal Scale y Extra Edition.
- El gato del menú conserva el dibujo original y ahora usa PNG por densidad, prefiltrado mediante reducciones sucesivas, evitando reducir directamente un bitmap de 1254 píxeles a unos 96 en pantalla. El banner incluye imágenes de 480×270 a 1920×1080 por densidad (960×540 en xhdpi), por encima del mínimo de Android TV, para dar más detalle al ampliar el foco. No se inventan detalles del logo ni se cambia su composición.

## Generador web

Se investigó https://atvlauncher.trekgonewild.de/: crea un APK adicional que abre otro paquete y reduce los banners a 320×180. No mejora la imagen original ni el icono dentro de AnimeTV. AnimeTV ya declara LEANBACK_LAUNCHER y banner, así que no necesita una aplicación intermedia para aparecer en el launcher.

Se incluyen PNG listos para probar un generador, sin subir archivos ni instalar una segunda app: outputs/Leanback-assets/AnimeTV-banner-320x180.png, AnimeTV-banner-1280x720.png y AnimeTV-icon-512x512.png. Nombre AnimeTV, paquete pe.animetv. Ese APK adicional produciría otra entrada en el menú y no reemplazaría el APK principal.

## Verificación

Pruebas completas, APK y lint. Pruebas de sugerencias: orden de saga antes de otros animes, búsqueda paginada, exclusión del anime actual, eliminación de duplicados, límite de diez, caché, fallo parcial y rechazo de franquicias distintas. Captura nativa con géneros y comprobación del texto dentro de la altura disponible. La nitidez final del launcher requiere comprobar esta versión en la TV: depende también de su escalado y caché; no se garantiza que un generador web lo mejore.
