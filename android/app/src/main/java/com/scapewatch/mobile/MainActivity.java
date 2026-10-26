package com.scapewatch.mobile;

import android.os.Build;
import android.os.SystemClock;
import android.webkit.RenderProcessGoneDetail;
import android.webkit.WebView;
import androidx.activity.OnBackPressedCallback;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.WebViewListener;

public class MainActivity extends BridgeActivity {

    private static long lastRendererLoss;

    @Override
    protected void load() {
        OrientationPreference orientation = new OrientationPreference(this);
        setRequestedOrientation(OrientationPreference.requestedOrientation(orientation.get()));
        registerPlugin(OrientationPlugin.class);
        WebView webView = findViewById(com.getcapacitor.android.R.id.webview);
        MobileUiInjector mobileUi = new MobileUiInjector(getAssets());

        // Bridge construction loads the remote game. Install first so its initial
        // document receives the same UI bundle as every subsequent navigation.
        mobileUi.install(webView);
        bridgeBuilder.addWebViewListener(mobileUi);
        bridgeBuilder.addWebViewListener(new WebViewListener() {
            @Override
            public boolean onRenderProcessGone(WebView view, RenderProcessGoneDetail detail) {
                recoverFromRendererLoss(view);
                return true;
            }
        });
        super.load();
        tuneForLowEndDevices(webView);
        KeepAliveService.start(this);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            private boolean pending;

            @Override
            public void handleOnBackPressed() {
                if (pending) {
                    return;
                }
                if (!MobileUiInjector.isGameUrl(webView.getUrl())) {
                    fallback();
                    return;
                }
                pending = true;
                webView.evaluateJavascript(
                    "(() => { try { return typeof window.__swMobileBack === 'function' "
                        + "&& window.__swMobileBack() === true; } catch (_) { return false; } })()",
                    result -> {
                        pending = false;
                        if (!"true".equals(result)) {
                            fallback();
                        }
                    }
                );
            }

            private void fallback() {
                if (!MobileUiInjector.isGameUrl(webView.getUrl()) && webView.canGoBack()) {
                    webView.goBack();
                    return;
                }
                setEnabled(false);
                try {
                    getOnBackPressedDispatcher().onBackPressed();
                } finally {
                    setEnabled(true);
                }
            }
        });
    }

    /**
     * Lite build: keep the game's renderer process at high priority even when the
     * app is in the background, so Android's low-memory killer picks other apps
     * first and the game does not have to reload and reconnect.
     */
    private static void tuneForLowEndDevices(WebView webView) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            webView.setRendererPriorityPolicy(WebView.RENDERER_PRIORITY_IMPORTANT, false);
        }
    }

    /**
     * Lite build: on low-memory phones Android can kill the WebView's renderer
     * process while the app is in the background. The WebView is then unusable and
     * shows a black screen. Rebuild the activity (and with it a fresh WebView that
     * reloads the game) instead. If the renderer dies again right away, close the
     * app cleanly rather than loop.
     */
    private void recoverFromRendererLoss(WebView view) {
        long now = SystemClock.elapsedRealtime();
        boolean repeated = now - lastRendererLoss < 15000;
        lastRendererLoss = now;
        if (view.getParent() instanceof android.view.ViewGroup) {
            ((android.view.ViewGroup) view.getParent()).removeView(view);
        }
        view.destroy();
        if (repeated) {
            finish();
        } else {
            recreate();
        }
    }

    @Override
    public void onDestroy() {
        if (isFinishing()) {
            KeepAliveService.stop(this);
        }
        super.onDestroy();
    }
}
