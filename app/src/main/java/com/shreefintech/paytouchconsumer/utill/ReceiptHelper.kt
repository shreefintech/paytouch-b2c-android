package com.shreefintech.paytouchconsumer.utill

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Path
import android.graphics.RectF
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.card.MaterialCardView
import com.shreefintech.paytouchconsumer.R
import java.io.File
import java.io.FileOutputStream

object ReceiptHelper {

    fun captureViewAsBitmap(view: View): Bitmap {
        val scale = 2f
        val bitmap = Bitmap.createBitmap(
            (view.width * scale).toInt().coerceAtLeast(1),
            (view.height * scale).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        canvas.scale(scale, scale)
        val cornerRadius = (view as? MaterialCardView)?.radius ?: 0f
        if (cornerRadius > 0f) {
            val path = Path().apply {
                addRoundRect(
                    RectF(0f, 0f, view.width.toFloat(), view.height.toFloat()),
                    cornerRadius, cornerRadius,
                    Path.Direction.CW
                )
            }
            canvas.clipPath(path)
        }
        view.draw(canvas)
        return bitmap
    }

    fun saveBitmapAndGetUri(context: Context, bitmap: Bitmap): Uri? {
        val filename = "PayTouch_Receipt_${System.currentTimeMillis()}.png"
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/PayTouch")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return null
                context.contentResolver.openOutputStream(uri)?.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, values, null, null)
                uri
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    "PayTouch"
                ).also { it.mkdirs() }
                val file = File(dir, filename)
                FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), null, null)
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun openImageInGallery(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/png")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(Intent.createChooser(intent, context.getString(R.string.btnOpen)))
        }
    }

    fun performDownload(context: Context, view: View): Uri? {
        val bitmap = captureViewAsBitmap(view)
        return saveBitmapAndGetUri(context, bitmap)
    }

    fun shareReceipt(activity: Activity, view: View, title: String, onFailure: (() -> Unit)? = null) {
        val bitmap = captureViewAsBitmap(view)
        try {
            val dir = File(activity.cacheDir, "receipts").also { it.mkdirs() }
            val file = File(dir, "receipt_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            val uri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            activity.startActivity(Intent.createChooser(intent, title))
        } catch (e: Exception) {
            e.printStackTrace()
            onFailure?.invoke()
        }
    }

    fun applyStatusStyle(context: Context, badge: MaterialCardView, textView: TextView, status: String?) {
        val (bgColor, textColor) = when (status) {
            "success" -> Pair(R.color.toast_bg_success, R.color.toast_text_success)
            "failed"  -> Pair(R.color.toast_bg_delete, R.color.form_wizard_reject)
            else      -> Pair(R.color.toast_bg_warning, R.color.toast_text_warning)
        }
        badge.setCardBackgroundColor(ContextCompat.getColor(context, bgColor))
        textView.setTextColor(ContextCompat.getColor(context, textColor))
    }
}
