# AnimeTV 0.21.0 — optimización preparada, sin APK

## Desu y Magi

La muestra de Psyren 2 entrega HLS con H.264 High, nivel 4.0, y audio AAC. No es AV1. Un segmento de 5,09 s mide 9.040.920 bytes, y el siguiente de 2,50 s mide 3.920.928 bytes. El segundo tardó 43.063 ms en descargarse en la comprobación de red.

Las réplicas publicadas del CDN entregan prefijos idénticos y longitudes coincidentes. PlaybackDataSources detecta transferencias extremadamente lentas en los hosts concretos cdn1–cdn6.ducvomes.com, exclusivamente para segmentos TS de un reproductor propio de JKAnime. A partir de seis segundos y menos de 250 KB/s, interrumpe esa transferencia para que Media3 reintente, y pone el nodo en cuarentena dos minutos. El siguiente intento compara 64 KiB de datos, código HTTP, longitud y Content-Range antes de aceptar una réplica. Si no coinciden, conserva el origen. Se conserva HTTPS y su validación de certificados. No se cambian otras fuentes ni se ocultan servidores.

Prueba real del lector nuevo: detectó la transferencia lenta; el segundo intento descargó los 3.920.928 bytes desde cdn1 en 3.440 ms, con prefijo idéntico. El búfer de inicio de Desu/Magi se eleva de 0,75 a 4 s y la recuperación de 1,5 a 6 s; asignación objetivo de 40 MiB, con límite temporal de 30 s. Se registran decodificador, fotogramas descartados y segmentos lentos en el diagnóstico local.

Esto no confirma todavía la fluidez en la TV física. Una conexión local insuficiente o todas las réplicas lentas pueden seguir provocando esperas; no hay una variante de menor calidad publicada en la lista comprobada.

## Carga y búsquedas

- Caché limitada de catálogo/búsqueda en AnimeAV1, Tioanime, Latanime y JKAnime (16 entradas, 2 minutos). AnimeLatinoHD conserva su caché ya existente.
- Las consultas sustituidas por otra pantalla o búsqueda cancelan su llamada HTTP real. Volver desde una ficha cancela su carga y sugerencias pendientes.
- Las búsquedas combinadas con filtros consultan hasta tres fichas en paralelo, preservan orden y no descartan todos los resultados por una ficha inaccesible. Si ninguna ficha puede comprobarse, se informa del error.
- JKAnime carga las páginas restantes de episodios y las portadas de relaciones con hasta tres solicitudes simultáneas. Se siguen validando paginación y total; no se crean episodios por suposición ni se devuelve una lista truncada.
- Saga y sugerencias usan un ejecutor independiente de la carga principal. Las relaciones de la saga aparecen antes de completar el relleno; se detiene la búsqueda al reunir diez entradas y se evitan redibujados iguales. Se conserva el foco de la tarjeta durante actualizaciones.

Medidas de búsqueda «sword art online» en esta máquina: JKAnime 1.409 ms, Tioanime 562 ms, Latanime 880 ms y AnimeAV1 1.177 ms. La repetición desde caché de cada adaptador quedó por debajo de 1 ms. Son tiempos de consulta, no una promesa de tiempo total de interfaz en la TV. AnimeLatinoHD depende además de la verificación/renderizado del navegador.

## Aviso al adelantar o retroceder

Pulsaciones seguidas en la misma dirección acumulan el indicador (+5, +10, +15 s; -5, -10, -15 s). La dirección contraria, una pausa de más de 1,2 s o un cambio de episodio reinician la cuenta. Cada salto real se aplica inmediatamente.

No se ha generado APK: se conserva exactamente el APK anterior 0.20.0. La incorporación de las nuevas fuentes queda pendiente de las URLs que indique el usuario.
