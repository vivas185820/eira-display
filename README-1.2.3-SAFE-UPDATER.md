# Eira Display 1.2.3 — Safe Updater

Cambios:
- El arranque permanece igual a 1.2.2.
- NO registra DownloadManager al iniciar.
- NO comprueba actualizaciones al iniciar.
- Espera a que `/display` cargue correctamente.
- Después espera 30 segundos.
- Solo entonces comprueba si hay versión nueva.
- Cualquier fallo del actualizador queda aislado y no puede cerrar la app.
- El receiver de descarga se registra únicamente cuando el usuario pulsa Actualizar.

Importante:
La mayor parte del diseño y funciones de Eira Display viven en `/display`.
Esas mejoras se publican desde Eira Infra y aparecen en la tablet sin instalar APK.
