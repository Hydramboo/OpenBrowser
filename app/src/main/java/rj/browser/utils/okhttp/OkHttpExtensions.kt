package rj.browser.utils.okhttp

import okhttp3.OkHttpClient
import okhttp3.Request

inline fun OkHttpClient.call(
    builder: Request.Builder.() -> Unit
) = newCall(Request.Builder().apply(builder).build())