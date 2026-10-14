# Retirada de puntuaciones 0.28.0

A petición del usuario se elimina el sistema de estrellas.

- Sin notas ni estrellas en catálogo o ficha; sin texto «Puntuación» en la cabecera.
- Eliminados los clientes Jikan/AniList, la cola de consultas y su caché. Las preferencias antiguas de puntuación se limpian al abrir la app; no se toca el historial, favoritos ni ajustes.
- Se conservan las etiquetas compactas de tipo/estado, el selector de fuente, los márgenes del buscador, la separación entre episodios y la hora local de publicación.
- Las pruebas exclusivas de la funcionalidad retirada se sustituyen por comprobaciones de catálogo/ficha sin notas y foco del selector.

No se ha generado un nuevo APK. El APK anterior 0.27.0 se conserva.

Validación: 275 pruebas aprobadas; lint sin errores. Se comprobó visualmente catálogo y ficha sin estrellas, y se verificó que el código principal no contiene los endpoints de Jikan/AniList ni sus conexiones de interfaz.

Referencia consultada: el código público de AnimeLatino/animelatinohd2 muestra `data.vote_average` y obtiene la ficha en `getServerSideProps` desde `anime/{slug}`. En ese diseño la nota llega con los datos de la ficha. No demuestra el origen ni la actualización de las notas en la web desplegada actualmente.
