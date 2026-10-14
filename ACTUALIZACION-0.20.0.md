# AnimeTV 0.20.0 — JKAnime

JKAnime es la quinta fuente. Incluye su favicon oficial local en el selector y en indicadores de origen, servidor predeterminado independiente, historial y favoritos conservando la fuente original.

- Catálogo con paginación nativa y filtros de género, tipo, estado, año y letra. Búsqueda paginada localmente; búsqueda + filtros contrasta la ficha real sin cargar sus páginas de episodios.
- Fichas con estado explícito, año, géneros y portada; peticiones de imágenes con el Referer que exige su CDN.
- Episodios obtenidos mediante la consulta anónima que utiliza la web, conservando cookies y token CSRF. Se cargan todas sus páginas y sus números reales; el contador HTML «0» no se usa para inventar episodios. Una página repetida provoca un error claro en vez de truncar silenciosamente el listado.
- Inicio y calendario de la web. Relaciones explícitas entre temporadas se priorizan antes de las sugerencias por título/género; se obtienen sus portadas.
- Desu/Magi: HLS propio; Desuka: reproductor antiguo. VOE, MP4Upload, Mixdrop, Filemoon y Doodstream reutilizan extractores existentes. Streamwish descodifica datos empaquetados sin ejecutar JavaScript e intenta las rutas de vídeo publicadas. Mega acepta enlaces oficiales /embed/ y conserva descifrado durante reproducción por rangos.
- Las calidades de JKAnime no se inventan a partir del nombre del alojamiento. Todos los servidores soportados publicados permanecen visibles; no se aplica filtrado automático por códecs.

## Comprobaciones reales

Episodios: Psyren 2 y Sword Art Online 11. Las pruebas de HLS verifican lista y un segmento real, y las de MP4 verifican una muestra por rango (descifrada en Mega). Esto no sustituye una prueba de fluidez en la TV física.

Desu, Magi, VOE y Mega: accesibles en ambos episodios. Mixdrop: accesible en Psyren, enlace eliminado en SAO. MP4Upload: accesible en SAO, timeout en Psyren. Filemoon/Byse: 404 en Psyren. Desuka: 410 en SAO. Doodstream: fallo TLS en Psyren y dominio no resoluble en SAO. Streamwish: Psyren accesible; SAO devuelve errores de servidor también al comprobar las tres rutas alternativas publicadas.

El catálogo devuelve 30 entradas; búsqueda «sword art online», 14; combinada con 2012/Serie/Finalizado, 1. Fichas: Psyren 2 episodios, SAO 25. Inicio 70 entradas y calendario 74 distribuidas en siete días (datos del sitio en el momento de la prueba).

La comprobación adicional confirmó cuatro relaciones explícitas de SAO, con Extra Edition primero y portadas HTTP 200.

Distribución final: únicamente APK armeabi-v7a para la TV del usuario, según BUILD_PREFERENCES.md.

## Corrección adicional de fichas y estados

AnimeLatinoHD: Ordinal Scale y Scherzo incluyen un registro T de texto sin salto de línea antes del siguiente registro JSON. El lector anterior descartaba también los datos del anime/episodios que seguían al texto. NextPage ahora respeta la longitud en bytes UTF-8, incluidos acentos, varias líneas y bloques repartidos entre scripts. Un bloque truncado falla claramente. Se comprobó el formato con los scripts públicos reales del navegador y se añadieron pruebas de regresión para películas, estados y episodios. El navegador confirmó Finalizado y un episodio en ambas películas. Metadata utiliza el mismo lector y mantiene la verificación no interactiva en producción.

Latanime: la consulta de metadatos de las tarjetas agrupadas usa primero una ficha real ya observada en catálogo/inicio. No prueba otros doblajes ni exige episodios disponibles para mostrar el estado; el listado completo al entrar al anime conserva su carga de audios. Los ejemplos Dragon Ball Super: Beerus, Avatar: Los Siete Refugios, Tank Chair y Mezametara publican únicamente «Estreno». Se conserva esa etiqueta, sin deducir Finalizado/En emisión a partir del año o del número de episodios. Prueba específica con ficha sin episodios aprobada.

Resultado actualizado: 212 pruebas aprobadas; lint sin errores. Comprobaciones previas a la entrega final. Registro: VERIFICACION-FICHAS-0.20.0.json.

## Seguir viendo y entrega para la TV

Mantener OK abre un menú para eliminar la tarjeta de Seguir viendo, con la opción Cancelar. El gesto usa el tiempo de pulsación larga del sistema; la pulsación normal conserva el comportamiento de la tarjeta. Repetir OK no abre varios menús y la liberación del botón que abrió el menú no confirma una eliminación. Perder el foco o retirar la tarjeta cancela el gesto pendiente.

Ajustes → Reproducción incorpora «Limpiar Seguir viendo», con confirmación para eliminar todas las tarjetas de todas las fuentes. Las dos acciones conservan favoritos, último episodio y progreso de reproducción; al volver a reproducir una serie se incorpora otra vez a Seguir viendo. El Inicio se actualiza inmediatamente tras eliminar una tarjeta y conserva el foco en una tarjeta cercana, o en Inicio si no quedan tarjetas.

La distribución final incluye JKAnime, las correcciones de fichas/estados y estas opciones de limpieza, únicamente para armeabi-v7a.
