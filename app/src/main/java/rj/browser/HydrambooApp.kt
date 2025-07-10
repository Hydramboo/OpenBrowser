package rj.browser

import androidx.multidex.MultiDexApplication
import android.content.Context
import android.util.Log
import android.os.Build
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.tencent.smtt.sdk.ProgressListener
import com.tencent.smtt.sdk.QbSdk
import com.tencent.smtt.sdk.TbsFramework
import com.tencent.smtt.sdk.WebView
import com.tencent.smtt.sdk.core.dynamicinstall.DynamicInstallManager
import com.tencent.smtt.utils.AppUtil
import rj.browser.utils.ui.ThemeManager
import rj.browser.utils.ui.msg
import rj.browser.utils.ui.runOnUi
import rj.browser.ui.guide.GuideActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference

var contextRef: WeakReference<Context>? = null
val context: Context
    get() = contextRef?.get() ?: throw NullPointerException("context is null")

class HydrambooApp : MultiDexApplication() {
    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        contextRef = WeakReference(this)
        ThemeManager.applyThemeOnAppStart(this)
    }
    var isTriggedToast: Boolean = false
    override fun onCreate() {
        super.onCreate()
        
        val manager = DynamicInstallManager(this)
        val appContext = applicationContext
        val internalStorage = filesDir
        val path = internalStorage.absolutePath
        val targetConfig64Impl = File("$path/config64.tbs")
        copyAssetsToSDCard(this, "tbs", "$path/")
        var targetTbsVersion = 47804
        if (!AppUtil.is64BitImpl()) {
            TbsFramework.setUp(this)
        } else {
            targetTbsVersion = 47805
            TbsFramework.setUp(this, targetConfig64Impl)
        }
        QbSdk.enableX5WithoutRestart()
        manager.registerListener(object : ProgressListener {
            override fun onProgress(i: Int) {
                Log.i(TAG, "downloading: $i")
                if (!isTriggedToast) {
                    isTriggedToast = true
                    runOnUi {
                        GuideActivity.setProgress(i)
                        msg("正在下载内核，请勿使用！")
                    }
                }
            }

            override fun onFinished() {
                Log.i(TAG, "onFinished")
                QbSdk.preInit(appContext, true, null)
                runOnUi {
                    msg("安装成功！")
                }
            }

            override fun onFailed(code: Int, msg: String?) {
                Log.i(TAG, "onError: $code; msg: $msg")
                runOnUi {
                    msg("出错了，请报告开发者，错误码：$code; 信息: $msg")
                }
            }
        })
        val yourAppNeedUpdateX5 = QbSdk.getTbsVersion(this) != targetTbsVersion
        when {
            Build.VERSION.SDK_INT > 35 -> QbSdk.preInit(this, true, null)
            manager.needUpdateLicense() || yourAppNeedUpdateX5 -> manager.startInstall()
            Build.CPU_ABI.equals("x86") || Build.CPU_ABI.equals("mips") -> QbSdk.preInit(this, true, null)
            else -> QbSdk.preInit(this, true, null)
        }
        /* if (Build.VERSION.SDK_INT > 35) {
            QbSdk.preInit(this, true, null)
        } else if (manager.needUpdateLicense() || yourAppNeedUpdateX5) {
            manager.startInstall()
        } else {
            QbSdk.preInit(this, true, null)
        } */
        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        // 设置深色模式（初始化时）
        applyDarkMode(sharedPreferences)

        // 注册监听器，监听用户设置的变化
        sharedPreferences.registerOnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                "dark_mode" -> applyDarkMode(prefs) // 如果深色模式开关发生变化
                "ccache" -> handleCacheClear(prefs) // 如果清理缓存设置发生变化
            }
        }
    }

    /**
     * 根据用户选择设置深色模式
     */
    private fun applyDarkMode(sharedPreferences: SharedPreferences) {
        when (sharedPreferences.getString("dark_mode", "follow_system")) {
            "follow_system" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            "dark" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    /**
     * 清理缓存的实现逻辑
     */
    private fun handleCacheClear(sharedPreferences: SharedPreferences) {
        val isAutoClearCacheEnabled = sharedPreferences.getBoolean("ccache", false)
        if (isAutoClearCacheEnabled) {
            clearCache() // 执行清理缓存操作
        }
    }

    /**
     * 清理应用缓存
     */
    private fun clearCache() {
        var webView = WebView(this)
        webView.clearCache(true)
        webView.destroy()
        try {
            val cacheDir = cacheDir
            if (cacheDir != null && cacheDir.isDirectory) {
                cacheDir.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val TAG = "HydrambooApp"
        fun copyAssetsToSDCard(context: Context, sourceFolder: String, destinationFolder: String) {
            val assetManager = context.assets
            val files: Array<String>?
            try {
                files = assetManager.list(sourceFolder)
            } catch (e: IOException) {
                e.printStackTrace()
                return
            }

            val destFolder = File(destinationFolder)
            if (!destFolder.exists()) {
                destFolder.mkdirs()
            }

            for (filename in files!!) {
                var `in`: InputStream? = null
                var out: OutputStream? = null
                try {
                    `in` = assetManager.open("$sourceFolder/$filename")
                    val outFile = File(destinationFolder, filename)
                    out = FileOutputStream(outFile)
                    Log.i("Qbsdk", "copy 开始")
                    copyFile(`in`, out)
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    try {
                        `in`?.close()
                        out?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        private fun copyFile(`in`: InputStream, out: OutputStream) {
            try {
                val buffer = ByteArray(1024)
                var read: Int
                Log.i("Qbsdk", "copy 进行中...")
                while ((`in`.read(buffer).also { read = it }) != -1) {
                    out.write(buffer, 0, read)
                }
                Log.i("Qbsdk", "copy 文件成功")
            } catch (e: Exception) {
                Log.i("Qbsdk", "copy 文件失败" + e.message)
            }
        }
    }
}
