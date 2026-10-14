# Fuentes y extractores en módulos internos 0.29.0

Incluye la retirada de estrellas de 0.28.0 y todos los cambios anteriores. No se ha generado APK ni publicado nada en GitHub.

- Cinco bibliotecas Android independientes: `sources:animeav1`, `sources:tioanime`, `sources:latanime`, `sources:animelatinohd`, `sources:jkanime`.
- `source-core`: modelos/contrato, cliente HTTP independiente de AnimeAV1, utilidades de consultas/filtros y metadatos de las fuentes.
- `extractors`: protocolos de los servidores compartidos y entrada pública única `StreamExtractors`. Las cinco fuentes la reutilizan conservando URLs, referers, idiomas, cachés y los protocolos actuales.
- La app conserva interfaz, reproductor, favoritos, Seguir viendo, preferencias, búfer, compatibilidad, imágenes, recomendaciones y verificación web con el mando.
- La app consulta capacidades del contrato para precarga; registro de fuentes por ID/dominio y servidores declarados por cada módulo. Se mantienen los IDs usados por el historial.
- AnimeLatinoHD recibe páginas protegidas mediante `SourcePageAccess.Host`, sin depender de Activity ni WebView dentro del módulo.
- Dependencias sin ciclos: fuentes → extractors/source-core; extractors → source-core; app → bibliotecas. Ninguna biblioteca necesita otra fuente o la app.
- Sin extensiones descargables por ahora. Esta separación no permite actualizar fuentes ya empaquetadas sin publicar otro APK. Instalación, firmas, compatibilidad y recuperación se diseñarán cuando compense esa distribución.

Ver ARCHITECTURE.md para el mapa, contratos y límites. Se conserva el paquete Java para reducir riesgos; las bibliotecas usan namespaces de recursos separados.

Validación: 279 pruebas aprobadas; lint sin errores en los ocho módulos. Optimización release con R8 verificada sin empaquetar APK. Comprobación física en TV pendiente.

Sobre puntuaciones: GitHub puede alojar un dataset sincronizado, pero no proporciona por sí mismo una API de notas que elimine las consultas por título. El dataset público revisado `manami-project/anime-offline-database` incluye puntuaciones y está archivado. No se reactiva ese sistema.
