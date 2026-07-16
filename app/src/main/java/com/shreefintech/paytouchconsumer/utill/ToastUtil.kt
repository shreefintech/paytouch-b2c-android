package com.shreefintech.paytouchconsumer.utill

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.shreefintech.paytouchconsumer.glass.LiquidGlassEffect
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.LytCustomToastBinding

/**
 * ToastUtil - Common utility for showing styled toast messages.
 *
 * Layout uses MaterialCardView for pill shape + stroke border (no drawable needed).
 *
 * Icon drawables — import from Figma into res/drawable/:
 *   @drawable/ic_toast_upload    → upload / share arrow
 *   @drawable/ic_toast_success   → checkmark
 *   @drawable/ic_toast_edit      → pencil
 *   @drawable/ic_toast_delete    → X / cross
 *   @drawable/ic_toast_warning   → exclamation mark
 *   @drawable/ic_toast_expired   → clock
 */

// ─────────────────────────────────────────────
// Toast Type Enum
// ─────────────────────────────────────────────
enum class ToastType(
    @DrawableRes val iconRes: Int,
    @ColorRes val backgroundColor: Int,
    @ColorRes val iconBackgroundColor: Int,
    @ColorRes val iconTintColor: Int,
    @ColorRes val textColor: Int,
    @ColorRes val borderColor: Int
) {
    /** Blue — "File uploaded successfully" */
    SUCCESS_UPLOAD(
        iconRes             = R.drawable.ic_toast_upload,
        backgroundColor     = R.color.toast_bg_upload,
        iconBackgroundColor = R.color.toast_icon_bg_upload,
        iconTintColor       = R.color.toast_icon_tint_upload,
        textColor           = R.color.toast_text_upload,
        borderColor         = R.color.toast_border_upload
    ),

    /** Green — "Verification successfully" */
    SUCCESS(
        iconRes             = R.drawable.ic_toast_tick,
        backgroundColor     = R.color.toast_bg_success,
        iconBackgroundColor = R.color.toast_icon_bg_success,
        iconTintColor       = R.color.toast_icon_tint_success,
        textColor           = R.color.toast_text_success,
        borderColor         = R.color.toast_border_success
    ),

    /** Yellow — "Changes edited successfully" */
    EDIT(
        iconRes             = R.drawable.ic_toast_edit,
        backgroundColor     = R.color.toast_bg_edit,
        iconBackgroundColor = R.color.toast_icon_bg_edit,
        iconTintColor       = R.color.toast_icon_tint_edit,
        textColor           = R.color.toast_text_edit,
        borderColor         = R.color.toast_border_edit
    ),

    /** Red — "File has been deleted" */
    DELETE(
        iconRes             = R.drawable.ic_cross,
        backgroundColor     = R.color.toast_bg_delete,
        iconBackgroundColor = R.color.toast_icon_bg_delete,
        iconTintColor       = R.color.toast_icon_tint_delete,
        textColor           = R.color.toast_text_delete,
        borderColor         = R.color.toast_border_delete
    ),

    /** Orange — "Your file is pending" */
    WARNING(
        iconRes             = R.drawable.ic_toast_danger,
        backgroundColor     = R.color.toast_bg_warning,
        iconBackgroundColor = R.color.toast_icon_bg_warning,
        iconTintColor       = R.color.toast_icon_tint_warning,
        textColor           = R.color.toast_text_warning,
        borderColor         = R.color.toast_border_warning
    ),

    /** Gray — "Expired" */
    EXPIRED(
        iconRes             = R.drawable.ic_clock,
        backgroundColor     = R.color.toast_bg_expired,
        iconBackgroundColor = R.color.toast_icon_bg_expired,
        iconTintColor       = R.color.toast_icon_tint_expired,
        textColor           = R.color.toast_text_expired,
        borderColor         = R.color.toast_border_expired
    )
}

// ─────────────────────────────────────────────
// ToastUtil Object
// ─────────────────────────────────────────────
object ToastUtil {

    /**
     * Show a styled custom toast using ViewBinding + MaterialCardView.
     *
     * @param context  Any context (Activity / Fragment)
     * @param message  The text to display
     * @param type     One of [ToastType] — controls colours, icon and border
     * @param duration Toast.LENGTH_SHORT (default) or Toast.LENGTH_LONG
     */
    fun show(
        context: Context,
        message: String,
        type: ToastType,
        duration: Int = Toast.LENGTH_SHORT
    ) {
        val binding = LytCustomToastBinding.inflate(LayoutInflater.from(context))

        // ── Outer pill card ───────────────────────────────────
        binding.toastRoot.apply {
            setCardBackgroundColor(ContextCompat.getColor(context, type.backgroundColor))
            strokeColor = ContextCompat.getColor(context, type.borderColor)
        }

        // ── Inner icon card ───────────────────────────────────
        LiquidGlassEffect.attach( targetView = binding.toastIconContainer, rootView = (binding.root as ViewGroup), cornerRadius = context.resources.getDimensionPixelSize(R.dimen.toast_radius), distortion = 0f,blur= context.resources.getDimensionPixelSize(R.dimen.toast_blure))
        /*binding.toastIconContainer.setCardBackgroundColor(
            ContextCompat.getColor(context, type.iconBackgroundColor)
        )*/

        // ── Icon drawable & tint ──────────────────────────────
        binding.toastIcon.apply {
            setImageResource(type.iconRes)
            setImageTintList(ContextCompat.getColorStateList(context, type.iconTintColor))
        }

        // ── Message text & color ──────────────────────────────
        binding.toastMessage.apply {
            text = message
            setTextColor(ContextCompat.getColor(context, type.textColor))
        }

        // ── Show ──────────────────────────────────────────────
        @Suppress("DEPRECATION")
        Toast(context).apply {
            this.duration = duration
            this.view     = binding.root
        }.show()
    }

    // ── Convenience helpers ───────────────────────────────────

    fun showUpload(context: Context, message: String = "File uploaded successfully") =
        show(context, message, ToastType.SUCCESS_UPLOAD)

    fun showSuccess(context: Context, message: String = "Verification successfully") =
        show(context, message, ToastType.SUCCESS)

    fun showEdit(context: Context, message: String = "Changes edited successfully") =
        show(context, message, ToastType.EDIT)

    fun showDelete(context: Context, message: String = "File has been deleted") =
        show(context, message, ToastType.DELETE)

    fun showWarning(context: Context, message: String = "Your file is pending") =
        show(context, message, ToastType.WARNING)

    fun showExpired(context: Context, message: String = "Expired") =
        show(context, message, ToastType.EXPIRED)
}