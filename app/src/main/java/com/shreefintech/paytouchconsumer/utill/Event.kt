package com.shreefintech.paytouchconsumer.utill

open class Event<out T>(private val content: T) {
    private var hasBeenHandled = false

    fun getContentIfNotHandled(): T? {
        if (hasBeenHandled) return null
        hasBeenHandled = true
        return content
    }
}
