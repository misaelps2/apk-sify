package cloud.sifysistemas.pdv;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.net.URI;
import java.net.URISyntaxException;

public final class MainActivity extends Activity {
    private static final URI SITE = URI.create("https://pdv.sifysistemas.com/");
    private static final int PICK_FILE = 1;

    private FrameLayout root;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private LinearLayout errorPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        root = new FrameLayout(this);
        root.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(),
                    insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return WindowInsets.CONSUMED;
        });
        setContentView(root);

        openSite(savedInstanceState);
    }

    private static URI parseHttps(String text) {
        try {
            URI uri = new URI(text);
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && uri.getRawUserInfo() == null && uri.getPort() <= 65535
                    && uri.getPort() != 0) {
                return uri;
            }
        } catch (URISyntaxException ignored) {
            return null;
        }
        return null;
    }

    private static int port(URI uri) {
        return uri.getPort() == -1 ? 443 : uri.getPort();
    }

    private boolean sameOrigin(Uri url) {
        URI candidate = parseHttps(url.toString());
        return candidate != null && SITE.getHost().equalsIgnoreCase(candidate.getHost())
                && port(SITE) == port(candidate);
    }

    private void openSite(Bundle savedInstanceState) {
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame() || sameOrigin(request.getUrl())) {
                    return false;
                }
                URI external = parseHttps(request.getUrl().toString());
                if (request.hasGesture() && external != null) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, request.getUrl()));
                    } catch (ActivityNotFoundException ignored) {
                        Toast.makeText(MainActivity.this, "Nenhum navegador disponível.",
                                Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showError("O servidor redirecionou para um endereço fora do PDV.");
                }
                return true;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request,
                                        WebResourceError error) {
                if (request.isForMainFrame()) {
                    showError("Não foi possível abrir o PDV. Verifique a conexão e tente novamente.");
                }
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                            WebResourceResponse response) {
                if (request.isForMainFrame() && response.getStatusCode() >= 400) {
                    showError("O PDV respondeu com HTTP " + response.getStatusCode() + ".");
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (view.getUrl() == null || !sameOrigin(Uri.parse(view.getUrl()))) {
                    callback.onReceiveValue(null);
                    return true;
                }
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), PICK_FILE);
                } catch (ActivityNotFoundException ignored) {
                    fileCallback = null;
                    callback.onReceiveValue(null);
                }
                return true;
            }
        });

        Bundle webState = savedInstanceState == null ? null : savedInstanceState.getBundle("web");
        if (webState == null || webView.restoreState(webState) == null) {
            webView.loadUrl(SITE.toASCIIString());
        }
    }

    private void showError(String message) {
        if (errorPanel != null) {
            root.removeView(errorPanel);
        }
        errorPanel = new LinearLayout(this);
        errorPanel.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        errorPanel.setPadding(padding, padding, padding, padding);
        errorPanel.setBackgroundColor(0xFFF8FAFB);
        TextView text = new TextView(this);
        text.setText(message);
        Button retry = new Button(this);
        retry.setText("Tentar novamente");
        retry.setOnClickListener(button -> {
            root.removeView(errorPanel);
            errorPanel = null;
            webView.loadUrl(SITE.toASCIIString());
        });
        errorPanel.addView(text);
        errorPanel.addView(retry);
        root.addView(errorPanel);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && fileCallback != null) {
            ValueCallback<Uri[]> callback = fileCallback;
            fileCallback = null;
            Uri[] selected = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
            if (selected != null) {
                for (Uri uri : selected) {
                    if (!"content".equals(uri.getScheme())) {
                        selected = null;
                        break;
                    }
                }
            }
            callback.onReceiveValue(selected);
        }
    }

    @Override
    public void onBackPressed() {
        if (errorPanel == null && webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) {
            Bundle webState = new Bundle();
            webView.saveState(webState);
            outState.putBundle("web", webState);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (webView != null) {
            CookieManager.getInstance().flush();
        }
    }

    @Override
    protected void onDestroy() {
        if (fileCallback != null) {
            fileCallback.onReceiveValue(null);
            fileCallback = null;
        }
        if (webView != null) {
            root.removeView(webView);
            webView.destroy();
        }
        super.onDestroy();
    }
}
