# Mando y trabajo en segundo plano 0.24.0 — sin APK

Incluye los cambios preparados de 0.21.0, 0.22.0 y 0.23.0. No se genera APK ni cambia la arquitectura de distribución autorizada (armeabi-v7a).

## Navegación con mando

- Selector de fuentes: recorrido explícito vertical con Arriba/Abajo entre las cinco opciones y el pie fijo Cancelar. El ScrollView retenía el foco en la última opción al buscar un elemento fuera de su árbol; se corrige en el diálogo.
- Al abrir, enfoca la fuente activa; las opciones admiten foco programático también si el sistema quedó en modo táctil. Los bordes superior e inferior no envían el foco fuera del menú.
- Atrás/cerrar devuelve el foco al botón que abrió el selector, si sigue en pantalla. No modifica la fuente al cancelar.
- Las pruebas envían eventos reales DPAD_DOWN, DPAD_UP y BACK al diálogo; no se limitan a comprobar atributos nextFocus.

## Fuentes, filtros, búsqueda y servidores

- Consultas de opciones de filtros en un ejecutor separado: varias pulsaciones durante la carga comparten una consulta y no abren diálogos superpuestos.
- Cambiar de pantalla cancela la consulta pendiente y evita ventanas de filtros tardías. Abrir el selector de fuentes también invalida esa consulta con un contador independiente, aunque no haya cambiado el catálogo.
- Precarga de servidores separada del catálogo/búsqueda y cancelada al cambiar de pantalla. Consulta el episodio guardado para reanudar en vez de preparar siempre el primero. Conserva los mismos servidores, audios, cabeceras y comportamiento de reproducción.
- Se mantienen las cachés de catálogo/metadatos/opciones ya presentes en las cinco fuentes, las consultas de metadatos con paralelismo máximo de tres y los filtros combinados con búsqueda basados en los datos reales de cada ficha. No se elimina validación de año, estado, tipo, género o idioma para ahorrar consultas.
- No se modifica el orden de resultados ni la paginación y no se aumenta la concurrencia contra los servidores. Las interrupciones, DNS, límites de servicio, códecs y ancho de banda del proveedor no se consideran resueltos por esta optimización.

## Imágenes sin reducir calidad

- Descarga cancelable de bytes originales. Interrumpir el trabajo cancela la llamada HTTP real, incluso si no ha llegado la respuesta.
- Cada descarga conserva sus consumidores; al desaparecer la última tarjeta que la necesita, se cancela el trabajo en cola o en curso. Otra tarjeta que comparta la imagen sigue recibiéndola.
- Cambiar la URL de una portada elimina su suscripción anterior. Un resultado o fallo antiguo no puede retirar los consumidores nuevos ni sobrescribir otra imagen asignada al mismo ImageView.
- Se conserva la caché HTTP de imágenes y el mismo muestreo/resolución, formato de bytes y tamaño de memoria de las versiones anteriores. No se recomprime ni reduce calidad para mejorar los tiempos.
- Las pruebas comprueban igualdad byte por byte, cancelación de socket, consumidores compartidos y reemplazo de imagen en una tarjeta.

## Límites de la comprobación

Pruebas locales con Robolectric API 28 y MockWebServer; se mantiene la suite de las cinco fuentes y sus búsquedas/filtros. No sustituye una prueba física en la Xiaomi TV 4S. No se publican porcentajes de mejora ni tiempos de Internet sin mediciones comparables.

Siguen pendientes las limitaciones registradas en 0.22/0.23: respuesta real de Jikan y disponibilidad DNS de Latanime desde este entorno. No se cambian proveedores ni se inventan resultados para ocultarlas.
