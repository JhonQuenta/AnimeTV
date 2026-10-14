# Ajustes de presentación 0.27.0

Incluye 0.26.0. No se ha generado un nuevo APK.

- Capítulos en lista: separación de 8 dp entre filas. El modelo de filas conserva los márgenes verticales al añadir los márgenes laterales del diseño con fondo; antes esa copia descartaba el espacio inferior. Arriba/abajo selecciona explícitamente el episodio vecino en la lista, también cuando debe adjuntarse una fila durante el desplazamiento; los extremos conservan la navegación habitual hacia otros controles.
- Etiqueta de tipo: Serie/TV/Película se ajusta al ancho del texto, dentro de la columna alineada a la derecha. La columna para organizar tipo/estado junto a la puntuación ya no estira la etiqueta de tipo.
- Buscador: margen derecho de 8 dp, igual al selector de fuente. La barra cede ese espacio y los tres botones siguen midiendo 46×46 dp, alineados con el buscador. Se conserva la navegación hacia el selector con el mando.
- Última publicación: fechas ISO UTC de AnimeLatinoHD se muestran como `12 oct 2026 · 09:52` según la zona horaria de la TV. Funciona también con datos guardados por versiones anteriores. Los textos relativos existentes permanecen intactos.
- Puntuaciones: el usuario confirmó que aparecen después de esperar. Se mantiene la consulta asíncrona y la caché existente; este cambio no modifica proveedores ni inventa notas ausentes.

Verificación de geometría renderizada, foco del mando, ancho real de la etiqueta, margen del buscador en ambos estados del menú y conversión horaria, incluyendo cambio de día.
