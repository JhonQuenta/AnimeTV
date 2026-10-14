# AnimeTV 0.15.1: fuentes, preferencias y Mega

El botón «Fuente» de la esquina superior derecha abre el selector y se puede enfocar con el mando. El cambio de fuente sale de Ajustes. La ficha identifica su fuente junto a los metadatos; Seguir viendo añade una cápsula arriba a la izquierda de cada tarjeta con la fuente original. Los favoritos e historial siguen compartidos y se abren desde su proveedor original.

La cabecera de la ficha deja de reservar siempre dos líneas para el título. El nombre ocupa su alto real, la portada conserva 164x246 dp y los botones mantienen su alineación inferior. Los géneros conservan el alto completo. Se elimina el separador sobrante cuando la fuente no informa la fecha de publicación.

Ajustes presenta «Servidor de AnimeAV1» y «Servidor de Tioanime», con Automático, Elegir al reproducir y servidores de esa fuente. La preferencia se guarda por fuente. Automático conserva el orden de respaldo; un servidor fijo se inicia directamente y, si no aparece en ese capítulo, se abre el selector. No cambia silenciosamente a otro servidor tras fallar un servidor fijo. La antigua preferencia global se usa sólo como valor de migración hasta elegir una opción por fuente. La preparación en segundo plano respeta la preferencia de cada proveedor.

Voe y YourUpload muestran 720p en el selector. YourUpload se comprobó con Magical★Explorer, episodio 3: MP4 H.264, 1280x720, HTTP 206. Las etiquetas describen la calidad habitual; las pistas reales siguen determinando las opciones del reproductor.

En ese mismo episodio, la API Mega tardó 21,66 segundos en entregar la URL HTTPS. La app tenía un límite global de 10 segundos para esa extracción. Mega ahora usa un cliente independiente: 8 segundos para conectar, 35 de lectura y 45 para la llamada completa. Los otros servidores conservan sus límites. Después de obtener la URL se comprobaron rangos HTTP 206 inicial y final, MP4 descifrado y códec AVC; no se descargó el archivo completo. Se registra el servidor y etapa de extracción cuando hay un error. Mega permanece como streaming y sujeto a sus límites de transferencia; reproducción en la TV con este ajuste pendiente.

Pruebas: foco del selector superior, etiquetas de origen de historial mixto, identificador de ficha, preferencias independientes, migración, geometría de portada/botones/título/géneros, lector de rangos y respuesta Mega posterior al antiguo límite de 10 segundos. Compilación release optimizada, lint, arquitecturas y firma verificados en el informe AnimeTV-0.15.1-verificacion.json. VersionCode 28, mismo paquete y firma.
