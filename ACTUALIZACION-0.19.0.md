# AnimeTV 0.19.0 — navegación y diseño para TV

El catálogo conserva un único selector «Fuente: …» en el encabezado, con el logo del proveedor. Buscar, voz y filtros son botones de icono de 46 dp, alineados con el buscador, con descripciones accesibles. Desde estos botones se puede bajar al selector con el mando.

Se distribuyen los favicons oficiales de AnimeAV1, Tioanime, Latanime y AnimeLatinoHD en recursos locales. Se utilizan en el selector, los encabezados, Seguir viendo, la ficha y las preferencias por fuente. Los ICO se convierten a PNG conservando el dibujo y la resolución del archivo original, sin recrear los logos. Latanime publica un favicon de 16×16; AnimeLatinoHD publica actualmente el icono triangular de su sitio.

La portada de la ficha conserva 164×246 dp. El encabezado fluye según el contenido real: publicación, géneros y sinopsis ausentes no reservan filas. Los botones siguen a la sinopsis. Favoritos tiene un botón cuadrado independiente y debajo se coloca el logo y el nombre del proveedor, con altura natural y sin recortar el texto. Se mantienen los márgenes de seguridad de 24 dp y el desplazamiento del contenido.

El estado de Latanime se lee de las etiquetas y atributos explícitos del sitio. Se reconocen Finalizado, En emisión y Estreno. En las dos fichas de Frieren, Latino y Castellano, la web publica solamente «Estreno». Eso no establece si el doblaje terminó: se muestra el dato de la fuente y no se infiere Finalizado por el año o la cantidad de episodios. Cuando no existe ningún estado publicado, no se inventa uno.

En episodios con varios idiomas, primero aparece el selector de audio. Después se muestran solamente los servidores de ese idioma. Esto también se respeta con un servidor predeterminado o automático: los reintentos no saltan a otro idioma. Con un único idioma, se pasa directamente a sus servidores. «Cambiar idioma» permite volver al paso anterior. Volver de otra aplicación conserva el servidor elegido. Se evita precargar enlaces de varios idiomas antes de la elección.

El reproductor muestra el tiempo actual a la izquierda y la duración total al extremo derecho de la barra, manteniendo la posición y los controles existentes.

Ajustes se organiza en dos columnas: categorías a la izquierda y opciones a la derecha. Reproducción incluye audio y velocidad; Control remoto incluye los saltos; Fuentes conserva servidores predeterminados independientes y sus logos; Avanzado contiene salida de vídeo y diagnóstico. El foco permite entrar a las opciones con derecha y regresar a la categoría con izquierda. Las opciones se guardan como antes.

Se genera únicamente el APK armeabi-v7a para la Xiaomi TV 4S de 32 bits, según la preferencia del usuario. Se mantienen el certificado de actualización, Android mínimo 23, minificación y reducción de recursos. No se generan variantes universal ni ARM64. Se conservan los fuentes de la app y de las bibliotecas GPL.

Se comprueban geometría del buscador, logos, etiqueta sin recorte, portada uniforme, ausencia de huecos por datos ausentes, categorías y preferencias, separación de idiomas, salto directo con un idioma y extremos de los tiempos. Las imágenes de prueba usan datos simulados; el resultado en la TV física requiere la prueba del usuario.
