# Probar sin pasar archivos a mano

## Android TV en la laptop

Android Studio incluye Android Emulator. Abrir Device Manager > Create Device > TV, elegir un perfil de Android TV y descargar una imagen compatible. Para comparar con la Xiaomi, preferir Android 9/API 28 si está disponible en el gestor. Una imagen de teléfono no sustituye el entorno de TV.

Abrir este proyecto en Android Studio, seleccionar ese dispositivo virtual y pulsar Run. Android Studio compila, instala y abre la app. También se puede instalar el APK en un emulador abierto con adb install -r. El teclado y el control virtual permiten probar la navegación direccional.

Esto comprueba interfaz, navegación y comportamiento de la aplicación. Los códecs, decodificadores y rendimiento de la Xiaomi pueden comportarse de forma diferente. Hace falta virtualización habilitada y recursos suficientes para el emulador; no se han verificado las especificaciones de esta laptop.

Documentación: https://developer.android.com/studio/run/managing-avds y https://developer.android.com/studio/run/emulator

## Actualizar directamente la TV con ADB

Si la TV permite habilitar depuración por red y autoriza la laptop, ambas pueden conectarse en la misma red. Después se puede instalar cada actualización directamente, sin copiarla con USB ni aplicaciones de transferencia:

```powershell
adb connect IP_DE_LA_TV:5555
adb install -r "AnimeTV-0.6.0-debug.apk"
adb shell am start -n pe.animetv/.MainActivity
```

El puerto debe ser el que exponga la TV. No se presupone que 5555 esté habilitado. Primero se comprueba la configuración de depuración y se acepta la autorización que aparezca en el televisor.

Android 9 no tiene el flujo moderno de emparejamiento inalámbrico de Android 11. El método estándar para versiones antiguas usa una conexión inicial de depuración USB y adb tcpip; algunos televisores exponen ADB por red directamente. Hay que revisar las opciones concretas de esta Xiaomi antes de elegir el método. No se debe conectar dos puertos USB host con un cable improvisado.

Documentación: https://developer.android.com/tools/adb



