# Imágenes y selector de fuentes 0.23.0 — sin APK

Incluye los cambios preparados de 0.21.0 y 0.22.0. No se genera APK; la distribución sigue limitada a armeabi-v7a cuando el usuario lo solicite.

## Logos

- AnimeAV1: símbolo vectorial con los tres trazados originales del SVG oficial, en lugar del favicon de 32 píxeles. No pierde detalle al ampliar.
- Tioanime: símbolo del logotipo oficial del encabezado, obtenido del PNG de 222 × 60. El recurso del símbolo conserva 54 × 60 píxeles; no se inventa resolución.
- JKAnime: símbolo del logotipo oficial del encabezado de 300 × 64, con recurso de 77 × 64, en lugar del favicon de 16 × 16.
- Latanime: símbolo L extraído del logotipo oficial del encabezado previamente descargado, de 1224 × 648. Recurso de 245 × 265, en lugar del favicon de 16 × 16. La descarga nueva falló por DNS tanto en terminal como en navegador; se aprovechó el original ya conservado.
- AnimeLatinoHD: se conserva su recurso de 256 × 256, que ya tenía resolución suficiente.
- Los recursos se usan en todos los indicadores de fuente y permanecen locales: no añaden consultas al navegar. No son logos generados ni redibujados a partir de capturas.
- Cancelar en el selector vuelve al centro con el icono de cierre y queda fijo debajo de la lista desplazable, siempre visible; mantiene foco, cierre y fuente elegida.

Procedencia de los originales: https://animeav1.com/img/logo.svg ; https://tioanime.com/assets/img/logo-dark.png ; https://cdn.jkdesa.com/assets3/css/img/jkanimenet.png?v=2.0.204 (Referer JKAnime) ; https://latanime.org/img/logito.png (copia oficial guardada de la revisión anterior).

## Portadas de Latanime

El listado reciente publica imágenes horizontales de episodios. La ficha independiente publica una portada vertical diferente. Las tarjetas ya no muestran ese horizontal recortado: consultan los metadatos de la ficha conocida para obtener la portada correcta y la guardan para próximas visitas.

Esta consulta se hace también cuando el tipo y el estado ya están presentes. Mientras falta la portada se muestra el fondo de carga habitual. La imagen horizontal sigue asociada al episodio y a Seguir viendo. No se deduce la URL de la portada cambiando carpetas ni se descargan todos los doblajes para resolver la tarjeta.

## Fondos de ficha

- Modelo Detail con un campo independiente backdrop; conserva los constructores anteriores. No reutiliza Store.backdrop, que sigue siendo imagen de episodio para Seguir viendo.
- AnimeAV1: /backdrops/ y coincidencia de identificador con la portada o nombre explícito del propio anime. No elige fondos de relacionados ni /screenshots/ del capítulo 1.
- Tioanime: figure.backdrop dentro de la ficha, carpeta /uploads/fondos/.
- AnimeLatinoHD: campo banner propio, con URL TMDB w1280.
- Latanime: utiliza el arte horizontal de /thumbs/portada/ únicamente si está compartido por todos los episodios de una versión, con al menos dos episodios. No selecciona arbitrariamente la primera captura cuando las imágenes difieren.
- JKAnime: fondo neutro al no disponer de un fondo propio identificado.
- Fondo detrás del encabezado, con degradado oscuro horizontal y desvanecimiento inferior. Portada vertical conserva 164 × 246 dp y la información mantiene su estructura; si falta fondo no se reserva otro espacio.
- Descarga asíncrona y caché de imágenes existente. Decodificación con objetivo máximo de 1280 píxeles de ancho, independiente de las miniaturas pequeñas. La nitidez depende del archivo original; no se promete detalle adicional para arte horizontal pequeño.

## Comprobación

Consultas reales previas confirmaron fondo de Beerus en AnimeAV1, Dark Summoner en Tioanime y Frieren en AnimeLatinoHD. La revisión actual confirmó descargas de logos de AnimeAV1, Tioanime y JKAnime. Latanime sigue dando error DNS en este entorno; la separación de sus imágenes se validó con HTML real guardado y pruebas locales.

Pruebas específicas: fondo propio frente a episodio/relacionado, banner de AnimeLatinoHD, fondo de Tioanime, arte común de Latanime frente a capturas diferentes, actualización de portada aun con tipo/estado conocidos, reutilización de portada, recursos de logos, Cancelar y conservación del tamaño de portada en un encabezado con fondo. Prueba física en la TV pendiente.
