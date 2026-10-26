package com.scapewatch.mobile;

import android.os.Build;
import android.webkit.WebView;
import androidx.activity.OnBackPressedCallback;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {

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

    @Override
    public void onDestroy() {
        if (isFinishing()) {
            KeepAliveService.stop(this);
        }
        super.onDestroy();
    }
}
