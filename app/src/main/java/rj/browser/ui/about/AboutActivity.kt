package rj.browser.ui.about

import android.os.Bundle
import android.view.Gravity
import androidx.activity.viewModels
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.core.view.updateMargins
import androidx.transition.TransitionManager
import com.google.android.material.progressindicator.CircularProgressIndicatorSpec
import com.google.android.material.progressindicator.IndeterminateDrawable
import com.highcapable.betterandroid.ui.extension.view.updateMargins
import com.highcapable.betterandroid.ui.extension.view.updatePadding
import com.highcapable.hikage.core.runtime.collectAsHikageState
import com.highcapable.hikage.extension.setContentView
import com.highcapable.hikage.extension.widget.boldTypeFace
import com.highcapable.hikage.extension.widget.editModeText
import com.highcapable.hikage.extension.widget.endToParent
import com.highcapable.hikage.extension.widget.onClick
import com.highcapable.hikage.extension.widget.startToParent
import com.highcapable.hikage.extension.widget.textRes
import com.highcapable.hikage.extension.widget.topToParent
import com.highcapable.hikage.widget.android.widget.FrameLayout
import com.highcapable.hikage.widget.android.widget.ImageView
import com.highcapable.hikage.widget.android.widget.ScrollView
import com.highcapable.hikage.widget.android.widget.TextView
import com.highcapable.hikage.widget.androidx.constraintlayout.widget.ConstraintLayout
import com.highcapable.hikage.widget.com.google.android.material.button.MaterialButton
import com.highcapable.hikage.widget.com.google.android.material.card.MaterialCardView
import com.highcapable.hikage.widget.com.google.android.material.chip.Chip
import com.highcapable.hikage.widget.com.google.android.material.divider.MaterialDivider
import rj.browser.R
import rj.browser.hikage.extensions.updatePaddingRelativeCompat
import rj.browser.ui.base.BaseActivity


class AboutActivity: BaseActivity() {

    val viewModel: AboutActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView {
            val uiState = viewModel.uiState.collectAsHikageState(this@AboutActivity)
            ScrollView(
                widthMatchParent {
                    gravity = Gravity.CENTER
                },
                init = {
                    uiState.observe {
                        TransitionManager.beginDelayedTransition(this)
                    }
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
                                updateMargins(top = 20.dp)
                            }
                        ) {
                            setImageResource(R.mipmap.x5_logo)
                        }
                        TextView(
                            id = "app_name_text_view",
                            lparams = LayoutParams {
                                startToParent()
                                endToParent()
                                topToBottom = viewId("icon")
                                updateMargins(top = 4.dp)
                            }
                        ) {
                            boldTypeFace()
                            textSize = 16f
                            textRes = R.string.app_name
                        }
                        Chip(
                            id = "version_name_chip",
                            lparams = LayoutParams {
                                topToBottom = viewId("app_name_text_view")
                                startToParent()
                                endToParent()
                            }
                        ) {
                            text = getString(
                                R.string.version_name_format,
                                packageManager.getPackageInfo(packageName, 0).versionName
                            )
                            editModeText = "v1.0.0"
                        }
                        MaterialDivider(
                            id = "divider",
                            lparams = widthMatchParent {
                                topToBottom = viewId("version_name_chip")
                                updateMargins(vertical = 3.dp)
                            }
                        ) {
                            isInvisible = true
                            dividerInsetStart = 16.dp
                            dividerInsetEnd = 16.dp
                        }
                        MaterialButton(
                            attr = R.layout.style_view_material_button_outlined,
                            id = "check_update",
                            lparams = LayoutParams {
                                topToBottom = viewId("divider")
                                startToParent()
                                endToParent()
                                updateMargins(top = 3.dp)
                            }
                        ) {
                            val spec =
                                CircularProgressIndicatorSpec(
                                    context,
                                    null,
                                    0,
                                    com.google.android.material.R.style.Widget_Material3_CircularProgressIndicator_ExtraSmall
                                )
                            val progressIndicatorDrawable =
                                IndeterminateDrawable.createCircularDrawable(context, spec)
                            textRes = R.string.check_update
                            uiState.observe {
                                icon = if (it.checkVersionState == CheckVersionState.Loading) progressIndicatorDrawable else null
                            }
                            onClick {
                                viewModel.checkForUpdate()
                            }
                        }
                        TextView(
                            lparams = LayoutParams {
                                topToBottom = viewId("check_update")
                                startToParent()
                                endToParent()
                            }
                        ) {
                            uiState.observe {
                                isVisible = it.checkVersionState is CheckVersionState.Error
                                text = (it.checkVersionState as? CheckVersionState.Error)?.error
                            }
                        }
                        MaterialCardView(
                            attr = R.layout.style_view_material_card_outlined, 
                            lparams = widthMatchParent {
                                topToBottom = viewId("divider")
                                startToParent()
                                updateMargins(horizontal = 6.dp)
                            }, 
                            init = {
                                setContentPadding(8.dp, 8.dp, 8.dp, 8.dp)
                                uiState.observe {
                                    isVisible = it.checkVersionState is CheckVersionState.Success
                                }
                            }
                        ) {
                            TextView {
                                uiState.observe { 
                                    text = context.getString(
                                        R.string.server_version,
                                        (it.checkVersionState as? CheckVersionState.Success)?.serverVersion
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}