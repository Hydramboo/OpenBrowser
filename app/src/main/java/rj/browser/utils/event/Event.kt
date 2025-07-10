package rj.browser.utils.event

import kotlin.let

class Event<out T>(private val content: T) {
    var handled = false
        private set

    fun getContentIfNotHandled(): T? {
        return if (handled) null else {
            handled = true
            content
        }
    }

    inline fun handle(
        handler: (data: T) -> Unit
    ) {
        getContentIfNotHandled()?.let {
            handler(it)
        }
    }
}

fun <T> T.event() = Event(this)

fun emptyEvent() = Event(Unit)