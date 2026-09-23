package com.shreefintech.paytouchconsumer.utill

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.SystemClock
import android.text.InputFilter
import android.text.Spanned
import android.util.TypedValue
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.widget.NestedScrollView
import com.shreefintech.paytouchconsumer.R
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.core.graphics.createBitmap

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
        val cleaned = createdAt.substringBefore(".").substringBefore("+")
        val inputFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "dd/MM/yyyy hh:mm a",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy",
            "yyyy-MM-dd"
        )
        val output = SimpleDateFormat(format, Locale.getDefault())
        for (pattern in inputFormats) {
            try {
                val date = SimpleDateFormat(pattern, Locale.getDefault()).parse(cleaned)
                if (date != null) return output.format(date)
            } catch (_: Exception) {
            }
        }
        return createdAt
    }

    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun applyImeOverlapPadding(view: View, imeBottom: Int, activity: Activity) {
        val loc = IntArray(2)
        view.getLocationOnScreen(loc)
        val overlap = if (imeBottom > 0)
            maxOf(0, loc[1] + view.height - (view.rootView.height - imeBottom))
        else 0
        view.setPadding(0, 0, 0, overlap)
        if (imeBottom > 0) scrollToFocused(activity)
    }

    fun scrollToFocused(activity: Activity) {
        val focused = activity.currentFocus ?: return
        var scrollView: NestedScrollView? = null
        var v: View = focused
        while (true) {
            val parent = v.parent ?: break
            if (parent is NestedScrollView) { scrollView = parent; break }
            v = parent as? View ?: break
        }
        val nsv = scrollView ?: return
        nsv.post {
            var absoluteTop = 0
            var current: View = focused
            while (current !== nsv) {
                absoluteTop += current.top
                current = (current.parent as? View) ?: break
            }
            val focusedBottom = absoluteTop + focused.height
            val visibleHeight = nsv.height - nsv.paddingBottom
            val target = focusedBottom - visibleHeight + activity.resources.getDimensionPixelSize(R.dimen.margin_medium)
            if (target > nsv.scrollY) nsv.smoothScrollTo(0, target)
        }
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
            val fmt = NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = 0
            }
            "₹${fmt.format(number)}"
        } catch (_: Exception) {
            "₹$raw"
        }
    }

    fun formatAmount(raw: Double?): String = formatAmount(raw?.toString())

    fun maskNumber(number: String): String {
        if (number.length < 5) return number
        return "${number.take(4)}*****${number.takeLast(1)}"
    }

    fun renderPdfFirstPage(context: Context, uri: Uri, widthPx: Int = 800): Bitmap? = try {
        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                renderer.openPage(0).use { page ->
                    val scale = widthPx.toFloat() / page.width
                    val bmp = createBitmap(widthPx, (page.height * scale).toInt())
                    bmp.eraseColor(Color.WHITE)
                    page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bmp
                }
            }
        }
    } catch (_: Exception) { null }

    fun calculatePlatformFee(amount: Double): Double {
        return when {
            amount < 1000 -> 4.0
            amount <= 5000 -> 8.0
            amount <= 40000 -> 20.0
            else -> 30.0
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
