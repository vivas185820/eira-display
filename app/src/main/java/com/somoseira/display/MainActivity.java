package com.somoseira.display;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.content.SharedPreferences;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.database.Cursor;
import android.widget.Toast;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private static final String DISPLAY_URL = "https://infra.somoseira.com/display";
    private static final String UPDATE_URL = "https://infra.somoseira.com/api/display/app/latest";
    private static final long UPDATE_CHECK_MS = 6L * 60L * 60L * 1000L;
    private long downloadId = -1L;
    private final Runnable updateLoop = new Runnable() {
        @Override public void run() {
            checkForUpdate(false);
            handler.postDelayed(this, UPDATE_CHECK_MS);
        }
    };
    private WebView webView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable retry = new Runnable() {
        @Override public void run() {
            if (webView != null) webView.loadUrl(DISPLAY_URL);
        }
    };


    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(intent.getAction())) return;
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L);
            if (id != downloadId || id == -1L) return;

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            Uri uri = dm.getUriForDownloadedFile(id);
            if (uri == null) {
                Toast.makeText(MainActivity.this, "No se pudo abrir la actualización", Toast.LENGTH_LONG).show();
                return;
            }
            launchInstaller(uri);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        immersive();

        webView = new WebView(this);
        setContentView(webView);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setBlockNetworkImage(false);
        s.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // Keep Cloudflare Access authentication and Eira pages inside the app.
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                handler.removeCallbacks(retry);
                immersive();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    showOffline();
                    handler.removeCallbacks(retry);
                    handler.postDelayed(retry, 10000);
                }
            }
        });

        String splash = "<html><body style='margin:0;background:#070b14;color:#fff;font-family:sans-serif;"
            + "display:flex;height:100vh;align-items:center;justify-content:center;text-align:center'>"
            + "<div><div style='width:60px;height:60px;margin:auto;border-radius:16px;background:#7868ff;"
            + "display:flex;align-items:center;justify-content:center;font-size:32px;font-weight:900'>E</div>"
            + "<h2 style='margin:12px 0 4px'>Eira Infra</h2><p style='color:#8794aa;margin:0'>Conectando…</p></div></body></html>";
        webView.loadDataWithBaseURL(DISPLAY_URL, splash, "text/html", "UTF-8", null);
        handler.postDelayed(() -> webView.loadUrl(DISPLAY_URL), 120);
    }

    private void showOffline() {
        String html = "<html><body style='margin:0;background:#070b14;color:#fff;"
            + "font-family:sans-serif;display:flex;height:100vh;align-items:center;"
            + "justify-content:center;text-align:center'><div><h1>Eira Display</h1>"
            + "<p style='color:#8794aa'>Sin conexión con Eira Infra</p>"
            + "<p style='color:#7868ff'>Reintentando automáticamente…</p></div></body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
    }


    private void checkForUpdate(boolean manual) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(UPDATE_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);
                conn.setRequestProperty("Accept", "application/json");
                conn.setInstanceFollowRedirects(true);

                // Cloudflare Access session is handled inside WebView; a raw native
                // request may be rejected by Access. If so, updater stays idle.
                int code = conn.getResponseCode();
                if (code != 200) return;

                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line);
                br.close();

                JSONObject j = new JSONObject(sb.toString());
                if (!j.optBoolean("configured", false)) return;

                int latest = j.optInt("version_code", 0);
                String name = j.optString("version_name", "");
                String apk = j.optString("apk_url", "");
                String notes = j.optString("notes", "");
                boolean mandatory = j.optBoolean("mandatory", false);

                int current = getPackageManager()
                    .getPackageInfo(getPackageName(), 0).versionCode;

                if (latest > current && apk.startsWith("https://")) {
                    runOnUiThread(() -> showUpdateDialog(name, apk, notes, mandatory));
                } else if (manual) {
                    runOnUiThread(() -> Toast.makeText(
                        MainActivity.this, "Eira Display está actualizado", Toast.LENGTH_SHORT
                    ).show());
                }
            } catch (Exception ignored) {
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    private void showUpdateDialog(String versionName, String apkUrl, String notes, boolean mandatory) {
        AlertDialog.Builder b = new AlertDialog.Builder(this)
            .setTitle("Actualización de Eira Display")
            .setMessage(
                "Nueva versión " + versionName + " disponible." +
                (notes == null || notes.isEmpty() ? "" : "\n\n" + notes)
            )
            .setPositiveButton("Actualizar", (dialog, which) -> downloadUpdate(apkUrl));

        if (!mandatory) {
            b.setNegativeButton("Después", null);
        }
        b.setCancelable(!mandatory);
        b.show();
    }

    private void downloadUpdate(String apkUrl) {
        try {
            DownloadManager.Request req = new DownloadManager.Request(Uri.parse(apkUrl));
            req.setTitle("Actualizando Eira Display");
            req.setDescription("Descargando nueva versión…");
            req.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            req.setMimeType("application/vnd.android.package-archive");
            req.setDestinationInExternalFilesDir(
                this,
                android.os.Environment.DIRECTORY_DOWNLOADS,
                "Eira-Display-update.apk"
            );

            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            downloadId = dm.enqueue(req);
            Toast.makeText(this, "Descargando actualización…", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "No se pudo iniciar la descarga", Toast.LENGTH_LONG).show();
        }
    }

    private void launchInstaller(Uri uri) {
        if (android.os.Build.VERSION.SDK_INT >= 26 &&
            !getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(this)
                .setTitle("Permitir actualizaciones")
                .setMessage("Android necesita permitir que Eira Display instale su propia actualización. Esto solo se configura una vez.")
                .setPositiveButton("Abrir ajuste", (d, w) -> {
                    Intent i = new Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + getPackageName())
                    );
                    startActivity(i);
                    Toast.makeText(
                        this,
                        "Activa el permiso y vuelve a Eira Display. La próxima actualización será mucho más rápida.",
                        Toast.LENGTH_LONG
                    ).show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
            return;
        }

        Intent install = new Intent(Intent.ACTION_VIEW);
        install.setDataAndType(uri, "application/vnd.android.package-archive");
        install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(install);
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    @Override
    public void onBackPressed() {
        // Dedicated monitor: never exit by accident.
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            webView.loadUrl(DISPLAY_URL);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        immersive();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(updateLoop);
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
