package rj.browser.ui.setting
// SettingsFragment.kt
import android.os.Bundle
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import rj.browser.utils.ui.ThemeManager
import rj.browser.R

class SettingsFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)

        // 1. 找到我们的 ListPreference
        val themePreference: ListPreference? = findPreference("dark_mode")

        // 2. 为它设置 OnPreferenceChangeListener
        themePreference?.setOnPreferenceChangeListener { preference, newValue ->
            // newValue 是用户刚刚选择的值，例如 "1", "2", 或 "-1"

            if (newValue is String) {
                // 3. 调用 ThemeManager 来应用新主题
                ThemeManager.applyTheme(newValue)

                // 4. 重建 Activity 来让主题生效
                // activity 是 Fragment 的一个属性，它引用了宿主 Activity
                activity?.recreate()
            }

            // 5. 返回 true，表示我们接受这个新值，
            // Preference 框架会继续将其保存到 SharedPreferences。
            true
        }
    }
}
