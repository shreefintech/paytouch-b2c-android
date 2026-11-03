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
     * @param onDismissed User closed the dialog via the X button — caller decides whether to proceed.
     *                    Back press / outside tap fires nothing.
     */
    fun show(
        activity: Activity,
        authorization: String,
        onSuccess: () -> Unit,
        onDismissed: (() -> Unit)? = null
    ) {
        val binding = DialogPanBinding.inflate(activity.layoutInflater)
        val showProgress = ObservableBoolean(false)
        binding.showProgress = showProgress

        val dialog = Dialog(activity)
        dialog.setContentView(binding.root)
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
                DismissReason.CLOSE   -> onDismissed?.invoke()
                DismissReason.CANCEL  -> Unit
            }
        }

        binding.onClickListener = android.view.View.OnClickListener { view ->
            when (view) {
                binding.cardClose -> {
                    if (Utility.stopClick()) return@OnClickListener
                    dismissReason = DismissReason.CLOSE
                    dialog.dismiss()
                }
                binding.cardSubmit -> {
                    if (Utility.stopClick()) return@OnClickListener
                    if (showProgress.get()) return@OnClickListener
                    onSubmit(activity, authorization, binding, showProgress, dialog) {
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
        binding: DialogPanBinding,
        showProgress: ObservableBoolean,
        dialog: Dialog,
        onBeforeDismiss: () -> Unit
    ) {
        val pan = binding.etPanNumber.text?.toString()?.trim()?.uppercase() ?: ""
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
                        // Dialog window is gone once the host is destroyed — dismiss() would crash
                        if (activity.isFinishing || activity.isDestroyed) return
                        ToastUtil.showSuccess(activity, activity.getString(R.string.msgPanSaved))
                        onBeforeDismiss()
                        dialog.dismiss()
                    } else {
                        if (activity.isFinishing || activity.isDestroyed) return
                        ToastUtil.showDelete(
                            activity,
                            ApiHelper.parseErrorMessage(activity, response.code(), response.errorBody()?.string())
                        )
                    }
                }

                override fun onFailure(call: Call<PanV2Item>, t: Throwable) {
                    showProgress.set(false)
                    if (activity.isFinishing || activity.isDestroyed) return
                    ToastUtil.showDelete(
                        activity,
                        t.localizedMessage ?: activity.getString(R.string.errGeneric)
                    )
                }
            })
    }

    // CANCEL = back press / outside tap
    private enum class DismissReason { SUCCESS, CLOSE, CANCEL }
}
