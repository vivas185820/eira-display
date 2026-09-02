# Eira Display — Compilar APK con GitHub Actions

No necesitas Android Studio.

## Pasos

1. Crea un repositorio nuevo en GitHub, por ejemplo:
   `eira-display`

2. Descomprime este ZIP.

3. Sube TODO el contenido de la carpeta al repositorio.
   Es importante incluir la carpeta oculta:
   `.github/workflows/`

4. En GitHub entra a:
   **Actions**

5. Abre:
   **Build Eira Display APK**

6. Pulsa:
   **Run workflow**

7. Espera a que termine en verde.

8. Abre la ejecución terminada y baja hasta:
   **Artifacts**

9. Descarga:
   **Eira-Display-APK**

10. Dentro del ZIP descargado estará:
    `app-debug.apk`

11. Pasa `app-debug.apk` a la tablet e instálalo.

## Después de instalar

- Abre Eira Display.
- Inicia sesión en Cloudflare Access si te lo pide.
- Si la tablet será exclusiva para Eira:
  Ajustes → Aplicaciones predeterminadas → Aplicación de inicio → Eira Display

Al reiniciar la tablet, Eira Display podrá actuar como pantalla principal.

## URL configurada

`https://infra.somoseira.com/display`
