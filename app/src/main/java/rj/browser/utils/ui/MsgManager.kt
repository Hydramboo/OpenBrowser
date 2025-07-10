package rj.browser.utils.ui

import android.widget.Toast
import rj.browser.context

object MsgManager {
    private var currentToast: Toast? = null
    fun toast(
        content: CharSequence,
        length: Int = Toast.LENGTH_SHORT
    ) {
        currentToast?.cancel()
        Toast.makeText(context, content, length).also {
            currentToast = it
        }.show()
    }
}

enum class MsgLength {
    SHORT,
    LONG
}

fun msg(
    content: CharSequence,
    length: MsgLength = MsgLength.SHORT
) {
    MsgManager.toast(
        content, when (length) {
            MsgLength.LONG -> Toast.LENGTH_LONG
            MsgLength.SHORT -> Toast.LENGTH_SHORT
        }
    )
}