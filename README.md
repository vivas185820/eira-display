# Eira Display 1.0

Pantalla dedicada para `https://infra.somoseira.com/display`.

## Comportamiento
- Horizontal.
- Pantalla completa / modo inmersivo.
- Mantiene la pantalla encendida.
- WebView interno.
- Cookies persistentes para Cloudflare Access.
- Reintenta cada 10 segundos si pierde conexión.
- Puede elegirse como Launcher/Home de una tablet dedicada.
- Receiver de arranque incluido.

## Primer arranque
1. Instala la app.
2. Ábrela.
3. Inicia sesión una sola vez en Cloudflare Access si aparece.
4. En una tablet dedicada: Ajustes > Apps predeterminadas > Aplicación de inicio > Eira Display.
5. Reinicia la tablet y debe entrar directamente en Eira Display.

Android moderno puede bloquear que una app normal abra una ventana por sí sola después de BOOT_COMPLETED.
Por eso el método robusto incluido es usar Eira Display como Home/Launcher de la tablet.

## Compilar
Abre esta carpeta con Android Studio.
Build > Build APK(s).

La URL está en:
`MainActivity.java`
`DISPLAY_URL = "https://infra.somoseira.com/display"`
