package rj.browser.ui.splash

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import androidx.transition.TransitionManager
import com.highcapable.betterandroid.ui.extension.view.updateMargins
import com.highcapable.betterandroid.ui.extension.view.updatePadding
import com.highcapable.hikage.extension.widget.endToParent
import com.highcapable.hikage.extension.widget.startToParent
import com.highcapable.hikage.extension.widget.topToParent
import com.highcapable.hikage.extension.setContentView
import com.highcapable.hikage.widget.android.widget.FrameLayout
import com.highcapable.hikage.widget.android.widget.ImageView
import com.highcapable.hikage.widget.android.widget.ScrollView
import com.highcapable.hikage.widget.androidx.constraintlayout.widget.ConstraintLayout
import rj.browser.R
import rj.browser.hikage.extensions.updatePaddingRelativeCompat
import rj.browser.ui.base.BaseActivity
import rj.browser.ui.guide.GuideActivity

class SplashActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView {
            ScrollView(
                widthMatchParent {
                    gravity = Gravity.CENTER
                },
                init = {
                    
                }
            ) {
                FrameLayout(widthMatchParent()) {
                    ConstraintLayout(
                        lparams = widthMatchParent(),
                        init = {
                            updatePadding(horizontal = 6.dp)
                            updatePaddingRelativeCompat(bottom = 16.dp)
                        }
                    ) {
                        ImageView(
                            id = "icon",
                            lparams = LayoutParams(64.dp, 64.dp) {
                                topToParent()
                                startToParent()
                                endToParent()
                                //updateMargins(marginTop = 20.dp)
                            }
                        ) {
                            setImageResource(R.mipmap.x5_logo)
                        }
                    }
                }
            }
        }
        // 延迟跳转
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, GuideActivity::class.java)
            startActivity(intent)
            finish()
        }, 1000)
    }
}