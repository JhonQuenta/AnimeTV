# AnimeTV

Aplicación nativa para Android TV, en fase beta, diseñada para navegar con mando.

## Fuentes incluidas

AnimeAV1, Tioanime, Latanime, AnimeLatinoHD y JKAnime vienen incluidas en el APK. No hay que instalar extensiones ni configurar repositorios. Las correcciones de fuentes se distribuyen mediante nuevas versiones de AnimeTV. Las puntuaciones de estrellas están retiradas.

## Funciones

- Catálogo, búsqueda, filtros, calendario y favoritos.
- Selección de idioma y servidores disponibles por episodio.
- Seguir viendo con progreso guardado, eliminación manteniendo OK y limpieza desde Ajustes.
- Lista o miniaturas de episodios, bloques para series largas y orden ascendente/descendente.
- Reproductor con calidad disponible, velocidad y saltos configurables mediante el mando.
- Fondos cuando la fuente proporciona imágenes adecuadas y logos de los proveedores.

## Descargar e instalar

Los APK se adjuntan a las Releases de GitHub cuando se publique el repositorio. Versión preparada: **0.30.0**, código 43.

| Archivo | Dispositivo |
|---|---|
| AnimeTV-0.30.0-armeabi-v7a.apk | Android TV ARM de 32 bits; incluye la Xiaomi TV 4S usada durante el desarrollo |
| AnimeTV-0.30.0-arm64-v8a.apk | Android TV con sistema ARM de 64 bits |
| AnimeTV-0.30.0-universal.apk | Android TV ARM32 o ARM64; contiene ambas arquitecturas |

Universal significa ARM32 + ARM64: no incluye x86. La arquitectura corresponde al sistema Android, no solo al procesador. SDK mínimo 23 (Android 6). La interfaz requiere Android TV/Leanback; no es una edición para teléfonos. Instalar encima de una versión con el mismo identificador y certificado conserva los datos.

## Compilar

Requisitos: JDK 17, Android SDK con plataforma 36, conexión para descargar dependencias y Gradle Wrapper incluido. Configura tu SDK en local.properties, que no se publica.

```sh
./gradlew testDebugUnitTest lintRelease
./gradlew assembleDebug
```

Para release, configura mediante variables de entorno ANIMETV_KEYSTORE (ruta a tu clave privada), ANIMETV_STORE_PASSWORD, ANIMETV_KEY_ALIAS y ANIMETV_KEY_PASSWORD. No se incluye ninguna clave ni contraseña en el código; empaquetar release sin credenciales falla explícitamente.

```sh
./gradlew assembleRelease                 # ARM32 por defecto
./gradlew assembleRelease -PallAbis=true  # ARM32, ARM64 y universal
```

En Windows usa gradlew.bat. Los resultados están en app/build/outputs/apk/. Para tu propia distribución usa un certificado propio y conserva una copia privada: una firma diferente no puede actualizar las instalaciones oficiales existentes. Las entregas actuales conservan el certificado de desarrollo original para permitir actualizar las versiones anteriores; no se proporciona su clave privada. Ver RELEASE.md.

## Arquitectura y comprobaciones

Cinco bibliotecas de fuente, extractores compartidos y contratos en source-core. Interfaz, reproductor e historial permanecen en app. Ver ARCHITECTURE.md y VERIFICACION-0.30.0.json para los resultados de esta entrega. Las pruebas usan Robolectric; no sustituyen una prueba física con mando y reproducción en la TV.

## Limitaciones conocidas

- Las páginas y servidores externos pueden cambiar, bloquear solicitudes o eliminar episodios.
- AnimeLatinoHD puede solicitar verificación mediante WebView; mantener Android System WebView actualizado.
- Desu/Magi y MP4Upload de JKAnime pueden sufrir pausas por velocidad del servidor. No se garantiza fluidez por cambiar de arquitectura o aumentar el búfer.
- La compatibilidad y velocidad de decodificación dependen del dispositivo y del formato real del vídeo.
- ARM64 necesita comprobación en un dispositivo compatible; la generación del APK no demuestra reproducción física.

## Informar de un problema

Incluye versión de AnimeTV, dispositivo, versión de Android, fuente, anime, episodio y servidor. En errores de reproducción adjunta el informe disponible en Ajustes, revisando antes si contiene información privada. No adjuntes contraseñas ni cookies.

## Licencia

El código original de AnimeTV se distribuye bajo GPL-3.0-only; ver LICENSE y THIRD_PARTY_NOTICES.md. Los componentes externos conservan sus licencias. Acompañar los APK con AnimeTV-0.30.0-source.zip y AnimeTV-0.30.0-third-party-source.zip. Los logos identifican a sus respectivos proveedores; la licencia del código no concede derechos sobre marcas, imágenes ni vídeos de esos sitios. AnimeTV no aloja vídeos ni está afiliada a las fuentes.

Las notas antiguas están en HISTORY.md; no describen necesariamente la versión actual.
