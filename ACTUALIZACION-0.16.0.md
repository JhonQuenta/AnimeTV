# AnimeTV 0.16.0 — Latanime y doblajes agrupados

Latanime se añade al botón superior Fuente y a las preferencias de servidor por fuente. Se conservan AnimeAV1, Tioanime, favoritos e historial global con identificadores de origen.

Las variantes latino/castellano de Latanime se presentan en una sola ficha cuando comparten el identificador de serie, conservando temporadas, películas y especiales como obras distintas. La ficha reúne los números de episodio reales de ambas versiones. Al seleccionar un capítulo, el menú muestra LATINO y CASTELLANO con sus respectivos servidores; solo aparecen los audios que tienen ese número de episodio. Inicio, búsqueda y calendario agrupan también las variantes. Cada extracción utiliza el Referer del episodio real del doblaje elegido.

Los identificadores internos `animetvAudio=all` permiten guardar favoritos, progreso y reanudar después de reiniciar la aplicación. No se solicitan esas rutas sintéticas a la web. Se reconstruye la relación consultando fichas reales válidas; no se crean enlaces de episodios a partir de su posición en una lista. Las series cuyos identificadores son diferentes no se unen por mera similitud de título. Se descartan relaciones con años de estreno incompatibles.

Por defecto Latanime abre el selector de audio/servidor. Ajustes permite elegir Automático o un servidor fijo. Audio preferido admite Subtitulado, Doblado, Latino y Castellano; la preferencia genérica Doblado incluye los dos últimos. Las otras fuentes mantienen sus preferencias existentes.

## Servidores

- Prioridades implementadas y verificadas: Dsvplay, Vide0, Filemoon y Lulu. Dsvplay/Vide0/Do7go comparten el protocolo DoodStream. Filemoon en las muestras utiliza la plataforma Byse. Lulu devuelve HLS.
- Reutilizados: Byse, Voe, MP4Upload y Mega. Añadidos también Mixdrop y Do7go por compartir protocolos y superar comprobaciones de vídeo/rangos.
- Los nombres se normalizan por el dominio real, no por el texto del botón. En La Caja Azul latino, «ok» apuntaba a Voe. Mixdrop y mxdrop se normalizan como Mixdrop.
- Hexload y Savefiles quedan fuera de esta entrega; no se ha validado su protocolo completo. No se afirma que todos sus enlaces estén obsoletos. Listeamed se excluye: una muestra acabó en una página ajena al vídeo.
- Hay fallos de enlaces individuales: Mega de Frieren latino devuelve código -6; Voe de Frieren castellano devuelve 404; los enlaces Mixdrop antiguos no ofrecen un vídeo en el HTML recibido. Se conserva el servidor si funciona en otros episodios.

El lector de diccionarios JavaScript se cambió a un analizador iterativo para evitar desbordar la pila al leer los scripts grandes de Lulu; nunca ejecuta scripts ni publicidad. DoodStream utiliza las cabeceras verificadas del reproductor. Lulu utiliza su petición habitual: añadir cabeceras de iframe ajenas a esa petición produjo enlaces que el CDN rechazaba.

Las etiquetas de resolución de otras fuentes no se copian a Latanime: sus muestras de Voe/Byse/Filemoon/Lulu ofrecen 1080p, pero eso no prueba que todo el catálogo tenga la misma calidad. El menú de calidad del reproductor utiliza las pistas reales de Media3.

Se incorpora un respaldo DNS limitado a latanime.org/www.latanime.org cuando el resolutor del dispositivo devuelve UnknownHostException. Usa DNS sobre HTTPS de Google, caché de cinco minutos y conserva la validación TLS. El DNS de este equipo no resolvía Latanime, mientras Cloudflare y Google devolvían registros válidos. El resto de dominios mantiene el DNS habitual.

## Comprobación

En La Caja Azul 27, latino y castellano: Dsvplay, Mixdrop, Mega y MP4Upload entregaron rangos MP4 HTTP 206; Byse/Voe entregaron listas y segmentos HLS. En Frieren 1: Filemoon y Lulu entregaron HLS en ambos doblajes, Vide0 en latino y Do7go en castellano entregaron rangos MP4. Se verificó el flujo agrupado real: La Caja Azul 27 y Frieren 1 muestran LAT/CAST y reproducen los enlaces correspondientes a cada doblaje.

Las pruebas automatizadas cubren agrupación, episodios faltantes en un doblaje, restauración sin caché de catálogo, años/temporadas distintos, encabezados del selector, preferencias, servidor de origen y lectura de scripts grandes. Las pruebas de red verifican acceso a medios, no decodificación ni fluidez en una TV física. La comprobación en la Xiaomi sigue pendiente.

Se mantienen R8, reducción de recursos y APK ARM32/ARM64; no se añaden dependencias de vídeo ni bibliotecas nativas nuevas. Instalar el APK armeabi-v7a sobre la versión anterior en la Xiaomi TV 4S con Android de 32 bits.
