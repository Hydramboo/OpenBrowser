package rj.browser.ui.home

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.highcapable.hikage.extension.setContentView
import com.highcapable.hikage.widget.androidx.fragment.app.FragmentContainerView
import rj.browser.R
import rj.browser.ui.base.BaseActivity
import rj.browser.ui.setting.SettingsFragment

class HomeActivity: BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        theme.applyStyle(
            /* resId = */ com.huanli233.materialpreferences.R.style.ThemeOverlay_Material3_Preference,
            /* force = */ true
        )
        val hikage = setContentView {
            FragmentContainerView(
                lparams = matchParent(),
                id = "container"
            )
        }
        supportFragmentManager
            .beginTransaction()
            .replace(hikage.getActualViewId("container"), HomeFragment())
            .commit()
    }
}

class HomeFragment: PreferenceFragmentCompat() {
    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?
    ) {
        setPreferencesFromResource(
            /* preferencesResId = */ R.xml.home,
            /* key = */ rootKey
        )
    }

}