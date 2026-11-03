package com.appforge.socialchat;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebChromeClient;

public class MainActivity extends BridgeActivity {
    private WebView website;
    private final boolean allowHttp = false;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        if (bridge == null) return;
        website = new WebView(this);
        WebSettings settings = website.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setSupportMultipleWindows(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(website, false);
        website.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return navigate(request.getUrl());
            }
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return navigate(Uri.parse(url));
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                CookieManager.getInstance().flush();
            }
        });
        // Preserve file pickers and permission requests initiated by the website.
        website.setWebChromeClient(new BridgeWebChromeClient(bridge) {
            @Override
            public boolean onCreateWindow(WebView view, boolean dialog, boolean userGesture, Message result) {
                if (!userGesture) return false;
                final WebView popup = new WebView(MainActivity.this);
                popup.setWebViewClient(new WebViewClient() {
                    private boolean handled = false;
                    private boolean forward(Uri url) {
                        if (handled || "about".equals(url.getScheme())) return true;
                        handled = true;
                        if (!navigate(url)) website.loadUrl(url.toString());
                        popup.post(popup::destroy);
                        return true;
                    }
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                        return forward(request.getUrl());
                    }
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        return forward(Uri.parse(url));
                    }
                });
                ((WebView.WebViewTransport) result.obj).setWebView(popup);
                result.sendToTarget();
                return true;
            }
        });
        setContentView(website);
        fullscreen();
        // android_asset remains readable even with arbitrary file access disabled.
        website.loadUrl("file:///android_asset/public/index.html");
    }

    private boolean navigate(Uri url) {
        String scheme = url.getScheme();
        if ("https".equalsIgnoreCase(scheme) || (allowHttp && "http".equalsIgnoreCase(scheme))) return false;
        // Explicit phone/mail links may open their dedicated apps, never arbitrary intents.
        if ("tel".equalsIgnoreCase(scheme) || "mailto".equalsIgnoreCase(scheme) || "sms".equalsIgnoreCase(scheme)) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, url)); }
            catch (ActivityNotFoundException ignored) { }
        }
        return true;
    }

    private void fullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) fullscreen();
    }

    @Override
    public void onPause() {
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (website != null && website.canGoBack()) website.goBack();
        else super.onBackPressed();
    }

    @Override
    public void onDestroy() {
        if (website != null) website.destroy();
        super.onDestroy();
    }
}
