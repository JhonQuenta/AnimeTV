# AnimeTV 0.14.1

## Ficha del anime

Fondo carbón liso, sin descargar ni ampliar miniaturas de episodios. Margen de 24 dp en los cuatro lados, portada de 164 × 246 dp, separación de 24 dp entre portada e información y acciones compactas. El título de capítulos y los controles de orden y vista comparten una fila. Se conservan la lista compacta, las miniaturas, el progreso y los bloques para series de al menos 101 capítulos.

El adaptador actual de AnimeAV1 obtiene portadas y miniaturas de episodios, pero no una imagen horizontal independiente del anime. Esto no demuestra que la página nunca tenga otras imágenes; la aplicación deja de reutilizar miniaturas como fondo.

## Reproductor

El encabezado muestra el nombre del anime y el episodio actual y se actualiza al cambiar de episodio. Hay 10 dp entre el salto configurable y Episodios. El salto de canción se conserva únicamente como botón del reproductor; se elimina su duplicado del menú de reproducción.

## Logo del launcher

El gato y el texto AnimeTV se reconstruyeron como vectores. Se eliminaron las imágenes PNG y recursos de launcher sustituidos. El banner conserva proporción 16:9, fondo azul oscuro y margen interior; el icono cuadrado tiene una versión adaptativa para Android 8 o posterior y una alternativa para versiones anteriores. El logo dentro de la aplicación tiene fondo transparente.

Las fotos muestran bordes dentados, compatibles con rasterización y ampliación de un recurso pequeño. No permiten identificar por sí solas el comportamiento exacto del launcher. Los vectores evitan partir de un bitmap de baja resolución, pero el launcher puede volver a rasterizar o mantener recursos en caché. La nitidez con y sin foco requiere comprobarse en el televisor.

Referencia oficial: [TV app icon guidelines](https://developer.android.com/design/ui/tv/guides/system/tv-app-icon-guidelines), que distingue banner 16:9 e icono 1:1 y recomienda recursos adaptativos.

## Carga y limpieza

- Caché compartida y acotada de 48 fichas durante cinco minutos; consultas concurrentes de la misma ficha comparten trabajo. Los errores permiten reintento inmediato.
- Caché de filtros durante una hora, para evitar descargar el catálogo al abrirlos repetidamente.
- Las vistas de imágenes separadas de la pantalla se retiran de las suscripciones pendientes de descarga.
- Muestreo de imágenes por potencias de dos según la densidad del dispositivo; se evita reducir por debajo del ancho solicitado.
- Se retiran el fondo de ficha, la configuración obsoleta de margen exterior, una lista temporal sin uso y recursos antiguos de marca.

No se midieron FPS ni tiempos en una TV física. Estas mejoras reducen trabajo redundante sin cambiar los enlaces de reproducción ni inventar calidades.

## Validación e instalación

Pruebas unitarias y de interfaz con Robolectric/API 28, compilación debug y Android Lint. Las capturas de comprobación usan datos de prueba. No hay un televisor físico conectado.

Instalar AnimeTV-0.14.1-debug.apk encima de la versión anterior. Se conservan el identificador pe.animetv y el certificado de desarrollo para mantener favoritos, progreso y ajustes.
