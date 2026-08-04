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
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

object Utility {

    val STATE_LIST = listOf(
        "01" to "Andhra Pradesh",
        "02" to "Assam",
        "03" to "Bihar & Jharkhand",
        "04" to "Chennai",
        "05" to "Delhi & NCR",
        "06" to "Gujarat",
        "07" to "Haryana",
        "08" to "Himachal Pradesh",
        "09" to "Jammu & Kashmir",
        "10" to "Karnataka",
        "11" to "Kerala",
        "12" to "Kolkata",
        "13" to "Maharashtra & Goa (except Mumbai)",
        "14" to "MP & Chattisgarh",
        "15" to "Mumbai",
        "16" to "North East",
        "17" to "Orissa",
        "18" to "Punjab",
        "19" to "Rajasthan",
        "20" to "Tamilnadu",
        "21" to "UP(EAST)",
        "22" to "UP(WEST) & Uttarakhand",
        "23" to "West Bengal",
        "51" to "All India (except Delhi/Mumbai)"
    )

    fun formatDate(createdAt: String?, format: String = "dd/MM/yyyy hh:mm a"): String {
        if (createdAt.isNullOrBlank()) return "--"
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val output = SimpleDateFormat(format, Locale.getDefault())
            val date = input.parse(createdAt.substringBefore(".").substringBefore("+"))
            if (date != null) output.format(date) else createdAt
        } catch (e: Exception) {
            createdAt
        }
    }

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

    fun formatAmount(raw: String?): String {
        if (raw.isNullOrBlank()) return "-"
        return try {
            val number = raw.toDouble()
            val fmt = NumberFormat.getNumberInstance(Locale("en", "IN")).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 2
            }
            "₹${fmt.format(number)}"
        } catch (e: Exception) {
            "₹$raw"
        }
    }

    fun formatAmount(raw: Double?): String = formatAmount(raw?.toString())

    fun maskNumber(number: String): String {
        if (number.length < 5) return number
        return "${number.take(4)}*****${number.takeLast(1)}"
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
