# Eira Display 1.2 — Auto Update

La aplicación ahora:
- comprueba automáticamente si existe una versión nueva;
- descarga el APK sin tener que pasarlo manualmente a la tablet;
- abre directamente el instalador Android;
- conserva el modo launcher, pantalla completa y caché rápida.

## Limitación Android
Una app Android normal NO puede reemplazarse a sí misma de forma silenciosa.
Android mostrará una confirmación de instalación por seguridad.

En la primera actualización también puede pedir:
"Permitir instalar aplicaciones desconocidas" para Eira Display.
Se habilita una sola vez.

## Backend Eira Infra
Worker 1.4.3.4 agrega:
`/api/display/app/latest`

Variables de Worker:
- DISPLAY_APP_VERSION_CODE
- DISPLAY_APP_VERSION_NAME
- DISPLAY_APP_APK_URL
- DISPLAY_APP_FORCE_UPDATE = false|true
- DISPLAY_APP_UPDATE_NOTES

Ejemplo:
DISPLAY_APP_VERSION_CODE=4
DISPLAY_APP_VERSION_NAME=1.3.0
DISPLAY_APP_APK_URL=https://.../Eira-Display-1.3.0.apk
DISPLAY_APP_FORCE_UPDATE=false

El APK debe estar en una URL HTTPS descargable directamente.

## Importante sobre Cloudflare Access
El endpoint de actualización está protegido por el mismo Access de Eira Infra.
La app conserva la sesión dentro del WebView, pero el comprobador nativo puede no compartir
esa cookie. Si Access bloquea la petición nativa, la pantalla seguirá funcionando y el
actualizador simplemente no se activará. En ese caso, la próxima revisión debe usar un
endpoint de manifiesto firmado específico para la app o un Service Token.
