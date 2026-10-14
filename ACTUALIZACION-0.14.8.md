# AnimeTV 0.14.8: HLS/Zilla y AV1

El proveedor HLS vuelve al selector. El capítulo 2 de Kashita Maryoku compartido devuelve una lista de fragmentos MP4 con vídeo AV1. Zilla exige `Sec-Fetch-Site: same-origin`: las pruebas comparativas obtuvieron 403 sin ella y 200 con ella. El enlace `/m3u8/<id>` declara ahora su MIME HLS explícitamente, ya que no acaba en `.m3u8`.

El extractor valida la inicialización y el primer fragmento, conserva las cabeceras en el reproductor y rechaza recursos fuera del mismo proveedor. No acepta páginas HTML de error como vídeo aunque devuelvan HTTP 200. Voe, Byse y MP4Upload mantienen sus cabeceras y prioridad; HLS se ofrece después.

La Xiaomi MiTV-MSSP3/machuca proporcionada usa Android 9/API 28, CPU de cuatro núcleos Cortex-A55 y aplicaciones ARM de 32 bits. En las capturas de AIDA64 no aparece AV1. Para HLS se agrega un renderer FFmpeg/dav1d de Nextlib, con bibliotecas armeabi-v7a incluidas, después del renderer de Android. Audio y otros servidores mantienen el renderer de Android. Se limita a cuatro hilos y cuatro buffers de entrada/salida; la espera inicial de HLS permite 25 segundos. El informe registra ABI y disponibilidad real del decodificador AV1 de software.

Dependencias alineadas: Media3 1.11.1 y Nextlib 1.11.1-0.16.0; compileSdk 36, minSdk 23 y targetSdk 35. Se mantienen preferencias, identificador y firma de desarrollo.

Comprobación real de red con el extractor Java/OkHttp: lista, inicialización y segmentos 000, 020 y 060 con HTTP 200. La inicialización contiene `av01`. Esto prueba acceso al contenido, no decodificación en la TV. Las pruebas de Android se ejecutan en Robolectric/API 28: no hay TV física conectada. La reproducción, sincronización y fluidez de AV1 requieren prueba en la Xiaomi.

Prueba pendiente: abrir ese capítulo, seleccionar HLS, comprobar imagen y sonido, adelantar unos minutos, salir a otra app y volver. Si hay pantalla negra, error o tirones, consultar Ajustes > Informe de reproducción.
