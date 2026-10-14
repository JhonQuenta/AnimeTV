# AnimeTV 0.14.10: distribución y carga de portadas

El selector muestra Voe - 720p, Byse - 1080p, HLS - 1080p y MP4Upload - 1080p. Estas etiquetas son las calidades habituales del proveedor; la selección de calidad del vídeo sigue leyendo las pistas reales de Media3 y no inventa resoluciones.

El APK 0.14.9 ocupaba 50.785.512 bytes. Las bibliotecas nativas AV1/FFmpeg para cuatro arquitecturas aportaban 35.605.688 bytes; x86 y x86_64, destinadas a emuladores y dispositivos Intel, aportaban 20.610.312. Esta entrega conserva ARM de 32 y 64 bits y excluye Intel. El APK universal ARM sirve para ambas; también se generan APK por arquitectura.

La entrega utiliza R8 y eliminación de recursos sin uso. Las clases y miembros de FFmpeg accesibles por JNI se preservan explícitamente. Se mantienen todos los códecs incluidos, el respaldo AV1, audio de Android, gráficos, calidad de las portadas, caché de vídeo y navegación. Firma e identificador iguales a los anteriores, con versionCode 26.

Las portadas tienen ahora una caché HTTP compartida de hasta 16 MiB en el directorio de caché de la app. Respeta Cache-Control, validación y no-store del servidor; no fuerza contenido obsoleto ni cambia los bytes o resolución. Reduce nuevas descargas cuando se descarta una imagen de la caché de memoria y el servidor permite reutilizarla. Su límite de disco es independiente del tamaño del APK.

Verificación: pruebas del selector, renderers, HTTP, extracción y navegación; pruebas nuevas para reutilizar portadas válidas y respetar no-store; compilación y lint release; firma, arquitecturas y bibliotecas AV1 del APK final. No se ha ejecutado este APK optimizado en una TV física. La prueba previa de HLS confirmó carga y avance, pero AV1 1080p por software sigue sin ser fluido en la Xiaomi MiTV-MSSP3.
