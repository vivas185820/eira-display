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

public class MainActivity extends Activity {
    private static final String DISPLAY_URL = "https://infra.somoseira.com/display";
    private WebView webView;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable retry = new Runnable() {
        @Override public void run() {
            if (webView != null) webView.loadUrl(DISPLAY_URL);
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
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
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

        webView.loadUrl(DISPLAY_URL);
    }

    private void showOffline() {
        String html = "<html><body style='margin:0;background:#070b14;color:#fff;"
            + "font-family:sans-serif;display:flex;height:100vh;align-items:center;"
            + "justify-content:center;text-align:center'><div><h1>Eira Display</h1>"
            + "<p style='color:#8794aa'>Sin conexión con Eira Infra</p>"
            + "<p style='color:#7868ff'>Reintentando automáticamente…</p></div></body></html>";
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null);
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
        handler.removeCallbacksAndMessages(null);
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
