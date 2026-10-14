# AnimeTV 0.14.2

## Logo aprobado

Se restaura el gato, la tipografía y el fondo originales desde los recursos de 0.14.0. El banner PNG mide 1672 × 940 píxeles y la marca transparente 1254 × 1254. No se redibujan ni se estiran sus proporciones. Los recursos bitmap activan el filtrado y antialiasing. El banner se declara también en la actividad que abre el launcher. El icono adaptativo conserva el gato original.

El APK de TV App Repo proporcionado contiene un banner PNG de 320 × 180 y variantes de icono por densidad. Se inspeccionaron sus recursos y la declaración del manifest sin ejecutar ni instalar ese APK. Su [generador público](https://github.com/ITVlab/TvAppRepo/blob/master/python/leanbackshortcut.py) incorpora un PNG como drawable y lo declara como banner; no convierte el logo en otro dibujo. También se revisó el envío de imágenes en [ShortcutPostTask](https://github.com/ITVlab/TvAppRepo/blob/master/app/src/main/java/news/androidtv/tvapprepo/utils/ShortcutPostTask.java). No se copió código de ese proyecto.

La captura local del banner restaurado se revisó a 1280 × 720. No se puede garantizar el resultado del escalado o de la caché del launcher sin probarlo en el televisor.

## Episodios y bordes

Hay 12 dp de espacio bajo los controles de orden y vista. La lista compacta representa el avance mediante un relleno horizontal que ocupa toda la altura de la fila: empieza vacío y se completa al avanzar. El texto permanece encima del relleno y el borde de foco sigue visible. Los episodios marcados como vistos tienen relleno completo y una marca de verificación. Las tarjetas con miniatura conservan su indicador de progreso.

Se conservan los márgenes de inicio de la ficha, pero se elimina el recorte dentro del padding de la lista. El carrusel y el contenedor de recomendaciones dejan de cortar sus hijos antes de alcanzar el borde real de la pantalla. Las recomendaciones se pueden seguir desplazando con el mando.

## Capítulos recientes

Al abrir un capítulo desde Inicio se muestra la ficha y una pregunta con el episodio seleccionado. «Ver capítulo» inicia el flujo de reproducción; «Elegir otro capítulo» conserva la ficha y enfoca la lista. Atrás cierra la pregunta. No se precargan servidores para esa entrada antes de la elección. Si existe progreso, se conserva la elección posterior entre reanudar y empezar desde cero.

## Validación

Pruebas unitarias y nativas de interfaz con Robolectric/API 28, compilación del APK y Android Lint. Se comprueban el relleno proporcional, el espacio de controles, la restauración del recurso original y la ausencia de lanzamiento antes de elegir. Las capturas utilizan datos de prueba; no hay TV física conectada.

Instalar AnimeTV-0.14.2-debug.apk encima de la versión anterior para conservar los datos. Se mantienen pe.animetv y el certificado de desarrollo.
