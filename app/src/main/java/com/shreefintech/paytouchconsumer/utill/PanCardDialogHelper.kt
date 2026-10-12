package com.shreefintech.paytouchconsumer.utill

import android.app.Activity
import android.app.Dialog
import android.view.ViewGroup
import androidx.databinding.ObservableBoolean
import com.shreefintech.paytouchconsumer.Constant
import com.shreefintech.paytouchconsumer.R
import com.shreefintech.paytouchconsumer.databinding.DialogPanBinding
import com.shreefintech.paytouchconsumer.retrofit.ApiClient
import com.shreefintech.paytouchconsumer.retrofit.ApiHelper
import com.shreefintech.paytouchconsumer.retrofit.model.v2.PanV2Item
import com.shreefintech.paytouchconsumer.retrofit.model.v2.PanV2Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object PanCardDialogHelper {

    /**
     * Shows the PAN card dialog.
     *
     * @param onSuccess   PAN was saved successfully — proceed to the next step.
     * @param onSkip      User tapped "Skip for now" — treat the same as success for the caller.
     * @param onDismissed User closed the dialog via the X button — caller decides whether to proceed.
     */
    fun show(
        activity: Activity,
        authorization: String,
        onSuccess: () -> Unit,
        onSkip: (() -> Unit)? = null,
        onDismissed: (() -> Unit)? = null
    ) {
        val b = DialogPanBinding.inflate(activity.layoutInflater)
        val showProgress = ObservableBoolean(false)
        b.showProgress = showProgress

        val dialog = Dialog(activity)
        dialog.setContentView(b.root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.setWindowAnimations(R.style.DialogScaleFadeAnimation)
        dialog.window?.setLayout(
            (activity.resources.displayMetrics.widthPixels * 0.88).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.setCancelable(true)

        // Track dismiss reason so the right callback fires exactly once.
        var dismissReason = DismissReason.CANCEL
        dialog.setOnDismissListener {
            when (dismissReason) {
                DismissReason.SUCCESS -> onSuccess()
                DismissReason.SKIP    -> onSkip?.invoke()
                DismissReason.CANCEL  -> onDismissed?.invoke()
            }
        }

        b.onClickListener = android.view.View.OnClickListener { view ->
            when (view) {
                b.cardClose -> dialog.dismiss()
                b.tvSkipPan -> {
                    dismissReason = DismissReason.SKIP
                    dialog.dismiss()
                }
                b.cardSubmit -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgress.get()) return@OnClickListener
                    onSubmit(activity, authorization, b, showProgress, dialog) {
                        dismissReason = DismissReason.SUCCESS
                    }
                }
            }
        }

        dialog.show()
    }

    private fun onSubmit(
        activity: Activity,
        authorization: String,
        b: DialogPanBinding,
        showProgress: ObservableBoolean,
        dialog: Dialog,
        onBeforeDismiss: () -> Unit
    ) {
        val pan = b.etPanNumber.text?.toString()?.trim()?.uppercase() ?: ""
        if (pan.isEmpty()) {
            ToastUtil.showDelete(activity, activity.getString(R.string.msgPanEmpty))
            return
        }
        if (!pan.matches(Regex("^[A-Z]{5}[0-9]{4}[A-Z]$"))) {
            ToastUtil.showDelete(activity, activity.getString(R.string.msgPanInvalid))
            return
        }
        if (!Utility.isInternetAvailable(activity)) {
            ToastUtil.showDelete(activity, activity.getString(R.string.msgNoInternet))
            return
        }

        showProgress.set(true)
        ApiClient.apiService.addPan(authorization, PanV2Request(pan))
            .enqueue(object : Callback<PanV2Item> {
                override fun onResponse(call: Call<PanV2Item>, response: Response<PanV2Item>) {
                    showProgress.set(false)
                    if (response.isSuccessful && response.body()?.success == true) {
                        SharedPreferenceHelper.setSharedPreferenceBoolean(
                            activity, Constant.KEY_PAN_ADDED, true
                        )
                        ToastUtil.showSuccess(activity, activity.getString(R.string.msgPanSaved))
                        onBeforeDismiss()
                        dialog.dismiss()
                    } else {
                        ToastUtil.showDelete(
                            activity,
                            ApiHelper.parseErrorMessage(activity, response.code(), response.errorBody()?.string())
                        )
                    }
                }

                override fun onFailure(call: Call<PanV2Item>, t: Throwable) {
                    showProgress.set(false)
                    ToastUtil.showDelete(
                        activity,
                        t.localizedMessage ?: activity.getString(R.string.errGeneric)
                    )
                }
            })
    }

    private enum class DismissReason { SUCCESS, SKIP, CANCEL }
}
