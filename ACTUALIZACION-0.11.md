# AnimeTV 0.11.0

- Compilación Java con codificación UTF-8 explícita para conservar tildes, ñ y símbolos.
- Progreso persistente por episodio, con duración y marca de visto desde el 95%. La marca se conserva al volver a ver el capítulo.
- Barras de avance en Seguir viendo y en los episodios de la ficha; se actualizan al regresar del reproductor.
- Elección Reanudar / Empezar de cero en episodios con posición guardada, tanto en la ficha como en el selector del reproductor.
- El reloj y la barra se actualizan cada segundo aunque los controles estén ocultos; abrirlos también actualiza inmediatamente el tiempo.
- Un inicio fallido sin primer fotograma conserva la posición anterior.
- Al volver de otra aplicación se conserva el servidor elegido y la pausa; se invalida el enlace temporal para resolver uno nuevo.
- Byse utiliza el mismo User-Agent en extracción y reproducción, dispone de más tiempo de lectura y tiene un reintento con enlace renovado tras un error de reproducción.

También incluye el orden ascendente/descendente, bloques de 100 para series con más de 100 capítulos y acceso directo al primer anime desde el menú del catálogo.

La duración y el porcentaje se conocen después de reproducir con esta versión. Los registros anteriores contienen solo una posición; el porcentaje aparecerá al obtener la duración.

Los cambios de Byse requieren validación en la TV Xiaomi: las pruebas de interfaz y estado no garantizan la disponibilidad de un servidor externo ni su comportamiento en hardware real.
