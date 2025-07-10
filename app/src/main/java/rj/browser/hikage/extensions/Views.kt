package rj.browser.hikage.extensions

import android.view.View
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.core.view.MarginLayoutParamsCompat
import androidx.core.view.ViewCompat

var ViewGroup.MarginLayoutParams.marginStartCompat: Int
    get() = MarginLayoutParamsCompat.getMarginStart(this)
    set(value) {
        MarginLayoutParamsCompat.setMarginStart(this, value)
    }
var ViewGroup.MarginLayoutParams.marginEndCompat: Int
    get() = MarginLayoutParamsCompat.getMarginEnd(this)
    set(value) {
        MarginLayoutParamsCompat.setMarginEnd(this, value)
    }

fun ViewGroup.MarginLayoutParams.updateMarginsRelativeCompat(
    @Px start: Int = marginStartCompat,
    @Px top: Int = topMargin,
    @Px end: Int = marginEndCompat,
    @Px bottom: Int = bottomMargin
) {
    marginStartCompat = start
    topMargin = top
    marginEndCompat = end
    bottomMargin = bottom
}

val View.paddingStartCompat: Int
    get() = ViewCompat.getPaddingStart(this)
val View.paddingEndCompat: Int get() = ViewCompat.getPaddingEnd(this)

@Suppress("NOTHING_TO_INLINE")
inline fun View.updatePaddingRelativeCompat(
    @Px start: Int = paddingStartCompat,
    @Px top: Int = paddingTop,
    @Px end: Int = paddingEndCompat,
    @Px bottom: Int = paddingBottom
) {
    ViewCompat.setPaddingRelative(this, start, top, end, bottom)
}