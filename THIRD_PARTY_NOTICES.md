# Referencias y dependencias

Los logos oficiales de las fuentes se incluyen como identificadores locales. Su procedencia y resoluciones están documentadas en SOURCE_LOGOS.md; las marcas conservan la titularidad de cada sitio.

AnimeLatinoHdSource, NextPage y ProtectedPages son implementaciones propias del HTML público de AnimeLatinoHD, sus datos JSON y Android WebView. Referencias: https://www.animelatinohd.com/, https://developer.android.com/reference/android/webkit/WebView y https://developers.cloudflare.com/cloudflare-challenges/reference/supported-browsers/. Se consultó el repositorio público histórico https://github.com/Animelatino/animelatinohd2 como referencia de rutas; no se incorpora su código ni claves de configuración. Delta y Epsilon reutilizan ByseStreams y LatStreams existentes. No se añaden dependencias Maven ni bibliotecas nativas.

LatanimeSource y LatStreams son adaptadores propios del HTML y protocolos públicos de Latanime y sus alojamientos. Se consultaron también DoodExtractor.kt, LuluStream.kt y Filemoon.kt del proyecto Cloudstream (GPL-3.0): https://github.com/recloudstream/cloudstream/tree/master/library/src/commonMain/kotlin/com/lagradost/cloudstream3/extractors. No se incorpora la aplicación Cloudstream ni sus plugins. La distribución de AnimeTV ya incluye GPL-3.0 y su código fuente completo. Los enlaces públicos a vídeos en las fixtures se usan para verificar parsers; el APK no incluye esas fixtures.

TioAnimeSource y MegaDataSource son implementaciones propias basadas en el HTML público de https://tioanime.com/ y el protocolo de enlaces públicos MEGA. Referencia de AES-CTR y claves: https://github.com/meganz/sdk/blob/master/src/crypto/cryptopp.cpp. No se incorpora el SDK MEGA ni se ejecuta JavaScript del sitio. YourUpload reutiliza el lector de literales del adaptador nativo existente.

Desde 0.14.8 se incorpora Nextlib Media3ext 1.11.1-0.16.0 (GPL-3.0), con FFmpeg 9.0.1, dav1d 1.5.4 y mbedTLS 3.6.7. Se conserva THIRD_PARTY_GPL_LICENSE.txt. El código fuente y scripts correspondientes se entregan en AnimeTV-0.31.0-third-party-source.zip, junto con los archivos fuente originales de esas dependencias. Referencia fijada: https://github.com/anilbeesetti/nextlib/tree/v1.11.1-0.16.0 (commit 2dc77cd2d9352bc308aa26782f9c77a320df510b). Cada componente conserva su propia licencia; la distribución combinada que incluye Nextlib se entrega bajo los términos GPL-3.0 aplicables.

Este proyecto utiliza AndroidX Media3, OkHttp, Jsoup y desugar_jdk_libs mediante dependencias Maven. Se deben conservar las licencias correspondientes al distribuir el proyecto.

El formato del adaptador AnimeAV1 se estudió mediante la página pública y el repositorio yuzono/anime-extensions, que declara licencia Apache-2.0. El adaptador de esta aplicación es independiente del entorno de Aniyomi y no carga sus extensiones.

Referencias:

- https://github.com/yuzono/anime-extensions
- https://github.com/yuzono/anime-extensions/blob/master/LICENSE
- https://developer.android.com/media/media3/exoplayer
- https://square.github.io/okhttp/
- https://jsoup.org/license

Las licencias de software no conceden derechos sobre los catálogos ni los videos de los proveedores.

Raíz pública GTS Root R4: https://pki.goog/repo/certs/gtsr4.pem — repositorio oficial https://pki.goog/repository/

NativeStreams.java adapta los protocolos de UnsExtractor y VoeExtractor del proyecto yuzono/anime-extensions (Apache-2.0). Se conserva THIRD_PARTY_APACHE_LICENSE.txt.
- https://github.com/yuzono/anime-extensions/blob/master/src/es/animeav1/src/eu/kanade/tachiyomi/animeextension/es/animeav1/UnsExtractor.kt
- https://github.com/yuzono/anime-extensions/blob/master/lib/voeextractor/src/aniyomi/lib/voeextractor/VoeExtractor.kt

ByseStreams.java adapta FilemoonExtractor de yuzono/anime-extensions (Apache-2.0), con límites de tiempo y ejecución en segundo plano. Referencia: https://github.com/yuzono/anime-extensions/blob/master/lib/filemoonextractor/src/aniyomi/lib/filemoonextractor/FilemoonExtractor.kt

AndroidX Leanback 1.2.0 y RecyclerView se usan mediante Maven (Apache-2.0). SmartTube se consultó como referencia de arquitectura TV; no se copió código. Referencias: https://developer.android.com/reference/androidx/leanback/widget/VerticalGridView y https://github.com/yuliskov/smarttube. Inspiración visual: https://dribbble.com/search/google-tv.
