package rj.browser.ui.setting

import android.os.Bundle
import com.highcapable.hikage.extension.setContentView
import com.highcapable.hikage.widget.androidx.fragment.app.FragmentContainerView
import rj.browser.ui.base.BaseActivity

class SettingsActivity : BaseActivity() {
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
            .replace(hikage.getActualViewId("container"), SettingsFragment())
            .commit()
    }
}
