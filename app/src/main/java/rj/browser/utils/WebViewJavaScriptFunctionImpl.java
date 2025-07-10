package rj.browser.utils;
import android.content.Context;
import android.webkit.JavascriptInterface;

public class WebViewJavaScriptFunctionImpl implements WebViewJavaScriptFunction {
    private Context context;

    public WebViewJavaScriptFunctionImpl(Context context) {
        this.context = context;
    }

    @JavascriptInterface
    @Override
    public void onJsFunctionCalled(String tag) {
        // Implement your logic here, e.g. log or trigger something in the Activity
    }
}