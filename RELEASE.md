# Preparación de una release

Publicar solamente este proyecto, no la carpeta de trabajo que contiene SDK, cachés, claves y herramientas locales.

1. Ejecutar testDebugUnitTest y lintRelease; revisar las advertencias y limitaciones en VERIFICACION-0.30.0.json.
2. Configurar las cuatro variables ANIMETV_* descritas en README.md; compilar assembleRelease -PallAbis=true.
3. Verificar versión, arquitecturas, certificado y firma con aapt y apksigner. Nunca publicar el keystore ni las contraseñas.
4. Probar en TV navegación, búsqueda/paginación, historial y reproducción. Etiquetar como beta mientras existan verificaciones físicas pendientes.
5. Adjuntar los tres APK, SHA256SUMS-0.30.0.txt, el código fuente completo y las fuentes de Nextlib/FFmpeg/dav1d/mbedTLS a la misma release.

Las entregas 0.30.0 conservan el certificado original de desarrollo, para permitir actualización sobre 0.27.0. Es una decisión de continuidad: configurar una nueva firma no convierte las instalaciones anteriores y requiere desinstalación o un proceso de migración. Las compilaciones públicas no deben usar una clave generada automáticamente por Gradle. La configuración ahora exige credenciales externas explícitas para empaquetar release. Guardar y respaldar la clave original fuera del repositorio.

No se ha creado ni publicado un repositorio remoto desde esta tarea.
