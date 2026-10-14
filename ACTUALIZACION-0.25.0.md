# Paginación y puntuación en ficha 0.25.0 — sin APK

Incluye todos los cambios preparados anteriores. No se genera APK.

- La ficha muestra la estrella y nota de MyAnimeList junto al tipo, año y estado. Usa la misma caché persistente del Catálogo y no bloquea la carga de la ficha. Sin una coincidencia fiable no muestra una nota inventada ni reserva una fila vacía.
- Al regresar desde una ficha o restaurar el Catálogo, se retoma la comprobación pendiente de la siguiente página sin reemplazar las tarjetas ni perder su posición.

- Antes se mostraba Siguiente si la página actual tenía cualquier resultado. Ahora se consulta la página siguiente en segundo plano y solo se añade el botón si contiene URLs reales distintas de las de la página actual.
- La página actual se dibuja inmediatamente: no espera a la comprobación de la siguiente. No se muestra un botón de avance provisional o que conduzca deliberadamente a una página vacía.
- Si una fuente devuelve la misma página al pedir otra, no se considera que haya más resultados. Si la comprobación falla, no se inventa una página siguiente y la página actual permanece utilizable.
- La página comprobada se guarda en la caché del catálogo y se reutiliza al avanzar. La clave incluye búsqueda, número de página y filtros ordenados.
- JKAnime devuelve la búsqueda completa en una sola respuesta: ahora se conserva esa lista para dividirla y comprobar la siguiente página sin volver a descargar la búsqueda.
- Al pasar páginas rápidamente, las consultas de notas canceladas mientras esperaban ya no reservan turnos futuros en Jikan. Se conserva el intervalo de 1,2 segundos entre solicitudes y las pausas por error/HTTP 429.
- Anterior aparece en todas las páginas posteriores a la primera, incluso si la página está vacía o hubo un error de carga. En esas situaciones recibe el foco del mando.
- El botón Atrás del mando, cuando estás en el Catálogo y la página es mayor que uno, regresa a la página anterior conservando búsqueda y filtros. Desde una ficha sigue regresando primero al catálogo.
- Al usar los botones de página se lleva el foco a los resultados de la página cargada. Las respuestas antiguas no pueden añadir Siguiente ni restaurar el foco después de otra navegación.

Pruebas: búsqueda corta Sword Art Online sin avance, dos páginas con avance confirmado y página siguiente en caché, regreso conservando búsqueda/filtros, página vacía con foco en Anterior, página con error y regreso funcional, fuente que repite la misma página y comprobación lenta que no bloquea las tarjetas actuales. Regreso desde una ficha con comprobación pendiente; nota 7,6 de Ordinal Scale desde caché junto a año/estado y ausencia de fila vacía sin nota. Se conserva la suite de las cinco fuentes.

Límite: la comprobación puede tardar lo que tarde la fuente. Durante ese intervalo Siguiente permanece ausente. Un error externo al consultar más resultados no permite afirmar que existan; se mantiene accesible la página actual y el regreso. Las consultas reales a Jikan tuvieron tiempos de espera en este entorno en la validación previa; la integración se prueba con respuestas controladas y caché. Comprobación física en la TV pendiente.
