package com.shreefintech.paytouchconsumer.utill

import android.app.Activity
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.SystemClock
import android.text.InputFilter
import android.text.Spanned
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt

object Utility {


    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun hideKeyboard(activity: Activity) {
        val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        val view = activity.currentFocus ?: View(activity)

        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun hideKeyboard(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun calculatePlatformFee(amount: Double): Double {
        return when {
            amount < 1000  -> 4.0
            amount <= 5000 -> 8.0
            amount <= 40000 -> 20.0
            else            -> 30.0
        }
    }


    class EmojiExcludeFilter : InputFilter {

        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            source?.forEach {
                val type = Character.getType(it)

                if (type == Character.SURROGATE.toInt() ||
                    type == Character.OTHER_SYMBOL.toInt()
                ) {
                    return ""
                }
            }

            return null
        }
    }

    fun digitFilter() = InputFilter { source, start, end, _, _, _ ->
        val sub = source.subSequence(start, end)
        if (sub.all { it.isDigit() }) null else sub.filter { it.isDigit() }
    }

    fun alphaSpaceFilter() = InputFilter { source, start, end, _, _, _ ->
        val sub = source.subSequence(start, end)
        if (sub.all { it.isLetter() || it.isWhitespace() }) null
        else sub.filter { it.isLetter() || it.isWhitespace() }
    }


    var tapFlag = true
    var LAST_CLICK_TIME: Long = 0

    fun stopClick(): Boolean {
        if (SystemClock.elapsedRealtime() - LAST_CLICK_TIME < 800 && !tapFlag) {
            return true
        }
        LAST_CLICK_TIME = SystemClock.elapsedRealtime()
        tapFlag = false
        return false
    }

    fun View.visible() {
        this.visibility = View.VISIBLE
    }

    fun View.gone() {
        this.visibility = View.GONE
    }

    fun View.invisible() {
        this.visibility = View.INVISIBLE
    }

    @ColorInt
    fun Context.getThemeColor(@AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }


}
