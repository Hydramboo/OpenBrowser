package rj.browser.ui.browser

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.JavascriptInterface
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.net.toUri
import com.google.zxing.integration.android.IntentIntegrator
import com.tencent.smtt.export.external.interfaces.GeolocationPermissionsCallback
import com.tencent.smtt.export.external.interfaces.JsPromptResult
import com.tencent.smtt.export.external.interfaces.JsResult
import com.tencent.smtt.export.external.interfaces.WebResourceRequest
import com.tencent.smtt.sdk.ValueCallback
import com.tencent.smtt.sdk.WebChromeClient
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.WebViewClient
import rj.browser.R
import rj.browser.ui.base.BaseActivity
import rj.browser.utils.AssetsReader
import rj.browser.utils.PermissionUtil
import rj.browser.utils.WebViewJavaScriptFunction

open class WebViewActivity : BaseActivity() {
    private lateinit var backBtn: ImageButton
    private lateinit var forwardBtn: ImageButton
    protected lateinit var webView: WebView
    private val mHomeUrl = "file:///android_asset/webpage/homePage.html"
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mGeolocationCallback: GeolocationPermissionsCallback? = null
    private var locationPermissionUrl: String? = null
    private lateinit var mUrlEditText: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        initWebView()
        initButtons()
        onBackPressedDispatcher.addCallback {
            if (webView.canGoBack()) {
                webView.goBack()
                updateGoForwardButton(webView)
            } else {
                finish()
            }
        }
    }

    protected open fun initWebView() {
        val context = this
        webView = WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        val mContainer = findViewById<ViewGroup>(R.id.webViewContainer)
        mContainer.addView(webView)

        val webSetting = webView.settings
        webSetting.setUserAgentString("${webSetting.userAgentString} Hydramboo/20250531")
        webSetting.setJavaScriptEnabled(true)
        webSetting.setAllowFileAccess(true)
        webSetting.setSupportZoom(true)
        webSetting.setDatabaseEnabled(true)
        webSetting.setAllowFileAccess(true)
        webSetting.setDomStorageEnabled(true)

        initWebViewClient()
        initWebChromeClient()
        initJavaScriptInterface()

        webView.loadUrl(mHomeUrl)
    }

    private fun initWebViewClient() {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                return handleUrl(request.url.toString()) || super.shouldOverrideUrlLoading(view, request)
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                return handleUrl(url) || super.shouldOverrideUrlLoading(view, url)
            }

            private fun handleUrl(url: String): Boolean {
                if (url.isEmpty()) return false
                val uri = url.toUri()
                return when (uri.scheme?.lowercase()) {
                    "http", "https", "file" -> false
                    else -> {
                        try {
                            val intent = if (url.startsWith("intent://")) {
                                Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                            } else {
                                Intent(Intent.ACTION_VIEW, uri)
                            }
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                                true
                            } else {
                                Toast.makeText(
                                    this@WebViewActivity,
                                    "未找到支持该链接的应用",
                                    Toast.LENGTH_SHORT
                                ).show()
                                false
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                this@WebViewActivity,
                                "跳转失败",
                                Toast.LENGTH_SHORT
                            ).show()
                            false
                        }
                    }
                }
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                Log.i(TAG, "onPageStarted, url:$url")
                mUrlEditText.setText(url.orEmpty())
            }

            override fun onPageFinished(view: WebView, url: String) {
                Log.i(TAG, "onPageFinished, url:$url")
                val jsContent = AssetsReader.readAssetFile(this@WebViewActivity, "dark.js")
                if (jsContent != null) {
                    // 使用 evaluateJavascript 在主线程安全地执行 JavaScript
                    view.evaluateJavascript(jsContent, null)
                } else {
                    // 处理读取文件失败的情况
                    Log.w(TAG, "Failed to read dark.js from assets.")
                }
                updateGoForwardButton(view)
            }

            override fun onReceivedError(
                view: WebView,
                errorCode: Int,
                description: String,
                failingUrl: String
            ) {
                Log.e(TAG, "onReceivedError: $errorCode, $description, $failingUrl")
            }


        }
    }

    private fun initWebChromeClient() {
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                Log.i(TAG, "onProgressChanged, newProgress:$newProgress")
                updateGoForwardButton(view)
            }

            override fun onJsAlert(
                view: WebView,
                url: String,
                message: String,
                result: JsResult
            ): Boolean {
                AlertDialog.Builder(this@WebViewActivity)
                    .setTitle("JS弹窗")
                    .setMessage(message)
                    .setPositiveButton("确定") { _, _ -> result.confirm() }
                    .setCancelable(false)
                    .show()
                return true
            }

            override fun onJsConfirm(
                view: WebView,
                url: String,
                message: String,
                result: JsResult
            ): Boolean {
                AlertDialog.Builder(this@WebViewActivity)
                    .setTitle("JS弹窗")
                    .setMessage(message)
                    .setPositiveButton("确定") { _, _ -> result.confirm() }
                    .setNegativeButton("取消") { _, _ -> result.cancel() }
                    .setCancelable(false)
                    .show()
                return true
            }

            override fun onJsBeforeUnload(
                view: WebView,
                url: String,
                message: String,
                result: JsResult
            ): Boolean {
                AlertDialog.Builder(this@WebViewActivity)
                    .setTitle("页面即将跳转")
                    .setMessage(message)
                    .setPositiveButton("确定") { _, _ -> result.confirm() }
                    .setNegativeButton("取消") { _, _ -> result.cancel() }
                    .setCancelable(false)
                    .show()
                return true
            }

            override fun onJsPrompt(
                view: WebView,
                url: String,
                message: String,
                defaultValue: String,
                result: JsPromptResult
            ): Boolean {
                val input = EditText(this@WebViewActivity).apply {
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                }
                AlertDialog.Builder(this@WebViewActivity)
                    .setTitle("JS弹窗")
                    .setMessage(message)
                    .setView(input)
                    .setPositiveButton("确定") { _, _ -> result.confirm(input.text.toString()) }
                    .setCancelable(false)
                    .show()
                return true
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                Log.i(TAG, "openFileChooser: ${fileChooserParams.mode}")
                mFilePathCallback = filePathCallback
                openFileChooseProcess(fileChooserParams.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
                return true
            }

            override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissionsCallback
            ) {
                if (PermissionUtil.verifyLocationPermissions(this@WebViewActivity)) {
                    callback.invoke(origin, true, false)
                } else {
                    locationPermissionUrl = origin
                    mGeolocationCallback = callback
                }
            }
        }
    }

    private fun initJavaScriptInterface() {
        webView.addJavascriptInterface(object : WebViewJavaScriptFunction {
            @JavascriptInterface
            override fun onJsFunctionCalled(tag: String) {}

            @JavascriptInterface
            fun openQRCodeScan() {
                IntentIntegrator(this@WebViewActivity).initiateScan()
            }

            @JavascriptInterface
            fun openDebugX5() {
                webView.loadUrl("http://debugx5.qq.com")
            }
        }, "Android")
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PermissionUtil.REQUEST_EXTERNAL_STORAGE -> initWebView()
            PermissionUtil.REQUEST_GEOLOCATION -> {
                mGeolocationCallback?.let { callback ->
                    val allow = grantResults[0] == PackageManager.PERMISSION_GRANTED
                    callback.invoke(locationPermissionUrl!!, allow, false)
                    mGeolocationCallback = null
                    locationPermissionUrl = null
                }
            }
        }
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            FILE_CHOOSER_REQUEST_CODE -> {
                if (resultCode == RESULT_OK) {
                    mFilePathCallback?.let { callback ->
                        val uris = data?.clipData?.let {
                            val count = it.itemCount ?: 0
                            Array(count) { index ->
                                it.getItemAt(index).uri
                            }
                        } ?: arrayOf(data?.data ?: Uri.EMPTY)
                        callback.onReceiveValue(uris)
                        mFilePathCallback = null
                    }
                }
            }
            else -> {
                val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
                result?.let {
                    if (it.contents != null) {
                        webView.loadUrl(it.contents)
                    } else {
                        Toast.makeText(this, "扫描结果似乎为空", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun openFileChooseProcess(isMulti: Boolean) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            type = "*/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, isMulti)
        }
        startActivityForResult(Intent.createChooser(intent, "FileChooser"), FILE_CHOOSER_REQUEST_CODE)
    }

    private fun showPopupMenu(view: View) {
        PopupMenu(this, view).run {
            menuInflater.inflate(R.menu.website_menu, menu)
            setOnMenuItemClickListener { item ->
                val url = mHomeUrl
                url.takeIf { it.isNotEmpty() }?.let { webView.loadUrl(it) }
                true
            }
            show()
        }
    }

    private fun initButtons() {
        backBtn = findViewById(R.id.btn_back)
        backBtn.imageAlpha = DISABLED_IMAGE_ALPHA
        backBtn.isEnabled = false
        backBtn.setOnClickListener { webView.goBack() }

        forwardBtn = findViewById(R.id.btn_forward)
        forwardBtn.imageAlpha = DISABLED_IMAGE_ALPHA
        forwardBtn.isEnabled = false
        forwardBtn.setOnClickListener { webView.goForward() }

        findViewById<View>(R.id.btn_more).setOnClickListener { showPopupMenu(it) }
        findViewById<View>(R.id.btn_reload).setOnClickListener { webView.reload() }
        findViewById<View>(R.id.btn_exit).setOnClickListener { finish() }

        mUrlEditText = findViewById<EditText>(R.id.urlEdit).apply {
            setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    val url = text.toString()
                    loadUrlWithFallback(url)
                    text.clear()
                    true
                }
                false
            }
            setOnFocusChangeListener { v, hasFocus ->
                if (!hasFocus) {
                    (getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                        .hideSoftInputFromWindow(v.windowToken, 0)
                }
            }
        }

        findViewById<View>(R.id.urlLoad).setOnClickListener {
            val url = mUrlEditText.text.toString()
            loadUrlWithFallback(url)
            mUrlEditText.text.clear()
        }
    }

    private fun loadUrlWithFallback(url: String) {
        val uri = when {
            url.isBlank() -> mHomeUrl
            !url.contains(".") && !url.contains("://") -> "https://cn.bing.com/?q=$url"
            !url.contains("://") && !url.contains("file:") && !url.startsWith("javascript:") -> "https://$url"
            else -> url
        }
        webView.loadUrl(uri)
    }

    private fun updateGoForwardButton(view: WebView) {
        val canGoBack = view.canGoBack()
        backBtn.imageAlpha = if (canGoBack) ENABLED_IMAGE_ALPHA else DISABLED_IMAGE_ALPHA
        backBtn.isEnabled = canGoBack

        val canGoForward = view.canGoForward()
        forwardBtn.imageAlpha = if (canGoForward) ENABLED_IMAGE_ALPHA else DISABLED_IMAGE_ALPHA
        forwardBtn.isEnabled = canGoForward
    }

    companion object {
        const val TAG = "WebViewActivity"
        const val FILE_CHOOSER_REQUEST_CODE = 100

        const val DISABLED_IMAGE_ALPHA = 120
        const val ENABLED_IMAGE_ALPHA = 255
    }
}