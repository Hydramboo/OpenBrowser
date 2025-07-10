package rj.browser.utils.ui
// ThemeManager.kt
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    // 这个方法保持不变，用于应用启动时
    fun applyThemeOnAppStart(context: Context) {
        // PreferenceFragmentCompat 会使用默认的 SharedPreferences
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        // 从 Preference 的 key 读取保存的值，注意值是字符串
        val themeValue = prefs.getString("theme_preference", "follow_system") ?: "follow_system"
        applyTheme(themeValue)
    }

    /**
     * 根据 Preference 中存储的字符串值应用主题。
     * @param themeValue 对应 AppCompatDelegate 模式的字符串形式。
     */
    fun applyTheme(themeValue: String) {
        val mode = when (themeValue) {
            "light" -> AppCompatDelegate.MODE_NIGHT_NO
            "dark" -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }
}