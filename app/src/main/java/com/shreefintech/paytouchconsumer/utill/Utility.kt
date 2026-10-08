package com.shreefintech.paytouchconsumer.utill

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
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
import androidx.core.graphics.createBitmap
import androidx.core.widget.NestedScrollView
import androidx.exifinterface.media.ExifInterface
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.shreefintech.paytouchconsumer.R
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object Utility {

    /**
     * Prints [e] and reports it to Crashlytics as a non-fatal. Use only for unexpected failures —
     * expected ones (no app for an intent, network errors, OOM) stay on plain printStackTrace().
     */
    fun logError(e: Throwable) {
        e.printStackTrace()
        try {
            FirebaseCrashlytics.getInstance().recordException(e)
        } catch (reportError: Exception) {
            reportError.printStackTrace()
        }
    }

    fun formatDate(createdAt: String?, format: String = "dd/MM/yyyy hh:mm a"): String {
        if (createdAt.isNullOrBlank()) return "--"
        val cleaned = createdAt.substringBefore(".").substringBefore("+")
        val utc = TimeZone.getTimeZone("UTC")
        val ist = TimeZone.getTimeZone("Asia/Kolkata")
        val inputFormats = listOf(
            "yyyy-MM-dd'T'HH:mm:ss",
            "dd/MM/yyyy hh:mm a",
            "dd/MM/yyyy HH:mm:ss",
            "dd/MM/yyyy",
            "yyyy-MM-dd"
        )
        val output = SimpleDateFormat(format, Locale.getDefault()).apply { timeZone = ist }
        for (pattern in inputFormats) {
            try {
                val parser = SimpleDateFormat(pattern, Locale.getDefault()).apply {
                    isLenient = false
                    timeZone = if (pattern.contains("'T'")) utc else ist
                }
                val date = parser.parse(cleaned)
                if (date != null) return output.format(date)
            } catch (_: Exception) {
            }
        }
        return createdAt
    }

    fun isPasswordStrong(password: String): Boolean =
        password.any { it.isUpperCase() } &&
        password.any { it.isDigit() } &&
        password.any { !it.isLetterOrDigit() }

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

    /** [trimZeros] drops trailing ".00" — plan cards only; bills and transactions always show paise. */
    fun formatAmount(raw: String?, trimZeros: Boolean = false): String {
        if (raw.isNullOrBlank()) return "-"
        return try {
            val number = raw.toDouble()
            val fmt = NumberFormat.getNumberInstance(Locale.Builder().setLanguage("en").setRegion("IN").build()).apply {
                maximumFractionDigits = 2
                minimumFractionDigits = if (trimZeros) 0 else 2
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
    } catch (e: Exception) {
        logError(e)
        null
    }

    /**
     * Re-encodes [file] in place as an upright JPEG no larger than [maxBytes] (best effort).
     * Never throws — on decode/encode failure or OOM the original file is left untouched.
     * Must be called off the main thread.
     */
    fun compressImageFile(file: File, maxBytes: Int = 2 * 1024 * 1024) {
        try {
            compressImageFileInternal(file, maxBytes)
        } catch (e: OutOfMemoryError) {
            e.printStackTrace()
        } catch (e: Exception) {
            logError(e)
        }
    }

    // Longest edge kept after power-of-two subsampling; camera photos (12–50 MP) would otherwise
    // be decoded at full size (48–200 MB) and OOM on low-end devices.
    private const val MAX_DECODE_DIMENSION = 1600

    private fun compressImageFileInternal(file: File, maxBytes: Int) {
        val needsCompress = file.length() > maxBytes
        val rotation = try {
            when (ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Exception) { 0 }

        if (!needsCompress && rotation == 0) return

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val longestEdge = maxOf(bounds.outWidth, bounds.outHeight)
        if (longestEdge <= 0) return
        var sampleSize = 1
        while (longestEdge / (sampleSize * 2) >= MAX_DECODE_DIMENSION) sampleSize *= 2

        val raw = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: return
        val bitmap = if (rotation != 0) {
            try {
                Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, Matrix().apply { postRotate(rotation.toFloat()) }, true)
                    .also { if (it !== raw) raw.recycle() }
            } catch (e: OutOfMemoryError) {
                // Leave the original file untouched — compression is best effort.
                e.printStackTrace()
                raw.recycle()
                return
            }
        } else raw

        try {
            var quality = if (needsCompress) 85 else 95
            while (true) {
                val bos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos)
                val bytes = bos.toByteArray()
                if (bytes.size <= maxBytes || quality == 40) {
                    file.writeBytes(bytes)
                    break
                }
                quality -= 15
            }
        } finally {
            bitmap.recycle()
        }
    }

    /**
     * Deletes KYC working directories under filesDir on a background thread (safe from onCreate /
     * onDestroy, where lifecycleScope may already be cancelled). Safe to call when they do not exist.
     */
    fun deleteKycDirs(context: Context, vararg relativePaths: String) {
        val filesDir = context.applicationContext.filesDir
        Thread {
            relativePaths.forEach { path ->
                try {
                    File(filesDir, path).deleteRecursively()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }

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
