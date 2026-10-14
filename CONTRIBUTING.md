# Contribuir

Consulta README.md y ARCHITECTURE.md antes de modificar el proyecto. Usa JDK 17 y el Gradle Wrapper.

Las cinco fuentes están incluidas como bibliotecas internas. Los cambios de catálogo, filtros o ficha van en su módulo de fuente; los protocolos comunes de servidores van en extractors. La interfaz y reproducción permanecen en app. Conserva los IDs de fuentes y claves de almacenamiento para no romper favoritos y progreso.

Ejecuta `./gradlew testDebugUnitTest lintRelease`. Para cambios de parsers utiliza fixtures públicas y pruebas que reproduzcan el fallo. No guardes cookies, credenciales, enlaces privados ni claves de firma en fixtures o commits. Las comprobaciones automatizadas no certifican fluidez física en una TV.

Describe el problema, la conducta resultante y las comprobaciones en el pull request. Mantén las licencias y procedencia de cualquier código externo. No incluyas SDK, cachés, resultados de compilación ni local.properties.
