# AnimeTV 0.17.0 — AnimeLatinoHD

Se añade AnimeLatinoHD al selector superior y a las preferencias de servidor por fuente. La ficha y Seguir viendo conservan el identificador del proveedor original. Se mantienen AnimeAV1, Tioanime y Latanime.

Se implementan directorio, búsqueda, filtros de tipo/estado/año/género, calendario, capítulos recientes y fichas. Se leen los registros JSON públicos de React Flight incluidos en el HTML, sin evaluar sus scripts. Los números y audios proceden de los datos reales: no se crean episodios a partir del número total de capítulos ni se inventan doblajes.

El selector conserva Delta y Epsilon para cada idioma disponible: Japonés/Subtitulado (SUB), Español Latino (LAT) y Castellano (ESP, convertido al identificador interno CAST). Se omite Gamma, que el sitio muestra en móviles. Una ficha puede tener uno, dos o tres idiomas dependiendo del episodio. Por defecto se abre el selector; Ajustes permite Automático o un servidor fijo por fuente y un audio preferido.

En las muestras actuales y antiguas, Delta redirige mediante el reproductor público website.animelatinohd.com a Byse; Epsilon redirige a Lulu. Se reutilizan esos extractores y se entrega su HLS a Media3. Se validan los dominios del puente y del alojamiento. No se incorpora un reproductor web para reproducir los capítulos. Las resoluciones se determinan por las pistas reales del reproductor.

## Verificación de Cloudflare

La página principal respondió 403 a peticiones directas. El navegador de escritorio completó automáticamente la verificación y permitió leer el catálogo y los episodios. El API que aparece en los datos del sitio respondió 403; no se utiliza ese API ni se añaden claves privadas.

ProtectedPages intenta primero la petición HTTPS con el User-Agent y las cookies de la sesión WebView de la propia app. Si reconoce una respuesta de desafío Cloudflare, abre un navegador de verificación del sistema. El usuario mueve un puntero con las flechas del mando, pulsa OK para hacer clic y Atrás para cancelar. La app no resuelve desafíos ni CAPTCHAs automáticamente. Cuando se carga el documento real de AnimeLatinoHD, recoge sus datos y vuelve a la pantalla nativa. Las cookies permanecen en el CookieManager del dispositivo; no se exportan ni se registran.

Se mantiene el User-Agent durante la sesión, JavaScript y almacenamiento DOM para la verificación. Las navegaciones principales se limitan a AnimeLatinoHD y se conserva la validación TLS. La verificación se cancela al cerrar su actividad y tiene un límite de 90 segundos. Se evita abrir verificaciones desde la carga de etiquetas de portadas en segundo plano. El navegador utiliza Android System WebView; no se empaqueta un motor adicional.

Cloudflare advierte que los WebView y navegadores antiguos pueden tener compatibilidad limitada: https://developers.cloudflare.com/cloudflare-challenges/reference/supported-browsers/. La verificación real con el mando en la Xiaomi TV 4S sigue pendiente. Si el motor no puede completar el desafío, el usuario puede cancelar y usar otra fuente; no se garantiza que un clic permita acceder con cualquier versión de WebView.

## Pruebas

Se observaron los datos públicos de Tempal: Item no Chikara episodio 3 con SUB/LAT/ESP y de Azumanga Daiou The Animation episodio 26 con SUB. Con los extractores de la app, las ocho combinaciones probadas de idioma/servidor entregaron lista y segmento HLS accesibles. Ver AnimeTV-AnimeLatinoHD-0.17.0-red.txt.

Las pruebas automatizadas abarcan registros fragmentados de React Flight, episodios no consecutivos, separación de idiomas, ausencia de doblajes, rutas y filtros, estado, calendario ISO, validación de puentes, detección de desafíos y límites del puntero. Se conserva Android mínimo API 23. Los resultados están en AnimeTV-0.17.0-verificacion.json.

Acceso al HLS y comprobación de parsers no equivalen a medir decodificación o fluidez en una TV física. La reproducción y la verificación de esta versión en la Xiaomi están pendientes.
