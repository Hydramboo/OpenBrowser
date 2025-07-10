package rj.browser.ui.guide

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import android.widget.ProgressBar
import com.tencent.smtt.sdk.QbSdk
import rj.browser.R
import rj.browser.ui.base.BaseActivity
import rj.browser.ui.home.HomeActivity
import androidx.core.content.edit
import rj.browser.utils.ui.runOnUi

class GuideActivity : BaseActivity() {
    private lateinit var tvInstalling: TextView
    private lateinit var splashIcon: ImageView
    val progressBar: ProgressBar by lazy { findViewById<ProgressBar>(R.id.progress_bar) }
    private var isCheckingState = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_guide)
        initViews()
        if (!this.isFirstLaunch || Build.VERSION.SDK_INT > 35) {
            navigateToMainActivity()
            finish()
            }
        startCheckingQbSdkState()
        currentInstance = this
    }

    /**
     * 初始化视图
     */
    private fun initViews() {
        tvInstalling = findViewById(R.id.tv_installing)
        splashIcon = findViewById(R.id.splash_icon)
       // progressBar = findViewById(R.id.progress_bar)
        tvInstalling.text = "安装中"
    }

    private val isFirstLaunch: Boolean
        /**
         * 检查是否是第一次启动
         */
        get() {
            val prefs = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            )
            return prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        }

    /**
     * 标记已经不是第一次启动
     */
    private fun markNotFirstLaunch() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_FIRST_LAUNCH, false) }
    }

    private fun showSplashIcon() {
        splashIcon.visibility = View.VISIBLE
        tvInstalling.visibility = View.GONE
        progressBar.visibility = View.GONE // 隐藏 ProgressBar
    }

    private fun hideSplashIcon() {
        splashIcon.visibility = View.GONE
        tvInstalling.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE // 显示 ProgressBar
    }

    /**
     * 开始检查QbSdk状态
     */
    private fun startCheckingQbSdkState() {
        if (isCheckingState) {
            return
        }

        isCheckingState = true

        val checkThread = Thread {
            while (isCheckingState && !isFinishing) {
                try {
                    val isReady = checkQbSdkState()

                    if (isReady) {
                        runOnUi {
                            onQbSdkReady()
                        }
                        break
                    }

                    Thread.sleep(CHECK_INTERVAL.toLong())
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        checkThread.setDaemon(true)
        checkThread.start()
    }

    /**
     * 检查QbSdk状态
     * 注意：根据您的具体需求，这里可能需要调整检查方法
     */
    private fun checkQbSdkState(): Boolean {
        try {
            return QbSdk.getTbsVersion(this) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * QbSdk准备就绪时的回调
     */
    private fun onQbSdkReady() {
        isCheckingState = false
        tvInstalling.text = "安装成功"
        markNotFirstLaunch()
        navigateToMainActivity()
    }

    /**
     * 跳转到主Activity
     */
    private fun navigateToMainActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finish()
    }

    companion object {
        private var currentInstance: GuideActivity? = null
        private const val PREFS_NAME = "app_preferences"
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        private const val SPLASH_DELAY = 750
        private const val ANIM_DELAY = 1000
        private const val CHECK_INTERVAL = 1000
        fun setProgress(value: Int) {
            currentInstance?.progressBar?.progress = value
        }
    }
}
