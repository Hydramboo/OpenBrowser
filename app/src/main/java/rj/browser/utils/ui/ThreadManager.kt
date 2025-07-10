package rj.browser.utils.ui

import android.os.Handler
import android.os.Looper
import java.util.concurrent.TimeUnit

object ThreadManager {
    private val MAIN_THREAD_HANDLER = Handler(Looper.getMainLooper())

    @JvmStatic
    fun runOnUiThread(runnable: Runnable) {
        MAIN_THREAD_HANDLER.post(runnable)
    }

    @JvmStatic
    fun runOnUIThreadAfter(time: Long, unit: TimeUnit?, runnable: Runnable) {
        val millis = TimeUnit.MILLISECONDS.convert(time, unit)
        MAIN_THREAD_HANDLER.postDelayed(runnable, millis)
    }

    @JvmStatic
    fun runOnUIThreadAfter(time: Long, runnable: Runnable) {
        MAIN_THREAD_HANDLER.postDelayed(runnable, time)
    }
}

fun runOnUi(task: () -> Unit) =
    ThreadManager.runOnUiThread(task)