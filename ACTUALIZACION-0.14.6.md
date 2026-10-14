# AnimeTV 0.14.6

El popup de capítulos recientes muestra Ver capítulo y Cerrar. Cerrar únicamente cierra el popup y deja la ficha abierta, sin desplazar el foco a otro capítulo ni iniciar servidores. Se elimina el método de navegación que solo usaba la opción anterior. Prueba existente actualizada para verificar texto, cierre sin reproducción y confirmación del capítulo correcto.

## Comprobación del proveedor llamado HLS

Capítulo probado: https://animeav1.com/media/kashita-maryoku-wa-revo-barai-de-kyousei-choushuu/2

La página ofrece HLS de player.zilla-networks.com. Prueba HTTP desde este entorno, con sesión de cookies y GET normal y muestras de recursos mediante Range:

- Lista /m3u8/c2ecdb30cc72f4a5ad1249d6cd3cc2f4: 200, application/x-mpegURL.
- Archivo de inicio /segs/.../init.html: 403, text/html.
- Primer segmento /segs/.../000.html: 403, text/html.
- Mismo resultado con User-Agent de navegador y cuatro variantes: Referer del player; Referer + Origin del player; Referer del embed completo + Origin del player; Referer y Origin de AnimeAV1.
- Todos los recursos probados usan HTTPS. El rechazo se produce antes de la decodificación. No se ha reproducido este enlace en una TV física ni se ha comparado una sesión de navegador autenticada.

La app ya admite HLS mediante media3-exoplayer-hls y DefaultMediaSourceFactory, con la factoría HTTP compartida que envía User-Agent y Referer también a las solicitudes dependientes. Voe y Byse pueden usar el formato HLS; eso es distinto del proveedor listado con nombre HLS. No se reactiva Zilla solo porque su lista principal responda 200, ni se cambian las cabeceras de servidores funcionales sin evidencia. El resultado no permite afirmar si se necesita otra autorización, cookies de navegador o cambios del proveedor.

Referencias oficiales: https://developer.android.com/media/media3/exoplayer/customization (cabeceras en cada petición) y https://developer.android.com/media/media3/exoplayer/troubleshooting (HTTP sin TLS y formatos/códecs).

Registro resumido: AnimeTV-HLS-0.14.6-comprobacion.txt, junto al APK. No se guardan cookies ni enlaces firmados en el informe.
